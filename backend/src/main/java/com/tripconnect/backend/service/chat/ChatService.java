package com.tripconnect.backend.service.chat;

import com.tripconnect.backend.dto.chat.ChatResponses;
import com.tripconnect.backend.entity.ChatMessage;
import com.tripconnect.backend.entity.ChatMessageImage;
import com.tripconnect.backend.entity.ChatReadState;
import com.tripconnect.backend.entity.CustomRequest;
import com.tripconnect.backend.enums.CustomRequestStatus;
import com.tripconnect.backend.enums.NotificationType;
import com.tripconnect.backend.exception.ForbiddenException;
import com.tripconnect.backend.exception.ResourceNotFoundException;
import com.tripconnect.backend.realtime.RealtimePublisher;
import com.tripconnect.backend.repository.ChatMessageRepository;
import com.tripconnect.backend.repository.ChatReadStateRepository;
import com.tripconnect.backend.repository.CustomRequestRepository;
import com.tripconnect.backend.repository.NotificationRepository;
import com.tripconnect.backend.service.DisplayNames;
import com.tripconnect.backend.service.NotificationEvents;
import com.tripconnect.backend.service.WebNotifications;
import com.tripconnect.backend.service.customrequest.CustomRequestAssembler;
import com.tripconnect.backend.storage.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Trao đổi giữa khách và đơn vị tổ chức trong một yêu cầu tour riêng. Cuộc trò chuyện = (yêu cầu, đơn vị):
 *  - Khách: xem mọi cuộc của yêu cầu mình, gửi được ở cuộc với đơn vị đang phụ trách.
 *  - Agent: chỉ cuộc của mình, khi đang phụ trách (bị thay thì mất quyền xem).
 *  - Admin: xem mọi cuộc (bản gốc, không che), không gửi.
 * Gửi được khi yêu cầu đang xử lý / đã chốt; hủy / đóng thì chỉ xem.
 */
@Service
@RequiredArgsConstructor
public class ChatService {

    public enum Viewer { CUSTOMER, AGENT, ADMIN }

    public static final int MAX_LENGTH = 2000;
    public static final int MAX_IMAGES = 5;
    static final int DEFAULT_PAGE = 30;
    static final int MAX_PAGE = 100;
    static final Set<CustomRequestStatus> WRITABLE = EnumSet.of(CustomRequestStatus.IN_PROGRESS, CustomRequestStatus.AGREED);

    private final CustomRequestRepository requestRepository;
    private final ChatMessageRepository messageRepository;
    private final ChatReadStateRepository readStateRepository;
    private final NotificationRepository notificationRepository;
    private final CustomRequestAssembler assembler;
    private final FileStorageService fileStorageService;
    private final FileValidator fileValidator;
    private final TransactionalFileCleanup fileCleanup;
    private final RealtimePublisher realtimePublisher;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    /** Một cuộc trò chuyện người xem được mở. */
    record Conversation(CustomRequest request, Long agentId, Viewer viewer, Long viewerId, boolean canWrite) {

        Long customerId() {
            return request.getCustomer().getId();
        }

        /** Người còn lại (khách <-> Agent). */
        Long counterpartId() {
            return viewer == Viewer.CUSTOMER ? agentId : customerId();
        }
    }

    /** Đơn vị đang phụ trách và đã nhận yêu cầu (null nếu chưa có). */
    static Long currentAgentId(CustomRequest r) {
        return r.getAgent() != null && r.getAcceptedAt() != null ? r.getAgent().getId() : null;
    }

    Conversation open(CustomRequest r, Viewer viewer, Long viewerId, Long agentId) {
        Long current = currentAgentId(r);
        return switch (viewer) {
            case CUSTOMER -> {
                if (!r.getCustomer().getId().equals(viewerId)) throw notFound();
                Long target = agentId != null ? agentId : current;
                if (target == null || (!target.equals(current) && !messageRepository.findAgentIds(r.getId()).contains(target))) {
                    throw notFound();
                }
                yield new Conversation(r, target, viewer, viewerId, target.equals(current) && WRITABLE.contains(r.getStatus()));
            }
            case AGENT -> {
                if (current == null || !current.equals(viewerId)) throw notFound();
                yield new Conversation(r, viewerId, viewer, viewerId, WRITABLE.contains(r.getStatus()));
            }
            case ADMIN -> {
                Long target = agentId != null ? agentId : current;
                if (target == null) throw notFound();
                yield new Conversation(r, target, viewer, viewerId, false);
            }
        };
    }

    private static ResourceNotFoundException notFound() {
        return new ResourceNotFoundException("Không tìm thấy cuộc trò chuyện");
    }

    private CustomRequest load(Long requestId) {
        return requestRepository.findById(requestId).orElseThrow(ChatService::notFound);
    }

    // ===================== Xem =====================

