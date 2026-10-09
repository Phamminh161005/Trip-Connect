package com.tripconnect.backend.ai.assistant;

import com.tripconnect.backend.ai.AiUnavailableException;
import com.tripconnect.backend.ai.LlmClient;
import com.tripconnect.backend.dto.assistant.AssistantDtos;
import com.tripconnect.backend.dto.search.SearchResponses;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

/** Vòng hỏi - đáp của trợ lý: gọi hàm, đẩy sự kiện, lưu lịch sử, xử lý mô hình lỗi. */
@ExtendWith(MockitoExtension.class)
class AssistantServiceTest {

    private static final LlmClient.Tool TOOL = new LlmClient.Tool("search_tours", "Tìm tour", Map.of("type", "OBJECT"));

    @Mock private AssistantTools tools;
    @Mock private AssistantConversationService conversations;

    private ScriptedLlm llm;
    private AssistantService service;
    private final List<String[]> events = new ArrayList<>();
    private final List<Object> payloads = new ArrayList<>();

    @BeforeEach
    void setUp() {
        llm = new ScriptedLlm();
        Clock clock = Clock.fixed(Instant.parse("2026-10-07T03:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));
        service = new AssistantService(llm, tools, conversations, clock);
        lenient().when(tools.declarations()).thenReturn(List.of(TOOL));
    }

    private final AssistantService.Sink sink = (event, data) -> {
        events.add(new String[]{event});
        payloads.add(data);
    };

    private List<String> eventNames() {
        return events.stream().map(e -> e[0]).toList();
    }

    private static AssistantDtos.ChatRequest ask(String message) {
        return new AssistantDtos.ChatRequest(null, message, null);
    }

    private static SearchResponses.TourCard card(long id) {
        return new SearchResponses.TourCard(id, "Tour " + id, null, 3, 2, "Hà Nội", List.of("Quảng Ninh"), false,
                List.of(), null, 0, "TripConnect", 2_000_000, LocalDate.of(2026, 11, 1), 2);
    }

    @Test
    void runsToolThenStreamsAnswerWithOnlyKnownTourCards() {
        llm.reply(r -> new LlmClient.ModelTurn("", List.of(new LlmClient.FunctionCall("c1", "search_tours", Map.of("destination", "Quảng Ninh"))), null));
        llm.reply(r -> {
            r.onText().accept("Có tour Hạ Long [tour:9]");
            r.onText().accept(" và tour lạ [tour:999].");
            return new LlmClient.ModelTurn("Có tour Hạ Long [tour:9] và tour lạ [tour:999].", List.of(), null);
        });
        doAnswer(inv -> {
            AssistantTools.Context ctx = inv.getArgument(1);
            ctx.tours.put(9L, card(9));
            return Map.of("tours", List.of());
        }).when(tools).execute(any(), any());

        service.answer(null, ask("Tour Hạ Long?"), List.of(), sink);

        assertThat(eventNames()).containsExactly("delta", "delta", "tours", "done");
        @SuppressWarnings("unchecked")
        List<SearchResponses.TourCard> cards = (List<SearchResponses.TourCard>) payloads.get(2);
        assertThat(cards).extracting(SearchResponses.TourCard::id).containsExactly(9L);
        // Lượt 2 gửi kèm kết quả hàm
        assertThat(llm.requests.get(1).turns()).hasSize(3).last().isInstanceOf(LlmClient.ToolResultsTurn.class);
        // Khách vãng lai: không lưu
        verifyNoInteractions(conversations);
    }

    @Test
    void signedInUserExchangeIsSaved() {
        llm.reply(r -> {
            r.onText().accept("Xin chào!");
            return new LlmClient.ModelTurn("Xin chào!", List.of(), null);
        });
        when(conversations.saveExchange(eq(5L), isNull(), eq("Chào"), eq("Xin chào!"), any())).thenReturn(42L);

        service.answer(5L, ask("  Chào  "), List.of(), sink);

        assertThat(eventNames()).containsExactly("delta", "done");
        assertThat(payloads.get(1)).isEqualTo(Map.of("conversationId", 42L));
    }

    @Test
    void customRequestDraftIsSentAndSaved() {
        AssistantDtos.CustomRequestDraft draft = new AssistantDtos.CustomRequestDraft(null, null, List.of(2L), List.of("Tỉnh Hà Giang"),
                null, null, 5, 6, null, null, null, null);
        llm.reply(r -> new LlmClient.ModelTurn("", List.of(new LlmClient.FunctionCall("c1", "suggest_custom_request", Map.of())), null));
        llm.reply(r -> {
            r.onText().accept("Bạn bấm nút bên dưới nhé.");
            return new LlmClient.ModelTurn("Bạn bấm nút bên dưới nhé.", List.of(), null);
        });
        doAnswer(inv -> {
            AssistantTools.Context ctx = inv.getArgument(1);
            ctx.customRequest = draft;
            return Map.of("shown", true);
        }).when(tools).execute(any(), any());
        when(conversations.saveExchange(anyLong(), any(), any(), any(), any())).thenReturn(1L);

        service.answer(5L, ask("Đi Hà Giang riêng"), List.of(), sink);

        assertThat(eventNames()).containsExactly("delta", "customRequest", "done");
        ArgumentCaptor<AssistantDtos.Attachments> saved = ArgumentCaptor.forClass(AssistantDtos.Attachments.class);
        verify(conversations).saveExchange(eq(5L), isNull(), any(), any(), saved.capture());
        assertThat(saved.getValue().customRequest()).isEqualTo(draft);
    }

    @Test
    void stopsOfferingToolsAfterMaxRounds() {
        for (int i = 0; i < AssistantService.MAX_TOOL_ROUNDS; i++) {
            llm.reply(r -> new LlmClient.ModelTurn("", List.of(new LlmClient.FunctionCall(null, "search_tours", Map.of())), null));
        }
        llm.reply(r -> {
            r.onText().accept("Đây là kết quả.");
            return new LlmClient.ModelTurn("Đây là kết quả.", List.of(), null);
        });
        when(tools.execute(any(), any())).thenReturn(Map.of());

        service.answer(null, ask("Tìm tour"), List.of(), sink);

        assertThat(llm.requests).hasSize(AssistantService.MAX_TOOL_ROUNDS + 1);
        assertThat(llm.requests.get(AssistantService.MAX_TOOL_ROUNDS - 1).tools()).isNotEmpty();
        assertThat(llm.requests.get(AssistantService.MAX_TOOL_ROUNDS).tools()).isEmpty();
        verify(tools, times(AssistantService.MAX_TOOL_ROUNDS)).execute(any(), any());
    }

    @Test
    void modelUnavailableSendsBusyErrorAndSavesNothing() {
        llm.reply(r -> {
            throw new AiUnavailableException("503", false);
        });

        service.answer(5L, ask("Chào"), List.of(), sink);

        assertThat(eventNames()).containsExactly("error");
        assertThat(payloads.get(0)).isEqualTo(Map.of("message", AssistantService.BUSY_MESSAGE));
        verifyNoInteractions(conversations);
    }

    @Test
    void emptyAnswerGetsFallbackText() {
        llm.reply(r -> new LlmClient.ModelTurn("", List.of(), null));

        service.answer(null, ask("..."), List.of(), sink);

        assertThat(eventNames()).containsExactly("delta", "done");
        assertThat(payloads.get(0)).isEqualTo(Map.of("text", AssistantService.EMPTY_ANSWER));
    }

    @Test
    void historyGoesBeforeTheNewQuestion() {
        llm.reply(r -> new LlmClient.ModelTurn("ok", List.of(), null));
        List<LlmClient.Turn> history = List.of(new LlmClient.UserTurn("Tour Đà Lạt?"), new LlmClient.ModelTurn("Có [tour:11]", List.of(), null));

        service.answer(null, ask("Tour đó đi máy bay không?"), history, sink);

        List<LlmClient.Turn> sent = llm.requests.get(0).turns();
        assertThat(sent).hasSize(3);
        assertThat(sent.get(2)).isEqualTo(new LlmClient.UserTurn("Tour đó đi máy bay không?"));
        assertThat(llm.requests.get(0).systemInstruction()).contains("07/10/2026");
    }

    @Test
    void guestHistoryComesFromRequestAndSignedInFromDatabase() {
        AssistantDtos.ChatRequest guest = new AssistantDtos.ChatRequest(null, "Tiếp", List.of(
                new AssistantDtos.HistoryMessage(true, "Hỏi"), new AssistantDtos.HistoryMessage(false, "Đáp")));
        assertThat(service.history(null, guest)).containsExactly(
                new LlmClient.UserTurn("Hỏi"), new LlmClient.ModelTurn("Đáp", List.of(), null));

        AssistantDtos.ChatRequest user = new AssistantDtos.ChatRequest(7L, "Tiếp", guest.history());
        when(conversations.recentTurns(5L, 7L)).thenReturn(List.of(new LlmClient.UserTurn("Từ CSDL")));
        assertThat(service.history(5L, user)).containsExactly(new LlmClient.UserTurn("Từ CSDL"));

        // Cuộc mới của người đăng nhập: bỏ qua lịch sử trình duyệt gửi
        assertThat(service.history(5L, new AssistantDtos.ChatRequest(null, "Hỏi", guest.history()))).isEmpty();
    }

    @Test
    void referencedToursKeepOrderAndDropDuplicates() {
        AssistantTools.Context ctx = new AssistantTools.Context();
        ctx.tours.put(1L, card(1));
        ctx.tours.put(2L, card(2));
        assertThat(AssistantService.referencedTours("B [tour:2], A [tour:1], lại B [tour:2]", ctx))
                .extracting(SearchResponses.TourCard::id).containsExactly(2L, 1L);
        assertThat(AssistantService.referencedTours("không có mã", ctx)).isEmpty();
    }

    /** Mô hình giả trả lời theo kịch bản, ghi lại các yêu cầu nhận được. */
    private static final class ScriptedLlm implements LlmClient {
        record Call(ChatRequest request, Consumer<String> onText) {
        }

        final List<ChatRequest> requests = new ArrayList<>();
        private final Deque<Function<Call, ModelTurn>> script = new ArrayDeque<>();

        void reply(Function<Call, ModelTurn> step) {
            script.add(step);
        }

        @Override
        public boolean isConfigured() {
            return true;
        }

        @Override
        public List<float[]> embed(List<String> texts, EmbedTask task) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ModelTurn chat(ChatRequest request, Consumer<String> onText) {
            // Chép lại danh sách lượt: service tiếp tục thêm vào danh sách gốc sau khi gọi
            requests.add(new ChatRequest(request.systemInstruction(), List.copyOf(request.turns()), request.tools()));
            Function<Call, ModelTurn> step = script.poll();
            if (step == null) throw new AssertionError("Mô hình bị gọi nhiều hơn kịch bản");
            return step.apply(new Call(request, onText));
        }
    }
}
