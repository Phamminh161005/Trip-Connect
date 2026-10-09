package com.tripconnect.backend.controller;

import com.tripconnect.backend.ai.LlmClient;
import com.tripconnect.backend.ai.assistant.AssistantConversationService;
import com.tripconnect.backend.ai.assistant.AssistantService;
import com.tripconnect.backend.dto.assistant.AssistantDtos;
import com.tripconnect.backend.exception.TooManyRequestsException;
import com.tripconnect.backend.security.AuthenticatedUser;
import com.tripconnect.backend.security.RateLimiter;
import jakarta.annotation.PreDestroy;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Trợ lý AI cho khách. Không bắt buộc đăng nhập; đã đăng nhập thì lưu lịch sử trò chuyện.
 * Câu trả lời trả về dạng Server-Sent Events để chữ hiện dần.
 */
@Slf4j
@RestController
@RequestMapping("/api/assistant")
public class AssistantController {

    static final int GUEST_DAILY_LIMIT = 10;
    static final int USER_DAILY_LIMIT = 50;
    /** Nhiều mã khách vãng lai chung một IP (đổi mã để lách giới hạn). */
    static final int IP_DAILY_LIMIT = 60;
    private static final Duration ONE_DAY = Duration.ofDays(1);
    private static final long STREAM_TIMEOUT_MS = Duration.ofMinutes(3).toMillis();

    private final AssistantService assistantService;
    private final AssistantConversationService conversationService;
    private final RateLimiter rateLimiter;

    /** Luồng chạy câu trả lời (gọi mô hình mất vài giây đến vài chục giây — không giữ luồng xử lý request). */
    private final ThreadPoolExecutor executor;

    public AssistantController(AssistantService assistantService, AssistantConversationService conversationService,
                               RateLimiter rateLimiter) {
        this.assistantService = assistantService;
        this.conversationService = conversationService;
        this.rateLimiter = rateLimiter;
        AtomicInteger counter = new AtomicInteger();
        this.executor = new ThreadPoolExecutor(4, 16, 60, TimeUnit.SECONDS, new ArrayBlockingQueue<>(32), r -> {
            Thread t = new Thread(r, "assistant-" + counter.incrementAndGet());
            t.setDaemon(true);
            return t;
        });
    }

    @PreDestroy
    void shutdown() {
        executor.shutdownNow();
    }

    @GetMapping("/status")
    public AssistantDtos.Status status(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        return new AssistantDtos.Status(assistantService.isEnabled(), currentUser != null,
                currentUser != null ? USER_DAILY_LIMIT : GUEST_DAILY_LIMIT);
    }

    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> chat(@Valid @RequestBody AssistantDtos.ChatRequest request,
                                           @AuthenticationPrincipal AuthenticatedUser currentUser,
                                           @RequestHeader(value = TourController.VISITOR_HEADER, required = false) String visitorId,
                                           HttpServletRequest http) {
        if (!assistantService.isEnabled()) {
            throw new IllegalStateException("Trợ lý AI hiện chưa bật");
        }
        Long userId = currentUser == null ? null : currentUser.userId();
        if (userId != null) {
            rateLimiter.check("assistant-user", userId.toString(), USER_DAILY_LIMIT, ONE_DAY);
        } else {
            rateLimiter.check("assistant-ip", http.getRemoteAddr(), IP_DAILY_LIMIT, ONE_DAY);
            rateLimiter.check("assistant-visitor", visitorId == null || visitorId.isBlank() ? http.getRemoteAddr() : visitorId,
                    GUEST_DAILY_LIMIT, ONE_DAY);
        }
        // Đọc lịch sử (và kiểm tra quyền với cuộc trò chuyện) trước khi mở luồng để lỗi trả về đúng mã HTTP
        List<LlmClient.Turn> history = assistantService.history(userId, request);

        SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MS);
        AssistantService.Sink sink = (event, data) -> {
            try {
                emitter.send(SseEmitter.event().name(event).data(data, MediaType.APPLICATION_JSON));
            } catch (IOException | IllegalStateException e) {
                throw new AssistantService.ClientGoneException(e);
            }
        };
        try {
            executor.execute(() -> {
                try {
                    assistantService.answer(userId, request, history, sink);
                    emitter.complete();
                } catch (AssistantService.ClientGoneException e) {
                    log.debug("Người dùng đã đóng trợ lý giữa chừng");
                    emitter.complete();
                } catch (RuntimeException e) {
                    log.error("Lỗi khi trợ lý trả lời", e);
                    try {
                        sink.send("error", Map.of("message", "Đã có lỗi xảy ra, bạn thử lại nhé."));
                    } catch (AssistantService.ClientGoneException ignored) {
                        // đã đóng
                    }
                    emitter.complete();
                }
            });
        } catch (RejectedExecutionException e) {
            throw new TooManyRequestsException("Trợ lý đang quá tải, bạn thử lại sau ít phút nhé");
        }
        return ResponseEntity.status(HttpStatus.OK)
                .header("Cache-Control", "no-cache")
                // Tắt gom bộ đệm ở proxy (nginx) để chữ hiện dần
                .header("X-Accel-Buffering", "no")
                .body(emitter);
    }

    // ===================== Lịch sử (cần đăng nhập — chặn ở SecurityConfig) =====================

    @GetMapping("/conversations")
    public List<AssistantDtos.ConversationSummary> conversations(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        return conversationService.list(currentUser.userId());
    }

    @GetMapping("/conversations/{id}")
    public AssistantDtos.Conversation conversation(@PathVariable Long id, @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return conversationService.get(currentUser.userId(), id);
    }

    @DeleteMapping("/conversations/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal AuthenticatedUser currentUser) {
        conversationService.delete(currentUser.userId(), id);
        return ResponseEntity.noContent().build();
    }
}
