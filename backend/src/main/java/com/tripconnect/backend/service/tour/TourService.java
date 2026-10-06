package com.tripconnect.backend.service.tour;

import com.tripconnect.backend.dto.PageResponse;
import com.tripconnect.backend.dto.tour.TourContentRequest;
import com.tripconnect.backend.dto.tour.TourResponses;
import com.tripconnect.backend.entity.Tour;
import com.tripconnect.backend.entity.User;
import com.tripconnect.backend.enums.TourStatus;
import com.tripconnect.backend.repository.StablePaging;
import com.tripconnect.backend.repository.AgentProfileRepository;
import com.tripconnect.backend.repository.SearchPatterns;
import com.tripconnect.backend.repository.TourImageRepository;
import com.tripconnect.backend.repository.TourRepository;
import com.tripconnect.backend.repository.UserRepository;
import com.tripconnect.backend.service.EmailTemplates;
import com.tripconnect.backend.service.NotificationEvents;
import com.tripconnect.backend.service.WebNotifications;
import com.tripconnect.backend.storage.FileVisibility;
import com.tripconnect.backend.storage.TransactionalFileCleanup;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Quản lý tour phía người sở hữu: Agent (tour của mình) hoặc Admin (tour của TripConnect).
 * Vòng đời trạng thái: xem {@link TourStatus}.
 */
@Service
@RequiredArgsConstructor
public class TourService {

