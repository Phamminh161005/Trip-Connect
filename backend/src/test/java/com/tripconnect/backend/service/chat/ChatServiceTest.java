package com.tripconnect.backend.service.chat;

import com.tripconnect.backend.dto.chat.ChatResponses;
import com.tripconnect.backend.entity.ChatMessage;
import com.tripconnect.backend.entity.ChatReadState;
import com.tripconnect.backend.entity.CustomRequest;
import com.tripconnect.backend.entity.User;
import com.tripconnect.backend.enums.CustomRequestStatus;
import com.tripconnect.backend.exception.ForbiddenException;
import com.tripconnect.backend.exception.ResourceNotFoundException;
import com.tripconnect.backend.realtime.RealtimePublisher;
import com.tripconnect.backend.repository.ChatMessageRepository;
import com.tripconnect.backend.repository.ChatReadStateRepository;
import com.tripconnect.backend.repository.CustomRequestRepository;
import com.tripconnect.backend.repository.NotificationRepository;
import com.tripconnect.backend.service.NotificationEvents;
import com.tripconnect.backend.service.chat.ChatService.Viewer;
import com.tripconnect.backend.service.customrequest.CustomRequestAssembler;
import com.tripconnect.backend.storage.FileStorageService;
import com.tripconnect.backend.storage.FileValidator;
import com.tripconnect.backend.storage.TransactionalFileCleanup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.*;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Quyền xem / gửi, che liên hệ, thông báo và đã đọc của chat trong yêu cầu tour riêng. */
@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 6, 9, 0);
    private static final long CUSTOMER_ID = 5L;
    private static final long AGENT_ID = 9L;
    private static final long OLD_AGENT_ID = 8L;

    @Mock private CustomRequestRepository requestRepository;
    @Mock private ChatMessageRepository messageRepository;
    @Mock private ChatReadStateRepository readStateRepository;
    @Mock private NotificationRepository notificationRepository;
    @Mock private CustomRequestAssembler assembler;
    @Mock private FileStorageService fileStorageService;
    @Mock private FileValidator fileValidator;
    @Mock private TransactionalFileCleanup fileCleanup;
    @Mock private RealtimePublisher realtimePublisher;
    @Mock private ApplicationEventPublisher eventPublisher;

    private ChatService service;
    private CustomRequest request;

    @BeforeEach
    void setUp() {
        service = new ChatService(requestRepository, messageRepository, readStateRepository, notificationRepository, assembler,
                fileStorageService, fileValidator, fileCleanup, realtimePublisher, eventPublisher,
                Clock.fixed(NOW.atZone(VN).toInstant(), VN));
        User customer = new User();
        customer.setId(CUSTOMER_ID);
        customer.setFullName("Nguyễn Thị Lan");
        User agent = new User();
        agent.setId(AGENT_ID);
        request = new CustomRequest();
        request.setId(30L);
        request.setCode("YC261006000001");
        request.setCustomer(customer);
        request.setAgent(agent);
        request.setAcceptedAt(NOW.minusDays(1));
        request.setStatus(CustomRequestStatus.IN_PROGRESS);
        lenient().when(requestRepository.findById(30L)).thenReturn(Optional.of(request));
        lenient().when(requestRepository.findByIdForUpdate(30L)).thenReturn(Optional.of(request));
        lenient().when(readStateRepository.findById(any())).thenReturn(Optional.empty());
        lenient().when(messageRepository.save(any())).thenAnswer(inv -> {
            ChatMessage m = inv.getArgument(0);
            m.setId(100L);
            return m;
        });
        lenient().when(assembler.companyName(any())).thenReturn("Công ty B");
    }

    @Test
    void customerAndCurrentAgentCanWrite_adminOnlyReads_strangersGet404() {
        assertThat(service.open(request, Viewer.CUSTOMER, CUSTOMER_ID, null).canWrite()).isTrue();
        assertThat(service.open(request, Viewer.AGENT, AGENT_ID, null).canWrite()).isTrue();
        assertThat(service.open(request, Viewer.ADMIN, 1L, null).canWrite()).isFalse();

        assertThatThrownBy(() -> service.open(request, Viewer.CUSTOMER, 77L, null)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.open(request, Viewer.AGENT, 77L, null)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void replacedAgentLosesAccess_customerStillReadsOldThreadReadOnly() {
        when(messageRepository.findAgentIds(30L)).thenReturn(List.of(OLD_AGENT_ID, AGENT_ID));

        assertThatThrownBy(() -> service.open(request, Viewer.AGENT, OLD_AGENT_ID, null)).isInstanceOf(ResourceNotFoundException.class);
        ChatService.Conversation old = service.open(request, Viewer.CUSTOMER, CUSTOMER_ID, OLD_AGENT_ID);
        assertThat(old.agentId()).isEqualTo(OLD_AGENT_ID);
        assertThat(old.canWrite()).isFalse();
    }

    @Test
    void closedRequestIsReadOnly() {
        request.setStatus(CustomRequestStatus.CLOSED);
        assertThat(service.open(request, Viewer.AGENT, AGENT_ID, null).canWrite()).isFalse();
        assertThatThrownBy(() -> service.send(Viewer.CUSTOMER, CUSTOMER_ID, 30L, "Xin chào", null, null))
                .hasMessageContaining("chỉ xem");
    }

    @Test
    void send_masksContacts_bumpsActivity_notifiesOnceAndPushesToBothSides() {
        when(messageRepository.countUnread(30L, AGENT_ID, AGENT_ID, 0L)).thenReturn(0L);

        ChatResponses.Message sent = service.send(Viewer.CUSTOMER, CUSTOMER_ID, 30L, "  Gọi mình 0912 345 678 nhé ", null, "tmp-1");

        assertThat(sent.body()).isEqualTo("Gọi mình ••• nhé");
        assertThat(sent.masked()).isTrue();
        assertThat(sent.fromCustomer()).isTrue();
        assertThat(sent.clientId()).isEqualTo("tmp-1");
        ArgumentCaptor<ChatMessage> saved = ArgumentCaptor.forClass(ChatMessage.class);
        verify(messageRepository).save(saved.capture());
        assertThat(saved.getValue().getBody()).isEqualTo("Gọi mình 0912 345 678 nhé");
        assertThat(request.getLastActivityAt()).isEqualTo(NOW);
        verify(eventPublisher).publishEvent(any(NotificationEvents.UserWebEvent.class));
        verify(realtimePublisher).publish(eq(List.of(CUSTOMER_ID, AGENT_ID)), eq(RealtimePublisher.Type.CHAT_MESSAGE), any());
        // Người gửi coi như đã đọc tới tin của mình
        ArgumentCaptor<ChatReadState> read = ArgumentCaptor.forClass(ChatReadState.class);
        verify(readStateRepository).save(read.capture());
        assertThat(read.getValue().getLastReadMessageId()).isEqualTo(100L);
    }

    @Test
    void send_noBellWhenRecipientAlreadyHasUnread() {
        when(messageRepository.countUnread(30L, AGENT_ID, CUSTOMER_ID, 0L)).thenReturn(2L);

        service.send(Viewer.AGENT, AGENT_ID, 30L, "Bên em gửi thêm ảnh khách sạn ạ", null, null);

        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void send_rejectsEmptyTooLongAndAdmin() {
        assertThatThrownBy(() -> service.send(Viewer.CUSTOMER, CUSTOMER_ID, 30L, "   ", List.of(), null)).hasMessageContaining("trống");
        assertThatThrownBy(() -> service.send(Viewer.CUSTOMER, CUSTOMER_ID, 30L, "a".repeat(2001), null, null))
                .hasMessageContaining("2000");
        assertThatThrownBy(() -> service.send(Viewer.ADMIN, 1L, 30L, "Xin chào", null, null)).isInstanceOf(ForbiddenException.class);
        verify(messageRepository, never()).save(any());
    }

    @Test
    void markRead_onlyMovesForward_clearsChatBell_andTellsTheOtherSide() {
        service.markRead(Viewer.AGENT, AGENT_ID, 30L, null, 50L);

        verify(readStateRepository).save(any());
        verify(notificationRepository).markReadByLink(eq(AGENT_ID), any(), eq("/agent/requests/30#chat"), eq(NOW));
        verify(realtimePublisher).publish(eq(List.of(CUSTOMER_ID, AGENT_ID)), eq(RealtimePublisher.Type.CHAT_READ), any());

        ChatReadState existing = new ChatReadState();
        existing.setId(new ChatReadState.Key(30L, AGENT_ID, AGENT_ID));
        existing.setLastReadMessageId(80L);
        existing.setUpdatedAt(NOW);
        when(readStateRepository.findById(existing.getId())).thenReturn(Optional.of(existing));
        clearInvocations(readStateRepository, realtimePublisher);

        service.markRead(Viewer.AGENT, AGENT_ID, 30L, null, 60L);

        verify(readStateRepository, never()).save(any());
        verifyNoInteractions(realtimePublisher);
    }
}
