package com.tripconnect.backend.dto.assistant;

import com.tripconnect.backend.dto.search.SearchResponses;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** DTO của trợ lý AI. */
public final class AssistantDtos {

    private AssistantDtos() {
    }

    public static final int MAX_MESSAGE_LENGTH = 1000;
    public static final int MAX_GUEST_HISTORY = 10;

    /**
     * Câu hỏi gửi trợ lý.
     *
     * @param conversationId đã đăng nhập: tiếp tục cuộc trò chuyện này (null = cuộc mới)
     * @param history        chưa đăng nhập: vài tin gần nhất do trình duyệt giữ (đã đăng nhập thì bỏ qua)
     */
    public record ChatRequest(
            Long conversationId,
            @NotBlank(message = "Vui lòng nhập câu hỏi")
            @Size(max = MAX_MESSAGE_LENGTH, message = "Câu hỏi tối đa " + MAX_MESSAGE_LENGTH + " ký tự")
            String message,
            @Size(max = MAX_GUEST_HISTORY) List<@Valid @NotNull HistoryMessage> history
    ) {
    }

    public record HistoryMessage(
            @NotNull Boolean fromUser,
            @NotNull @Size(max = 4000) String content
    ) {
    }

    /** Bản nháp yêu cầu tour riêng do trợ lý gợi ý — khách bấm nút để mở form đã điền sẵn. */
    public record CustomRequestDraft(
            Long departureLocationId,
            String departureLocationName,
            List<Long> destinationIds,
            List<String> destinationNames,
            LocalDate earliestStart,
            LocalDate latestStart,
            Integer durationDays,
            Integer adults,
            Integer children,
            Integer infants,
            Long budgetMax,
            String notes
    ) {
    }

    /** Phần đính kèm câu trả lời (thẻ tour, nút tạo yêu cầu tour riêng). */
    public record Attachments(List<SearchResponses.TourCard> tours, CustomRequestDraft customRequest) {
        public boolean isEmpty() {
            return (tours == null || tours.isEmpty()) && customRequest == null;
        }
    }

    public record Status(boolean enabled, boolean signedIn, int dailyLimit) {
    }

    public record ConversationSummary(Long id, String title, LocalDateTime updatedAt) {
    }

    public record Message(Long id, boolean fromUser, String content, Attachments attachments, LocalDateTime createdAt) {
    }

    public record Conversation(Long id, String title, List<Message> messages) {
    }
}
