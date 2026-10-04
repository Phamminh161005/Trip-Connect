package com.tripconnect.backend.service.tour;

import com.tripconnect.backend.dto.PageResponse;
import com.tripconnect.backend.dto.TemporaryUrlResponse;
import com.tripconnect.backend.dto.tour.TourResponses;
import com.tripconnect.backend.entity.AgentProfile;
import com.tripconnect.backend.entity.Tour;
import com.tripconnect.backend.enums.TourStatus;
import com.tripconnect.backend.repository.StablePaging;
import com.tripconnect.backend.repository.SearchPatterns;
import com.tripconnect.backend.repository.TourRepository;
import com.tripconnect.backend.repository.UserRepository;
import com.tripconnect.backend.service.EmailTemplates;
import com.tripconnect.backend.service.NotificationEvents;
import com.tripconnect.backend.service.WebNotifications;
import jakarta.persistence.criteria.Subquery;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** Admin xem mọi tour, duyệt / yêu cầu chỉnh sửa / đình chỉ tour của đối tác. */
@Service
@RequiredArgsConstructor
public class TourReviewService {

    /** Lọc theo đơn vị tổ chức. */
    public enum ProviderFilter { AGENT, PLATFORM }

    private final TourRepository tourRepository;
    private final UserRepository userRepository;
    private final TourAccess access;
    private final TourAssembler assembler;
    private final TourMediaService mediaService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * @param status   null = mọi trạng thái
     * @param keyword  tìm theo tên tour hoặc tên công ty đối tác
     * @param provider null = cả tour đối tác lẫn tour TripConnect
     */
    @Transactional(readOnly = true)
    public PageResponse<TourResponses.Summary> list(TourStatus status, String keyword, ProviderFilter provider,
                                                    Pageable pageable) {
        Specification<Tour> spec = (root, query, cb) -> cb.conjunction();
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (provider != null) {
            spec = spec.and((root, query, cb) -> provider == ProviderFilter.PLATFORM
                    ? cb.isNull(root.get("agent"))
                    : cb.isNotNull(root.get("agent")));
        }
        if (keyword != null && !keyword.isBlank()) {
            String pattern = SearchPatterns.contains(keyword);
            spec = spec.and((root, query, cb) -> {
                // Tên công ty nằm ở agent_profiles (nối qua user) -> dùng truy vấn con
                Subquery<Long> company = query.subquery(Long.class);
                var profile = company.from(AgentProfile.class);
                company.select(profile.get("user").get("id")).where(
                        cb.like(cb.lower(profile.get("companyName")), pattern, SearchPatterns.ESCAPE));
                return cb.or(
                        cb.like(cb.lower(root.get("title")), pattern, SearchPatterns.ESCAPE),
                        root.get("agent").get("id").in(company));
            });
        }
        var page = tourRepository.findAll(spec, StablePaging.of(pageable));
        return PageResponse.of(page, assembler.toSummaries(page));
    }

    @Transactional(readOnly = true)
    public TourResponses.Detail get(Long tourId) {
        return assembler.toDetail(access.requireTour(tourId));
    }

    @Transactional(readOnly = true)
    public TemporaryUrlResponse itineraryFileUrl(Long tourId) {
        return mediaService.itineraryFileUrl(access.requireTour(tourId));
    }

    @Transactional
    public TourResponses.Detail approve(Long tourId, Long adminId) {
        Tour tour = requirePending(tourId);
        tour.setStatus(TourStatus.PUBLISHED);
        tour.setStatusReason(null);
        if (tour.getPublishedAt() == null) {
            tour.setPublishedAt(LocalDateTime.now());
        }
        markReviewed(tour, adminId);
        notifyAgent(tour, EmailTemplates.tourApproved(tour.getTitle()), WebNotifications.tourApproved(tour.getId(), tour.getTitle()));
        return assembler.toDetail(tour);
    }

    @Transactional
    public TourResponses.Detail requestRevision(Long tourId, Long adminId, String reason) {
        Tour tour = requirePending(tourId);
        tour.setStatus(TourStatus.NEEDS_REVISION);
        tour.setStatusReason(reason.trim());
        markReviewed(tour, adminId);
        notifyAgent(tour, EmailTemplates.tourNeedsRevision(tour.getTitle(), reason.trim()),
                WebNotifications.tourNeedsRevision(tour.getId(), tour.getTitle(), reason.trim()));
        return assembler.toDetail(tour);
    }

    /** Đình chỉ tour vi phạm: ngừng nhận booking mới, Agent không tự mở lại được. */
    @Transactional
    public TourResponses.Detail suspend(Long tourId, Long adminId, String reason) {
        Tour tour = requireAgentTour(tourId);
        if (tour.getStatus() != TourStatus.PUBLISHED && tour.getStatus() != TourStatus.HIDDEN) {
            throw new IllegalStateException("Chỉ đình chỉ được tour đang công khai hoặc đang tạm ẩn");
        }
        tour.setStatus(TourStatus.SUSPENDED);
        tour.setStatusReason(reason.trim());
        markReviewed(tour, adminId);
        notifyAgent(tour, EmailTemplates.tourSuspended(tour.getTitle(), reason.trim()),
                WebNotifications.tourSuspended(tour.getId(), tour.getTitle(), reason.trim()));
        return assembler.toDetail(tour);
    }

    @Transactional
    public TourResponses.Detail unsuspend(Long tourId, Long adminId) {
        Tour tour = requireAgentTour(tourId);
        if (tour.getStatus() != TourStatus.SUSPENDED) {
            throw new IllegalStateException("Tour không bị đình chỉ");
        }
        tour.setStatus(TourStatus.PUBLISHED);
        tour.setStatusReason(null);
        markReviewed(tour, adminId);
        notifyAgent(tour, EmailTemplates.tourUnsuspended(tour.getTitle()), WebNotifications.tourUnsuspended(tour.getId(), tour.getTitle()));
        return assembler.toDetail(tour);
    }

    private Tour requirePending(Long tourId) {
        Tour tour = requireAgentTour(tourId);
        if (tour.getStatus() != TourStatus.PENDING_APPROVAL) {
            throw new IllegalStateException("Chỉ xử lý được tour đang chờ duyệt");
        }
        return tour;
    }

    private Tour requireAgentTour(Long tourId) {
        Tour tour = access.requireTour(tourId);
        if (tour.isPlatformTour()) {
            throw new IllegalStateException("Tour của TripConnect không qua bước duyệt — hãy dùng Tạm ẩn / Công khai");
        }
        return tour;
    }

    private void markReviewed(Tour tour, Long adminId) {
        tour.setReviewedBy(userRepository.getReferenceById(adminId));
        tour.setReviewedAt(LocalDateTime.now());
    }

    private void notifyAgent(Tour tour, EmailTemplates.Email email, NotificationEvents.WebMessage message) {
        eventPublisher.publishEvent(new NotificationEvents.UserEmailEvent(tour.getAgent().getEmail(), email));
        eventPublisher.publishEvent(new NotificationEvents.UserWebEvent(tour.getAgent().getId(), message));
    }
}
