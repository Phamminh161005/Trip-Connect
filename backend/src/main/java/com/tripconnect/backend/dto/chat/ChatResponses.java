package com.tripconnect.backend.dto.chat;

import java.time.LocalDateTime;
import java.util.List;

public final class ChatResponses {

    private ChatResponses() {
    }

    /**
     * Một cuộc trò chuyện của yêu cầu (khách với một đơn vị).
     *
     * @param current              đơn vị đang phụ trách yêu cầu
     * @param canWrite             người xem gửi tin được (đang phụ trách, yêu cầu chưa hủy / đóng)
     * @param unread               số tin người xem chưa đọc
     * @param counterpartLastRead  phía bên kia đã đọc tới tin nào (hiện "Đã xem")
     */
    public record Thread(Long agentId, String agentName, String customerName, boolean current, boolean canWrite,
                         long unread, long myLastRead, long counterpartLastRead) {
    }

    public record Image(Long id, String url) {
    }

    /**
     * @param body       đã che số điện thoại / email với khách và Agent
     * @param masked     nội dung có phần bị che
     * @param clientId   mã tạm trình duyệt gửi kèm (chỉ có ở phản hồi gửi tin / sự kiện realtime) để khớp tin "đang gửi"
     */
    public record Message(Long id, Long requestId, Long agentId, Long senderId, boolean fromCustomer, String body,
                          boolean masked, List<Image> images, LocalDateTime createdAt, String clientId) {
    }

    /** Tin cũ trước, mới sau. */
    public record Page(List<Message> items, boolean hasMore) {
    }

    /** Sự kiện realtime: một người đã đọc tới tin nào. */
    public record Read(Long requestId, Long agentId, Long userId, long lastReadMessageId) {
    }
}