    private final TourRepository tourRepository;
    private final TourImageRepository imageRepository;
    private final UserRepository userRepository;
    private final AgentProfileRepository agentProfileRepository;
    private final TourAccess access;
    private final TourContentWriter contentWriter;
    private final TourAssembler assembler;
    private final TourBookingStats bookingStats;
    private final TransactionalFileCleanup fileCleanup;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Tour của người đang đăng nhập. Admin: các tour của TripConnect.
     *
     * @param status  null = mọi trạng thái
     * @param keyword tìm gần đúng theo tên tour; null = không lọc
     */
    @Transactional(readOnly = true)
    public PageResponse<TourResponses.Summary> listMine(TourActor actor, TourStatus status, String keyword,
                                                        Pageable pageable) {
        access.requireActorAllowed(actor);
        Specification<Tour> spec = actor.admin()
                ? (root, query, cb) -> cb.isNull(root.get("agent"))
                : (root, query, cb) -> cb.equal(root.get("agent").get("id"), actor.userId());
        // Tour riêng (từ yêu cầu thiết kế tour) quản lý trong yêu cầu / đơn đặt, không nằm trong danh sách tour
        spec = spec.and((root, query, cb) -> cb.notEqual(root.get("status"), TourStatus.PRIVATE));
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (keyword != null && !keyword.isBlank()) {
            String pattern = SearchPatterns.contains(keyword);
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("title")), pattern, SearchPatterns.ESCAPE));
        }
        var page = tourRepository.findAll(spec, StablePaging.of(pageable));
        return PageResponse.of(page, assembler.toSummaries(page));
    }

    @Transactional(readOnly = true)
    public TourResponses.Detail getMine(TourActor actor, Long tourId) {
        return assembler.toDetail(access.requireManageable(tourId, actor));
    }

    /** Tạo tour ở trạng thái Nháp (sau đó thêm ảnh, lịch khởi hành rồi gửi duyệt / công khai). */
    @Transactional
    public TourResponses.Detail create(TourActor actor, TourContentRequest request) {
        access.requireActorAllowed(actor);
        User user = userRepository.getReferenceById(actor.userId());

        Tour tour = new Tour();
        tour.setAgent(actor.admin() ? null : user);
        tour.setCreatedBy(user);
        tour.setStatus(TourStatus.DRAFT);
        contentWriter.apply(tour, request);
        return assembler.toDetail(tourRepository.save(tour));
    }

    @Transactional
    public TourResponses.Detail update(TourActor actor, Long tourId, TourContentRequest request) {
        Tour tour = access.requireManageable(tourId, actor);
        access.requireContentEditable(tour);

        int oldDays = tour.getDurationDays();
        if (request.getDurationDays() != oldDays && bookingStats.tourHasActiveBookings(tour.getId())) {
            throw new IllegalStateException("Tour đã có khách đặt nên không thể đổi số ngày");
        }
        contentWriter.apply(tour, request);
        access.onContentChanged(tour, actor);
        return assembler.toDetail(tour);
    }

    /** Agent gửi tour cho Admin duyệt. */
    @Transactional
    public TourResponses.Detail submit(TourActor actor, Long tourId) {
        requireAgent(actor, "Tour của TripConnect không cần duyệt — hãy dùng chức năng Công khai");
        Tour tour = access.requireManageable(tourId, actor);
        if (tour.getStatus() != TourStatus.DRAFT && tour.getStatus() != TourStatus.NEEDS_REVISION) {
            throw new IllegalStateException("Chỉ gửi duyệt được tour đang ở trạng thái Nháp hoặc Cần chỉnh sửa");
        }
        requireComplete(tour);

        tour.setStatus(TourStatus.PENDING_APPROVAL);
        tour.setSubmittedAt(LocalDateTime.now());

        String companyName = agentProfileRepository.findByUserId(actor.userId())
                .map(p -> p.getCompanyName()).orElse(tour.getAgent().getEmail());
        eventPublisher.publishEvent(new NotificationEvents.AdminWebEvent(
                WebNotifications.tourSubmitted(tour.getId(), tour.getTitle(), companyName, tour.getPublishedAt() != null)));
        return assembler.toDetail(tour);
    }

    /** Agent rút lại yêu cầu duyệt để sửa tiếp. */
    @Transactional
    public TourResponses.Detail withdraw(TourActor actor, Long tourId) {
        requireAgent(actor, "Tour của TripConnect không qua bước duyệt");
        Tour tour = access.requireManageable(tourId, actor);
        if (tour.getStatus() != TourStatus.PENDING_APPROVAL) {
            throw new IllegalStateException("Tour không ở trạng thái chờ duyệt");
        }
        tour.setStatus(TourStatus.DRAFT);
        return assembler.toDetail(tour);
    }

    /** Admin công khai tour của TripConnect (không qua duyệt). */
    @Transactional
    public TourResponses.Detail publish(TourActor actor, Long tourId) {
        if (!actor.admin()) {
            throw new IllegalStateException("Tour của đối tác phải được Admin duyệt — hãy dùng chức năng Gửi duyệt");
        }
        Tour tour = access.requireManageable(tourId, actor);
        if (tour.getStatus() != TourStatus.DRAFT && tour.getStatus() != TourStatus.HIDDEN) {
            throw new IllegalStateException("Chỉ công khai được tour đang Nháp hoặc Tạm ẩn");
        }
        requireComplete(tour);
        tour.setStatus(TourStatus.PUBLISHED);
        if (tour.getPublishedAt() == null) {
            tour.setPublishedAt(LocalDateTime.now());
        }
        return assembler.toDetail(tour);
    }

    /** Tạm ẩn: ngừng nhận booking mới, lịch đã có khách vẫn khởi hành. */
    @Transactional
    public TourResponses.Detail hide(TourActor actor, Long tourId) {
        Tour tour = access.requireManageable(tourId, actor);
        if (tour.getStatus() != TourStatus.PUBLISHED) {
            throw new IllegalStateException("Chỉ tạm ẩn được tour đang công khai");
        }
        tour.setStatus(TourStatus.HIDDEN);
        return assembler.toDetail(tour);
    }

    /** Hiện lại tour đã ẩn — nội dung không đổi kể từ lần duyệt trước nên không cần duyệt lại. */
    @Transactional
    public TourResponses.Detail unhide(TourActor actor, Long tourId) {
        Tour tour = access.requireManageable(tourId, actor);
        if (tour.getStatus() != TourStatus.HIDDEN) {
            throw new IllegalStateException("Tour không ở trạng thái tạm ẩn");
        }
        requireComplete(tour);
        tour.setStatus(TourStatus.PUBLISHED);
        return assembler.toDetail(tour);
    }

    /** Chỉ xóa được tour chưa từng có khách đặt; tour đã có khách thì dùng Tạm ẩn để giữ lịch sử. */
    @Transactional
    public void delete(TourActor actor, Long tourId) {
        Tour tour = access.requireManageable(tourId, actor);
        if (tour.getStatus() == TourStatus.SUSPENDED) {
            throw new IllegalStateException("Tour đang bị đình chỉ nên không thể xóa");
        }
        if (bookingStats.tourHasBookings(tour.getId())) {
            throw new IllegalStateException("Tour đã có khách đặt nên không thể xóa. Hãy dùng Tạm ẩn để ngừng bán");
        }

        // File trên Cloudinary chỉ xóa SAU KHI DB commit (xóa trước mà DB lỗi thì mất file)
        // Chỉ lấy mã file: nạp cả entity ảnh thì Hibernate báo lỗi vì ảnh còn trỏ tới tour sắp bị xóa
        for (String publicId : imageRepository.findPublicIdsByTourId(tour.getId())) {
            fileCleanup.deleteAfterCommit(publicId, FileVisibility.PUBLIC);
        }
        if (tour.hasItineraryFile()) {
            fileCleanup.deleteAfterCommit(tour.getItineraryFilePublicId(), FileVisibility.PRIVATE);
        }
        // Ảnh, lịch khởi hành, lịch trình... bị xóa theo nhờ ON DELETE CASCADE trong DB
        tourRepository.delete(tour);
    }

    private void requireComplete(Tour tour) {
        List<String> missing = assembler.missingItems(tour, imageRepository.countByTourId(tour.getId()));
        if (!missing.isEmpty()) {
            throw new IllegalStateException("Tour chưa đủ thông tin: " + String.join("; ", missing));
        }
    }

    private static void requireAgent(TourActor actor, String messageForAdmin) {
        if (actor.admin()) {
            throw new IllegalStateException(messageForAdmin);
        }
    }
}
