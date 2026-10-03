package com.tripconnect.backend.dto;

import java.time.LocalDateTime;

/** Link xem file private, tự hết hạn tại {@code expiresAt}. */
public record TemporaryUrlResponse(String url, LocalDateTime expiresAt) {
}
