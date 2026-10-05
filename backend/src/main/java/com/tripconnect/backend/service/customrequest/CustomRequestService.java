package com.tripconnect.backend.service.customrequest;

import com.tripconnect.backend.dto.PageResponse;
import com.tripconnect.backend.dto.customrequest.CustomRequestRequests;
import com.tripconnect.backend.dto.customrequest.CustomRequestResponses;
import com.tripconnect.backend.entity.CustomRequest;
import com.tripconnect.backend.entity.Location;
import com.tripconnect.backend.entity.TourCategory;
import com.tripconnect.backend.enums.CustomRequestStatus;
import com.tripconnect.backend.enums.UserRole;
import com.tripconnect.backend.exception.ForbiddenException;
import com.tripconnect.backend.exception.ResourceNotFoundException;
import com.tripconnect.backend.repository.*;
import com.tripconnect.backend.service.NotificationEvents;
import com.tripconnect.backend.service.WebNotifications;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;

/**
 * Khách gửi, xem, hủy yêu cầu thiết kế tour riêng. Agent không được gửi (tránh xem báo giá chi tiết của đơn vị khác).
 */
@Service
@RequiredArgsConstructor
public class CustomRequestService {

    private static final DateTimeFormatter CODE_DATE = DateTimeFormatter.ofPattern("yyMMdd");
    private static final SecureRandom RANDOM = new SecureRandom();

    private final CustomRequestRepository requestRepository;
    private final LocationRepository locationRepository;
    private final TourCategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final CustomRequestAssembler assembler;
    private final CustomRequestLifecycle lifecycle;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    @Transactional
    public CustomRequestResponses.Detail create(Long userId, String role, CustomRequestRequests.Create body) {
        if (!UserRole.CUSTOMER.name().equals(role)) {
            throw new ForbiddenException("Chỉ tài khoản khách hàng mới gửi được yêu cầu thiết kế tour");
        }
        LocalDate today = LocalDate.now(clock);
        validate(body, today);
        if (requestRepository.countByCustomerIdAndStatusIn(userId, CustomRequestRules.OPEN_STATUSES)
                >= CustomRequestRules.MAX_OPEN_PER_CUSTOMER) {
            throw new IllegalStateException("Bạn đang có " + CustomRequestRules.MAX_OPEN_PER_CUSTOMER
                    + " yêu cầu chưa kết thúc. Hãy chờ xử lý xong hoặc hủy bớt trước khi gửi yêu cầu mới");
        }

        CustomRequest request = new CustomRequest();
        request.setCode(newCode(today));
        request.setCustomer(userRepository.getReferenceById(userId));
        request.setDepartureLocation(locationRepository.findById(body.getDepartureLocationId())
                .orElseThrow(() -> new IllegalArgumentException("Nơi khởi hành không hợp lệ")));
        List<Location> destinations = locationRepository.findAllById(new HashSet<>(body.getDestinationIds()));
        if (destinations.size() != new HashSet<>(body.getDestinationIds()).size()) {
            throw new IllegalArgumentException("Điểm đến không hợp lệ");
        }
        request.getDestinations().addAll(destinations);
        if (body.getCategoryIds() != null && !body.getCategoryIds().isEmpty()) {
            List<TourCategory> categories = categoryRepository.findAllById(new HashSet<>(body.getCategoryIds()));
            if (categories.size() != new HashSet<>(body.getCategoryIds()).size()) {
                throw new IllegalArgumentException("Loại hình tour không hợp lệ");
            }
            request.getCategories().addAll(categories);
        }
        if (body.getTransportModes() != null) request.getTransportModes().addAll(body.getTransportModes());
        request.setEarliestStart(body.getEarliestStart());
        request.setLatestStart(body.getLatestStart());
        request.setDurationDays(body.getDurationDays().shortValue());
        request.setAdults(body.getAdults().shortValue());
        request.setChildren(body.getChildren().shortValue());
        request.setInfants(body.getInfants().shortValue());
        request.setBudgetMin(body.getBudgetMin());
        request.setBudgetMax(body.getBudgetMax());
        request.setAccommodationType(body.getAccommodationType());
        request.setNotes(body.getNotes() == null || body.getNotes().isBlank() ? null : body.getNotes().trim());
        request.setStatus(CustomRequestStatus.NEW);
        request.setCreatedAt(LocalDateTime.now(clock));
        requestRepository.save(request);

        eventPublisher.publishEvent(new NotificationEvents.AdminWebEvent(WebNotifications.customRequestNew(
                request.getId(), request.getCode(), CustomRequestAssembler.destinationsText(request))));
        return assembler.toDetail(request, CustomRequestAssembler.Viewer.CUSTOMER, null, true, false);
    }

