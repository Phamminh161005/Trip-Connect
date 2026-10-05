package com.tripconnect.backend.dto.customrequest;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.tripconnect.backend.dto.LocationResponse;
import com.tripconnect.backend.dto.TourCategoryResponse;
import com.tripconnect.backend.dto.tour.TourResponses;
import com.tripconnect.backend.enums.AccommodationType;
import com.tripconnect.backend.enums.AssignmentStatus;
import com.tripconnect.backend.enums.CustomRequestStatus;
import com.tripconnect.backend.enums.ProposalStatus;
import com.tripconnect.backend.enums.TransportMode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

public final class CustomRequestResponses {

    private CustomRequestResponses() {
    }

    /**
     * Một dòng danh sách. customerName đã rút gọn khi Agent xem.
     * assignment*: lần giao cho Agent đang xem (danh sách của Agent) hoặc lần giao đang chờ (Admin).
     */
    public record Summary(
            Long id,
            String code,
            CustomRequestStatus status,
            LocationResponse departureLocation,
            List<LocationResponse> destinations,
            LocalDate earliestStart,
            LocalDate latestStart,
            int durationDays,
            int adults,
            int children,
            int infants,
            Long budgetMin,
            Long budgetMax,
            String customerName,
            /* Tên công ty của Agent đang được giao / đang xử lý */
            String agentName,
            AssignmentStatus assignmentStatus,
            LocalDateTime assignmentDeadline,
            Stage stage,
            /* Hạn Agent gửi đề xuất tiếp theo */
            LocalDateTime proposalDeadline,
            LocalDateTime createdAt
    ) {
    }

    /** Lịch sử giao việc (Admin). */
    public record AssignmentView(Long id, Long agentId, String agentName, AssignmentStatus status, BigDecimal matchScore,
                                 LocalDateTime deadline, String declineReason, LocalDateTime respondedAt,
                                 String assignedByName, LocalDateTime createdAt) {
    }

    public record Detail(
            Long id,
            String code,
            CustomRequestStatus status,
            LocationResponse departureLocation,
            List<LocationResponse> destinations,
            List<TourCategoryResponse> categories,
            Set<TransportMode> transportModes,
            AccommodationType accommodationType,
            LocalDate earliestStart,
            LocalDate latestStart,
            int durationDays,
            int adults,
            int children,
            int infants,
            Long budgetMin,
            Long budgetMax,
            String notes,
            String customerName,
            /* Chỉ Admin thấy */
            String customerEmail,
            String customerPhone,
            /* Agent đang được giao / xử lý (khách chỉ thấy khi Agent đã nhận) */
            Long agentId,
            String agentName,
            LocalDateTime assignedAt,
            LocalDateTime acceptedAt,
            LocalDateTime proposalDeadline,
            LocalDateTime closedAt,
            String closedReason,
            /* Lần giao cho Agent đang xem (Agent) */
            AssignmentStatus myAssignmentStatus,
            LocalDateTime myAssignmentDeadline,
            /* Lịch sử giao việc (chỉ Admin) */
            List<AssignmentView> assignments,
            /* Thao tác được phép với người đang xem */
            boolean canCancel,
            boolean canRespond,
            /* Đề xuất */
            Stage stage,
            int revisionCount,
            int maxRevisions,
            LocalDateTime agreedAt,
            /* Cũ trước mới sau. Khách: của đơn vị đang phụ trách; Agent: của mình; Admin: tất cả */
            List<ProposalView> proposals,
            boolean canPropose,
            boolean canAcceptProposal,
            boolean canRequestRevision,
            LocalDateTime createdAt
    ) {
    }

    /** Đang chờ ai (chỉ khi IN_PROGRESS). */
    public enum Stage {
        /** Agent soạn đề xuất đầu tiên */
        DRAFTING,
        /** Agent soạn bản chỉnh sửa theo góp ý của khách */
        REVISING,
        /** Đề xuất đã gửi, chờ khách phản hồi (hoặc đã hết hạn, khách vẫn có thể yêu cầu chỉnh sửa) */
        WAITING_CUSTOMER
    }

    public record ProposalView(
            Long id,
            int versionNo,
            ProposalStatus status,
            /* Chỉ Admin (có thể khác nhau khi yêu cầu đã giao lại) */
            String agentName,
            String title,
            LocalDate startDate,
            LocalDate endDate,
            int durationDays,
            int durationNights,
            List<TourResponses.ItineraryDay> itinerary,
            Set<TransportMode> transportModes,
            AccommodationType accommodationType,
            String meetingPoint,
            @JsonFormat(pattern = "HH:mm") LocalTime meetingTime,
            List<String> includedServices,
            List<String> excludedServices,
            String notes,
            long adultPrice,
            long childPrice,
            long totalPrice,
            long depositAmount,
            String agentMessage,
            String customerFeedback,
            LocalDateTime expiresAt,
            LocalDateTime respondedAt,
            LocalDateTime createdAt
    ) {
    }

    /** Agent gợi ý cho Admin, đã chấm điểm. */
    public record Candidate(
            Long agentId,
            Long agentProfileId,
            String companyName,
            BigDecimal rating,
            int ratingCount,
            BigDecimal locationScore,
            BigDecimal ratingScore,
            BigDecimal totalScore,
            int openRequests,
            int maxOpenRequests,
            /* Khu vực phục vụ trùng với điểm đến của yêu cầu */
            List<String> matchedAreas,
            LocalDateTime lastLoginAt,
            boolean recommended
    ) {
    }
}