    /** Các cuộc trò chuyện của yêu cầu: cuộc với đơn vị đang phụ trách đứng đầu. */
    @Transactional(readOnly = true)
    public List<ChatResponses.Thread> threads(Viewer viewer, Long viewerId, Long requestId) {
        CustomRequest r = load(requestId);
        Long current = currentAgentId(r);
        List<Long> agentIds;
        if (viewer == Viewer.AGENT) {
            agentIds = List.of(open(r, viewer, viewerId, null).agentId());
        } else {
            if (viewer == Viewer.CUSTOMER && !r.getCustomer().getId().equals(viewerId)) throw notFound();
            LinkedHashSet<Long> ids = new LinkedHashSet<>();
            if (current != null) ids.add(current);
            List<Long> previous = new ArrayList<>(messageRepository.findAgentIds(requestId));
            Collections.reverse(previous);
            ids.addAll(previous);
            agentIds = List.copyOf(ids);
        }
        if (agentIds.isEmpty()) return List.of();

        Map<Long, String> names = assembler.companyNames(agentIds);
        String customerName = viewer == Viewer.AGENT
                ? DisplayNames.masked(r.getCustomer().getFullName()) : r.getCustomer().getFullName();
        return agentIds.stream().map(agentId -> {
            Conversation c = new Conversation(r, agentId, viewer, viewerId,
                    viewer != Viewer.ADMIN && agentId.equals(current) && WRITABLE.contains(r.getStatus()));
            long myLastRead = viewer == Viewer.ADMIN ? 0 : lastRead(requestId, agentId, viewerId);
            long counterpartLastRead = viewer == Viewer.ADMIN ? 0 : lastRead(requestId, agentId, c.counterpartId());
            long unread = viewer == Viewer.ADMIN ? 0 : messageRepository.countUnread(requestId, agentId, viewerId, myLastRead);
            return new ChatResponses.Thread(agentId, names.getOrDefault(agentId, "Đơn vị tổ chức"), customerName,
                    agentId.equals(current), c.canWrite(), unread, myLastRead, counterpartLastRead);
        }).toList();
    }

    /**
     * Tin của một cuộc trò chuyện, cũ trước mới sau.
     *
     * @param before lấy các tin cũ hơn (cuộn lên); null = mới nhất
     * @param after  lấy các tin mới hơn (tải bù sau khi mất kết nối); ưu tiên hơn before
     */
    @Transactional(readOnly = true)
    public ChatResponses.Page messages(Viewer viewer, Long viewerId, Long requestId, Long agentId, Long before, Long after, Integer size) {
        Conversation c = open(load(requestId), viewer, viewerId, agentId);
        int limit = Math.max(1, Math.min(size == null ? DEFAULT_PAGE : size, MAX_PAGE));
        boolean mask = viewer != Viewer.ADMIN;
        List<ChatMessage> rows;
        boolean hasMore;
        if (after != null) {
            rows = messageRepository.findNewer(requestId, c.agentId(), after, PageRequest.of(0, limit + 1));
            hasMore = rows.size() > limit;
            if (hasMore) rows = rows.subList(0, limit);
        } else {
            List<ChatMessage> newestFirst = messageRepository.findOlder(requestId, c.agentId(), before, PageRequest.of(0, limit + 1));
            hasMore = newestFirst.size() > limit;
            rows = new ArrayList<>(hasMore ? newestFirst.subList(0, limit) : newestFirst);
            Collections.reverse(rows);
        }
        Long customerId = c.customerId();
        return new ChatResponses.Page(rows.stream().map(m -> toView(m, customerId, mask, null)).toList(), hasMore);
    }

    // ===================== Gửi =====================

