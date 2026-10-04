package com.tripconnect.backend.service.tour;

import com.tripconnect.backend.entity.Tour;
import com.tripconnect.backend.enums.TourStatus;
import com.tripconnect.backend.exception.ForbiddenException;
import com.tripconnect.backend.exception.ResourceNotFoundException;
import com.tripconnect.backend.repository.TourRepository;
import com.tripconnect.backend.security.AgentAccessGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Nạp tour + kiểm tra người thao tác có quyền quản lý tour đó không, và các quy tắc trạng thái dùng chung.
 * Phải được gọi trong transaction.
 */
@Component
@RequiredArgsConstructor
public class TourAccess {

    private final TourRepository tourRepository;
    private final AgentAccessGuard agentAccessGuard;

    /** Agent chỉ thấy tour của mình, Admin chỉ quản lý tour của TripConnect. */
    public Tour requireManageable(Long tourId, TourActor actor) {
        requireActorAllowed(actor);
        return checkOwner(tourRepository.findById(tourId), actor);
    }

    /** Như trên nhưng khóa dòng tour tới hết transaction (dùng khi thêm/xóa ảnh). */
    public Tour requireManageableForUpdate(Long tourId, TourActor actor) {
        requireActorAllowed(actor);
        return checkOwner(tourRepository.findByIdForUpdate(tourId), actor);
    }

    /** Chỉ Agent đã được duyệt hồ sơ mới được đăng tour. */
    public void requireActorAllowed(TourActor actor) {
        if (!actor.admin()) {
            agentAccessGuard.requireApprovedAgent(actor.userId());
        }
    }

    public Tour requireTour(Long tourId) {
        return tourRepository.findById(tourId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tour"));
    }

    /** Được sửa nội dung (thông tin, lịch trình, ảnh, file) hay không. */
    public void requireContentEditable(Tour tour) {
        switch (tour.getStatus()) {
            case PENDING_APPROVAL -> throw new IllegalStateException(
                    "Tour đang chờ duyệt. Hãy rút lại yêu cầu duyệt nếu muốn chỉnh sửa");
            case SUSPENDED -> throw new IllegalStateException("Tour đang bị đình chỉ nên không thể chỉnh sửa");
            default -> {
            }
        }
    }

    /**
     * Gọi sau khi nội dung tour thay đổi. Agent sửa tour đang bán / đang ẩn -> tour về Nháp,
     * ẩn khỏi trang tìm kiếm cho tới khi được duyệt lại (lịch đã có khách vẫn giữ nguyên).
     * Tour của TripConnect do Admin sửa thì không cần duyệt.
     */
    public void onContentChanged(Tour tour, TourActor actor) {
        if (!actor.admin() && (tour.getStatus() == TourStatus.PUBLISHED || tour.getStatus() == TourStatus.HIDDEN)) {
            tour.setStatus(TourStatus.DRAFT);
            tour.setStatusReason(null);
        }
    }

    private Tour checkOwner(Optional<Tour> found, TourActor actor) {
        Tour tour = found.orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tour"));
        if (actor.admin()) {
            if (!tour.isPlatformTour()) {
                throw new ForbiddenException("Đây là tour của đối tác — Admin chỉ được duyệt hoặc đình chỉ, không được chỉnh sửa");
            }
        } else if (tour.isPlatformTour() || !tour.getAgent().getId().equals(actor.userId())) {
            // Trả 404 thay vì 403 để không tiết lộ tour của Agent khác có tồn tại
            throw new ResourceNotFoundException("Không tìm thấy tour");
        }
        return tour;
    }
}