    static void validate(CustomRequestRequests.Create body, LocalDate today) {
        LocalDate earliestAllowed = today.plusDays(CustomRequestRules.MIN_LEAD_DAYS);
        if (body.getEarliestStart().isBefore(earliestAllowed)) {
            throw new IllegalArgumentException("Ngày khởi hành sớm nhất phải từ " + earliestAllowed.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                    + " (cần ít nhất " + CustomRequestRules.MIN_LEAD_DAYS + " ngày để thiết kế và chuẩn bị tour)");
        }
        if (body.getLatestStart().isBefore(body.getEarliestStart())) {
            throw new IllegalArgumentException("Ngày khởi hành muộn nhất phải sau ngày sớm nhất");
        }
        if (ChronoUnit.DAYS.between(body.getEarliestStart(), body.getLatestStart()) > CustomRequestRules.MAX_START_WINDOW_DAYS) {
            throw new IllegalArgumentException("Khoảng ngày có thể khởi hành tối đa " + CustomRequestRules.MAX_START_WINDOW_DAYS + " ngày");
        }
        if (body.getInfants() > body.getAdults()) {
            throw new IllegalArgumentException("Mỗi trẻ sơ sinh cần đi cùng một người lớn");
        }
        if (body.getAdults() + body.getChildren() + body.getInfants() > CustomRequestRules.MAX_TRAVELLERS) {
            throw new IllegalArgumentException("Mỗi yêu cầu tối đa " + CustomRequestRules.MAX_TRAVELLERS + " khách");
        }
        if (body.getBudgetMin() != null && body.getBudgetMax() != null && body.getBudgetMin() > body.getBudgetMax()) {
            throw new IllegalArgumentException("Ngân sách tối thiểu phải nhỏ hơn hoặc bằng ngân sách tối đa");
        }
    }

    private String newCode(LocalDate today) {
        for (int i = 0; i < 10; i++) {
            String code = "YC" + today.format(CODE_DATE) + String.format("%06d", RANDOM.nextInt(1_000_000));
            if (!requestRepository.existsByCode(code)) return code;
        }
        throw new IllegalStateException("Không tạo được mã yêu cầu, vui lòng thử lại");
    }

    @Transactional(readOnly = true)
    public PageResponse<CustomRequestResponses.Summary> listMine(Long userId, Pageable pageable) {
        Specification<CustomRequest> spec = (root, query, cb) -> cb.equal(root.get("customer").get("id"), userId);
        var page = requestRepository.findAll(spec, StablePaging.of(pageable));
        return PageResponse.of(page, assembler.toSummaries(page.getContent(), CustomRequestAssembler.Viewer.CUSTOMER));
    }

    @Transactional(readOnly = true)
    public CustomRequestResponses.Detail getMine(Long userId, Long requestId) {
        CustomRequest request = requestRepository.findById(requestId)
                .filter(r -> r.getCustomer().getId().equals(userId))
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu"));
        return assembler.toDetail(request, CustomRequestAssembler.Viewer.CUSTOMER, null,
                CustomRequestRules.OPEN_STATUSES.contains(request.getStatus()), false);
    }

    @Transactional
    public CustomRequestResponses.Detail cancel(Long userId, Long requestId, String reason) {
        CustomRequest request = requestRepository.findByIdForUpdate(requestId)
                .filter(r -> r.getCustomer().getId().equals(userId))
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu"));
        if (!CustomRequestRules.OPEN_STATUSES.contains(request.getStatus())) {
            throw new IllegalStateException("Yêu cầu đã kết thúc");
        }
        lifecycle.finish(request, CustomRequestStatus.CANCELLED,
                reason == null || reason.isBlank() ? "Khách hủy yêu cầu" : "Khách hủy yêu cầu: " + reason.trim());
        return assembler.toDetail(request, CustomRequestAssembler.Viewer.CUSTOMER, null, false, false);
    }
}
