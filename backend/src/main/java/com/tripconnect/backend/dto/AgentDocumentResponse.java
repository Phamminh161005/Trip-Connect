package com.tripconnect.backend.dto;

import com.tripconnect.backend.enums.AgentDocumentStatus;
import com.tripconnect.backend.enums.AgentDocumentType;

import java.time.LocalDateTime;

/** Thông tin giấy tờ (không kèm link — muốn xem thì gọi API lấy link tạm thời). */
public record AgentDocumentResponse(
        Long id,
        AgentDocumentType type,
        String typeLabel,
        AgentDocumentStatus status,
        String originalFilename,
        String mimeType,
        long sizeBytes,
        LocalDateTime uploadedAt
) {
}
