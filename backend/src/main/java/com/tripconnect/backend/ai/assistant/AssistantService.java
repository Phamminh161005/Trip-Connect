package com.tripconnect.backend.ai.assistant;

import com.tripconnect.backend.ai.AiUnavailableException;
import com.tripconnect.backend.ai.LlmClient;
import com.tripconnect.backend.dto.assistant.AssistantDtos;
import com.tripconnect.backend.dto.search.SearchResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Một lượt hỏi - đáp với trợ lý: gửi hội thoại cho mô hình, chạy các hàm mô hình yêu cầu,
 * lặp lại đến khi có câu trả lời; chữ được đẩy dần về trình duyệt qua {@link Sink}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AssistantService {

    /** Số vòng gọi hàm tối đa trong một lượt (tránh mô hình gọi mãi không dừng). */
    static final int MAX_TOOL_ROUNDS = 4;
    static final String BUSY_MESSAGE = "Trợ lý đang bận, bạn thử lại sau ít phút nhé.";
    static final String EMPTY_ANSWER = "Xin lỗi, mình chưa trả lời được câu này. Bạn diễn đạt lại giúp mình nhé.";

    private static final Pattern TOUR_REF = Pattern.compile("\\[tour:(\\d+)]");

    private final LlmClient llm;
    private final AssistantTools tools;
    private final AssistantConversationService conversations;
    private final Clock clock;

    /** Nơi nhận các sự kiện của câu trả lời (SSE ở controller). Ném {@link ClientGoneException} khi người dùng đã đóng. */
    public interface Sink {
        void send(String event, Object data);
    }

    /** Người dùng đóng kết nối giữa chừng — dừng gọi mô hình. */
    public static class ClientGoneException extends RuntimeException {
        public ClientGoneException(Throwable cause) {
            super(cause);
        }
    }

    public boolean isEnabled() {
        return llm.isConfigured();
    }

    /**
     * Lịch sử đưa vào ngữ cảnh. Đã đăng nhập: đọc từ CSDL (kiểm tra quyền, sai -> 404 trước khi mở luồng SSE).
     * Khách vãng lai: dùng vài tin trình duyệt gửi kèm.
     */
    public List<LlmClient.Turn> history(Long userId, AssistantDtos.ChatRequest request) {
        if (userId != null) {
            return request.conversationId() == null ? List.of() : conversations.recentTurns(userId, request.conversationId());
        }
        if (request.history() == null) return List.of();
        return request.history().stream()
                .map(m -> m.fromUser()
                        ? (LlmClient.Turn) new LlmClient.UserTurn(m.content())
                        : new LlmClient.ModelTurn(m.content(), List.of(), null))
                .toList();
    }

    /**
     * Trả lời một câu hỏi. Sự kiện gửi đi: delta {text} (nhiều lần), tours [thẻ tour], customRequest {bản nháp},
     * rồi done {conversationId} hoặc error {message}.
     */
    public void answer(Long userId, AssistantDtos.ChatRequest request, List<LlmClient.Turn> history, Sink sink) {
        String question = request.message().strip();
        List<LlmClient.Turn> turns = new ArrayList<>(history);
        turns.add(new LlmClient.UserTurn(question));

        AssistantTools.Context ctx = new AssistantTools.Context();
        StringBuilder answer = new StringBuilder();
        String system = AssistantPrompt.system(LocalDate.now(clock));
        List<LlmClient.Tool> declarations = tools.declarations();

        try {
            for (int round = 0; ; round++) {
                // Hết lượt gọi hàm: bắt mô hình trả lời bằng dữ liệu đã có
                List<LlmClient.Tool> available = round < MAX_TOOL_ROUNDS ? declarations : List.of();
                LlmClient.ModelTurn reply = llm.chat(new LlmClient.ChatRequest(system, turns, available), delta -> {
                    answer.append(delta);
                    sink.send("delta", Map.of("text", delta));
                });
                turns.add(reply);
                if (!reply.wantsTools() || available.isEmpty()) break;

                List<LlmClient.FunctionResult> results = new ArrayList<>();
                for (LlmClient.FunctionCall call : reply.calls()) {
                    log.debug("Trợ lý gọi {} {}", call.name(), call.args());
                    results.add(new LlmClient.FunctionResult(call.id(), call.name(), tools.execute(call, ctx)));
                }
                turns.add(new LlmClient.ToolResultsTurn(results));
            }
        } catch (AiUnavailableException e) {
            log.warn("Trợ lý không trả lời được: {}", e.getMessage());
            sink.send("error", Map.of("message", BUSY_MESSAGE));
            return;
        }

        if (answer.toString().isBlank()) {
            answer.append(EMPTY_ANSWER);
            sink.send("delta", Map.of("text", EMPTY_ANSWER));
        }

        AssistantDtos.Attachments attachments = new AssistantDtos.Attachments(
                referencedTours(answer.toString(), ctx), ctx.customRequest);
        if (!attachments.tours().isEmpty()) sink.send("tours", attachments.tours());
        if (attachments.customRequest() != null) sink.send("customRequest", attachments.customRequest());

        Long conversationId = null;
        if (userId != null) {
            conversationId = conversations.saveExchange(userId, request.conversationId(), question, answer.toString(), attachments);
        }
        sink.send("done", conversationId == null ? Map.of() : Map.of("conversationId", conversationId));
    }

    /** Thẻ của các tour được nhắc trong câu trả lời (theo thứ tự xuất hiện; mã tour bịa ra thì bỏ). */
    static List<SearchResponses.TourCard> referencedTours(String answer, AssistantTools.Context ctx) {
        Set<Long> ids = new LinkedHashSet<>();
        Matcher m = TOUR_REF.matcher(answer);
        while (m.find()) {
            try {
                ids.add(Long.parseLong(m.group(1)));
            } catch (NumberFormatException ignored) {
                // số quá dài — không phải mã tour
            }
        }
        return ids.stream().map(ctx.tours::get).filter(c -> c != null).toList();
    }
}
