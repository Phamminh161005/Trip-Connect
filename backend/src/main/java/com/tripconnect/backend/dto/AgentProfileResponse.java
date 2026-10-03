package com.tripconnect.backend.dto;

import com.tripconnect.backend.enums.AgentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Chi tiết hồ sơ Agent — dùng cho cả Agent (xem hồ sơ của mình) và Admin (xem để duyệt). */
public record AgentProfileResponse(
        Long id,
        Long userId,
        String fullName,
        String email,
        String phone,
        String companyName,
        String taxCode,
        String businessLicense,
        LocationResponse addressProvince,
        String address,
        BankResponse bank,
        String bankAccountNumber,
        String bankAccountHolder,
        AgentStatus status,
        String rejectionReason,
        boolean acceptingRequests,
        int maxOpenRequests,
        BigDecimal rating,
        int ratingCount,
        LocalDateTime submittedAt,
        LocalDateTime reviewedAt,
        LocalDateTime createdAt,
        List<LocationResponse> serviceAreas,
        List<TourCategoryResponse> specialties,
        List<AgentDocumentResponse> documents,
        /** Id yêu cầu cập nhật đang chờ duyệt (null nếu không có). */
        Long pendingChangeRequestId,
        /** Mục còn thiếu để được nộp hồ sơ (chỉ tính khi hồ sơ đang Nháp / Cần bổ sung; rỗng = đủ điều kiện). */
        List<String> missingItems
) {
}
