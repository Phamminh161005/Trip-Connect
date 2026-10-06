package com.tripconnect.backend.dto.chat;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public final class ChatRequests {

    private ChatRequests() {
    }

    /**
     * Đã đọc tới tin nào.
     *
     * @param agentId cuộc trò chuyện với đơn vị nào (khách); Agent bỏ trống
     */
    public record Read(Long agentId, @NotNull @PositiveOrZero Long lastMessageId) {
    }
}
