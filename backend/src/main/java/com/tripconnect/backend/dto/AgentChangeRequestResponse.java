package com.tripconnect.backend.dto;

import com.tripconnect.backend.enums.ChangeRequestStatus;

import java.time.LocalDateTime;
import java.util.List;

/** Trường thông tin = null nghĩa là yêu cầu không đổi trường đó. */
public record AgentChangeRequestResponse(
        Long id,
        Long agentProfileId,
        ChangeRequestStatus status,
        String companyName,
        String taxCode,
        String businessLicense,
        LocationResponse addressProvince,
        String address,
        BankResponse bank,
        String bankAccountNumber,
        String bankAccountHolder,
        String note,
        String rejectionReason,
        LocalDateTime createdAt,
        LocalDateTime reviewedAt,
        List<AgentDocumentResponse> documents
) {
}
