package com.tripconnect.backend.controller;

import com.tripconnect.backend.dto.chat.ChatRequests;
import com.tripconnect.backend.dto.chat.ChatResponses;
import com.tripconnect.backend.security.AuthenticatedUser;
import com.tripconnect.backend.security.RateLimiter;
import com.tripconnect.backend.service.chat.ChatService;
import com.tripconnect.backend.service.chat.ChatService.Viewer;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.util.List;

/**
 * Trao đổi trong yêu cầu tour riêng. Gửi tin qua đây; tin mới được đẩy tới hai bên qua WebSocket.
 *  - Khách: /api/custom-requests/{id}/chat/...
 *  - Agent: /api/agent/custom-requests/{id}/chat/...
 *  - Admin (chỉ xem): /api/admin/custom-requests/{id}/chat/...
 */
@RestController
@RequiredArgsConstructor
public class CustomRequestChatController {

    private static final String CUSTOMER = "/api/custom-requests/{id}/chat";
    private static final String AGENT = "/api/agent/custom-requests/{id}/chat";
    private static final String ADMIN = "/api/admin/custom-requests/{id}/chat";

    private final ChatService chatService;
    private final RateLimiter rateLimiter;

    // ----- Khách -----

    @GetMapping(CUSTOMER + "/threads")
    public List<ChatResponses.Thread> customerThreads(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id) {
        return chatService.threads(Viewer.CUSTOMER, user.userId(), id);
    }

    @GetMapping(CUSTOMER + "/messages")
    public ChatResponses.Page customerMessages(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id,
                                               @RequestParam(required = false) Long agentId,
                                               @RequestParam(required = false) Long before,
                                               @RequestParam(required = false) Long after,
                                               @RequestParam(required = false) Integer size) {
        return chatService.messages(Viewer.CUSTOMER, user.userId(), id, agentId, before, after, size);
    }

    @PostMapping(value = CUSTOMER + "/messages", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ChatResponses.Message customerSend(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id,
                                              @RequestParam(required = false) String body,
                                              @RequestParam(required = false) List<MultipartFile> images,
                                              @RequestParam(required = false) String clientId) {
        limit(user);
        return chatService.send(Viewer.CUSTOMER, user.userId(), id, body, images, clientId);
    }

    @PostMapping(CUSTOMER + "/read")
    public ResponseEntity<Void> customerRead(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id,
                                             @Valid @RequestBody ChatRequests.Read body) {
        chatService.markRead(Viewer.CUSTOMER, user.userId(), id, body.agentId(), body.lastMessageId());
        return ResponseEntity.noContent().build();
    }

    // ----- Agent -----

    @GetMapping(AGENT + "/threads")
    public List<ChatResponses.Thread> agentThreads(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id) {
        return chatService.threads(Viewer.AGENT, user.userId(), id);
    }

    @GetMapping(AGENT + "/messages")
    public ChatResponses.Page agentMessages(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id,
                                            @RequestParam(required = false) Long before,
                                            @RequestParam(required = false) Long after,
                                            @RequestParam(required = false) Integer size) {
        return chatService.messages(Viewer.AGENT, user.userId(), id, null, before, after, size);
    }

    @PostMapping(value = AGENT + "/messages", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ChatResponses.Message agentSend(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id,
                                           @RequestParam(required = false) String body,
                                           @RequestParam(required = false) List<MultipartFile> images,
                                           @RequestParam(required = false) String clientId) {
        limit(user);
        return chatService.send(Viewer.AGENT, user.userId(), id, body, images, clientId);
    }

    @PostMapping(AGENT + "/read")
    public ResponseEntity<Void> agentRead(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id,
                                          @Valid @RequestBody ChatRequests.Read body) {
        chatService.markRead(Viewer.AGENT, user.userId(), id, null, body.lastMessageId());
        return ResponseEntity.noContent().build();
    }

    // ----- Admin -----

    @GetMapping(ADMIN + "/threads")
    public List<ChatResponses.Thread> adminThreads(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id) {
        return chatService.threads(Viewer.ADMIN, user.userId(), id);
    }

    @GetMapping(ADMIN + "/messages")
    public ChatResponses.Page adminMessages(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id,
                                            @RequestParam(required = false) Long agentId,
                                            @RequestParam(required = false) Long before,
                                            @RequestParam(required = false) Long after,
                                            @RequestParam(required = false) Integer size) {
        return chatService.messages(Viewer.ADMIN, user.userId(), id, agentId, before, after, size);
    }

    /** Chống spam: tối đa 20 tin / phút mỗi người. */
    private void limit(AuthenticatedUser user) {
        rateLimiter.check("chat-send", String.valueOf(user.userId()), 20, Duration.ofMinutes(1));
    }
}