    @Transactional
    public ChatResponses.Message send(Viewer viewer, Long viewerId, Long requestId, String text, List<MultipartFile> images,
                                      String clientId) {
        if (viewer == Viewer.ADMIN) throw new ForbiddenException("Quản trị viên chỉ xem cuộc trò chuyện");
        String body = text == null || text.isBlank() ? null : text.strip();
        List<MultipartFile> files = images == null ? List.of() : images.stream().filter(f -> f != null && !f.isEmpty()).toList();
        if (body == null && files.isEmpty()) throw new IllegalArgumentException("Tin nhắn trống");
        if (body != null && body.length() > MAX_LENGTH) throw new IllegalArgumentException("Tin nhắn tối đa " + MAX_LENGTH + " ký tự");
        if (files.size() > MAX_IMAGES) throw new IllegalArgumentException("Mỗi tin tối đa " + MAX_IMAGES + " ảnh");
        files.forEach(f -> fileValidator.validate(f, FileRule.IMAGE));

        // Khóa yêu cầu: tuần tự với các thao tác đề xuất / đóng yêu cầu
        CustomRequest r = requestRepository.findByIdForUpdate(requestId).orElseThrow(ChatService::notFound);
        Conversation c = open(r, viewer, viewerId, null);
        if (!c.canWrite()) throw new IllegalStateException("Cuộc trò chuyện đã đóng, bạn chỉ xem được tin cũ");

        LocalDateTime now = LocalDateTime.now(clock);
        Long recipientId = c.counterpartId();
        boolean recipientHadUnread = messageRepository.countUnread(requestId, c.agentId(), recipientId,
                lastRead(requestId, c.agentId(), recipientId)) > 0;

        ChatMessage message = new ChatMessage();
        message.setRequestId(requestId);
        message.setAgentId(c.agentId());
        message.setSenderId(viewerId);
        message.setBody(body);
        message.setCreatedAt(now);
        short order = 0;
        for (MultipartFile file : files) {
            StoredFile stored = fileStorageService.upload(file, "chat/" + requestId, FileVisibility.PUBLIC);
            fileCleanup.deleteOnRollback(stored.publicId(), FileVisibility.PUBLIC);
            ChatMessageImage image = new ChatMessageImage();
            image.setMessage(message);
            image.setPublicId(stored.publicId());
            image.setFormat(stored.format());
            image.setSizeBytes(stored.sizeBytes());
            image.setSortOrder(order++);
            message.getImages().add(image);
        }
        messageRepository.save(message);

        // Tin nhắn là một lần trao đổi: yêu cầu không bị tự đóng vì "không hoạt động"
        if (r.getStatus() == CustomRequestStatus.IN_PROGRESS) r.setLastActivityAt(now);
        saveLastRead(requestId, c.agentId(), viewerId, message.getId(), now);

        // Chỉ báo chuông ở tin đầu tiên của một lượt chưa đọc, tránh mỗi tin một thông báo
        if (!recipientHadUnread) {
            String senderName = viewer == Viewer.CUSTOMER
                    ? DisplayNames.masked(r.getCustomer().getFullName()) : assembler.companyName(r.getAgent());
            eventPublisher.publishEvent(new NotificationEvents.UserWebEvent(recipientId,
                    WebNotifications.chatMessage(chatLink(r.getId(), viewer == Viewer.AGENT), r.getCode(), senderName)));
        }

        ChatResponses.Message view = toView(message, c.customerId(), true, clientId);
        realtimePublisher.publish(List.of(c.customerId(), c.agentId()), RealtimePublisher.Type.CHAT_MESSAGE, view);
        return view;
    }

    /** Trang chi tiết yêu cầu của người nhận (mở thẳng khung chat). */
    static String chatLink(Long requestId, boolean forCustomer) {
        return (forCustomer ? "/account/requests/" : "/agent/requests/") + requestId + "#chat";
    }

    // ===================== Đã đọc =====================

    @Transactional
    public void markRead(Viewer viewer, Long viewerId, Long requestId, Long agentId, long lastMessageId) {
        if (viewer == Viewer.ADMIN) return;
        Conversation c = open(load(requestId), viewer, viewerId, agentId);
        LocalDateTime now = LocalDateTime.now(clock);
        if (!saveLastRead(requestId, c.agentId(), viewerId, lastMessageId, now)) return;
        notificationRepository.markReadByLink(viewerId, NotificationType.CHAT_MESSAGE,
                chatLink(requestId, viewer == Viewer.CUSTOMER), now);
        realtimePublisher.publish(List.of(c.customerId(), c.agentId()), RealtimePublisher.Type.CHAT_READ,
                new ChatResponses.Read(requestId, c.agentId(), viewerId, lastMessageId));
    }

    private long lastRead(Long requestId, Long agentId, Long userId) {
        return readStateRepository.findById(new ChatReadState.Key(requestId, agentId, userId))
                .map(ChatReadState::getLastReadMessageId).orElse(0L);
    }

    /** Chỉ tiến lên, không lùi. Trả về false nếu không có gì thay đổi. */
    private boolean saveLastRead(Long requestId, Long agentId, Long userId, long messageId, LocalDateTime now) {
        ChatReadState.Key key = new ChatReadState.Key(requestId, agentId, userId);
        ChatReadState state = readStateRepository.findById(key).orElseGet(() -> {
            ChatReadState created = new ChatReadState();
            created.setId(key);
            return created;
        });
        if (state.getUpdatedAt() != null && state.getLastReadMessageId() >= messageId) return false;
        state.setLastReadMessageId(messageId);
        state.setUpdatedAt(now);
        readStateRepository.save(state);
        return true;
    }

    private ChatResponses.Message toView(ChatMessage m, Long customerId, boolean mask, String clientId) {
        String body = mask ? ContactMasker.mask(m.getBody()) : m.getBody();
        boolean masked = body != null && !body.equals(m.getBody());
        List<ChatResponses.Image> images = m.getImages().stream()
                .map(i -> new ChatResponses.Image(i.getId(), fileStorageService.publicUrl(i.getPublicId(), i.getFormat())))
                .toList();
        return new ChatResponses.Message(m.getId(), m.getRequestId(), m.getAgentId(), m.getSenderId(),
                m.getSenderId().equals(customerId), body, masked, images, m.getCreatedAt(), clientId);
    }
}
