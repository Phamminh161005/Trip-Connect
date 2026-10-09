package com.tripconnect.backend.ai.assistant;

import com.tripconnect.backend.ai.LlmClient;
import com.tripconnect.backend.dto.assistant.AssistantDtos;
import com.tripconnect.backend.entity.AiConversation;
import com.tripconnect.backend.entity.AiMessage;
import com.tripconnect.backend.exception.ResourceNotFoundException;
import com.tripconnect.backend.repository.AiConversationRepository;
import com.tripconnect.backend.repository.AiMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Lưu lịch sử trò chuyện với trợ lý của người đã đăng nhập. */
@Slf4j
@Service
@RequiredArgsConstructor
public class AssistantConversationService {

    /** Số tin gần nhất đưa vào ngữ cảnh khi hỏi tiếp. */
    static final int CONTEXT_MESSAGES = 10;
    static final int LIST_LIMIT = 30;
    static final int TITLE_MAX = 80;

    private final AiConversationRepository conversationRepository;
    private final AiMessageRepository messageRepository;
    private final JsonMapper jsonMapper;
    private final Clock clock;

    /** Các tin gần nhất của cuộc trò chuyện (cũ trước) dưới dạng lượt hội thoại. Không phải của mình -> 404. */
    @Transactional(readOnly = true)
    public List<LlmClient.Turn> recentTurns(Long userId, Long conversationId) {
        requireOwned(userId, conversationId);
        List<AiMessage> recent = new ArrayList<>(messageRepository.findRecent(conversationId, PageRequest.of(0, CONTEXT_MESSAGES)));
        Collections.reverse(recent);
        return recent.stream().map(m -> m.getRole() == AiMessage.Role.USER
                ? (LlmClient.Turn) new LlmClient.UserTurn(m.getContent())
                : new LlmClient.ModelTurn(m.getContent(), List.of(), null)).toList();
    }

    /** Lưu một lượt hỏi - đáp; conversationId null thì tạo cuộc mới. Trả về mã cuộc trò chuyện. */
    @Transactional
    public Long saveExchange(Long userId, Long conversationId, String question, String answer, AssistantDtos.Attachments attachments) {
        LocalDateTime now = LocalDateTime.now(clock);
        AiConversation conversation;
        if (conversationId == null) {
            conversation = new AiConversation();
            conversation.setUserId(userId);
            conversation.setTitle(title(question));
            conversation.setCreatedAt(now);
        } else {
            conversation = requireOwned(userId, conversationId);
        }
        conversation.setUpdatedAt(now);
        conversation = conversationRepository.save(conversation);

        messageRepository.save(message(conversation.getId(), AiMessage.Role.USER, question, null, now));
        messageRepository.save(message(conversation.getId(), AiMessage.Role.ASSISTANT, answer,
                attachments == null || attachments.isEmpty() ? null : jsonMapper.writeValueAsString(attachments), now));
        return conversation.getId();
    }

    @Transactional(readOnly = true)
    public List<AssistantDtos.ConversationSummary> list(Long userId) {
        return conversationRepository.findByUserIdOrderByUpdatedAtDesc(userId, PageRequest.of(0, LIST_LIMIT)).stream()
                .map(c -> new AssistantDtos.ConversationSummary(c.getId(), c.getTitle(), c.getUpdatedAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public AssistantDtos.Conversation get(Long userId, Long conversationId) {
        AiConversation c = requireOwned(userId, conversationId);
        List<AssistantDtos.Message> messages = messageRepository.findByConversationIdOrderByIdAsc(conversationId).stream()
                .map(m -> new AssistantDtos.Message(m.getId(), m.getRole() == AiMessage.Role.USER, m.getContent(),
                        readAttachments(m.getAttachments()), m.getCreatedAt()))
                .toList();
        return new AssistantDtos.Conversation(c.getId(), c.getTitle(), messages);
    }

    @Transactional
    public void delete(Long userId, Long conversationId) {
        // Tin nhắn bị xóa theo (ON DELETE CASCADE)
        conversationRepository.delete(requireOwned(userId, conversationId));
    }

    private AiConversation requireOwned(Long userId, Long conversationId) {
        return conversationRepository.findByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy cuộc trò chuyện"));
    }

    private AssistantDtos.Attachments readAttachments(String json) {
        if (json == null) return null;
        try {
            return jsonMapper.readValue(json, AssistantDtos.Attachments.class);
        } catch (JacksonException e) {
            log.warn("Không đọc được phần đính kèm của tin trợ lý: {}", e.getMessage());
            return null;
        }
    }

    private static AiMessage message(Long conversationId, AiMessage.Role role, String content, String attachments, LocalDateTime now) {
        AiMessage m = new AiMessage();
        m.setConversationId(conversationId);
        m.setRole(role);
        m.setContent(content);
        m.setAttachments(attachments);
        m.setCreatedAt(now);
        return m;
    }

    static String title(String question) {
        String oneLine = question.strip().replaceAll("\\s+", " ");
        return oneLine.length() <= TITLE_MAX ? oneLine : oneLine.substring(0, TITLE_MAX - 1) + "…";
    }
}
