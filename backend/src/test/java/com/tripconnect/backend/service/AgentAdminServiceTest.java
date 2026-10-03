package com.tripconnect.backend.service;

import com.tripconnect.backend.entity.AgentProfile;
import com.tripconnect.backend.entity.User;
import com.tripconnect.backend.enums.AgentStatus;
import com.tripconnect.backend.repository.AgentDocumentRepository;
import com.tripconnect.backend.repository.AgentProfileRepository;
import com.tripconnect.backend.repository.UserRepository;
import com.tripconnect.backend.service.agent.AgentDocumentStorage;
import com.tripconnect.backend.service.agent.AgentProfileAssembler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgentAdminServiceTest {

    private static final long PROFILE_ID = 3L;
    private static final long ADMIN_ID = 1L;

    @Mock private AgentProfileRepository agentProfileRepository;
    @Mock private AgentDocumentRepository documentRepository;
    @Mock private UserRepository userRepository;
    @Mock private AgentProfileAssembler assembler;
    @Mock private AgentDocumentStorage documentStorage;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks private AgentAdminService service;

    private AgentProfile profile;

    @BeforeEach
    void setUp() {
        User agent = new User();
        agent.setId(7L);
        agent.setEmail("agent@example.com");

        profile = new AgentProfile();
        profile.setId(PROFILE_ID);
        profile.setUser(agent);
        when(agentProfileRepository.findWithUserById(PROFILE_ID)).thenReturn(Optional.of(profile));
    }

    @Test
    void approve_pendingProfile_turnsOnAcceptingRequests() {
        profile.setStatus(AgentStatus.PENDING_APPROVAL);
        profile.setRejectionReason("lý do cũ");
        when(userRepository.getReferenceById(ADMIN_ID)).thenReturn(new User());

        service.approveAgentProfile(PROFILE_ID, ADMIN_ID);

        assertThat(profile.getStatus()).isEqualTo(AgentStatus.APPROVED);
        assertThat(profile.isAcceptingRequests()).isTrue();
        assertThat(profile.getRejectionReason()).isNull();
        assertThat(profile.getReviewedAt()).isNotNull();
        verify(eventPublisher).publishEvent(any(NotificationEvents.UserEmailEvent.class));
    }

    @Test
    void approve_draftProfile_isRejected() {
        profile.setStatus(AgentStatus.DRAFT);

        assertThatThrownBy(() -> service.approveAgentProfile(PROFILE_ID, ADMIN_ID))
                .isInstanceOf(IllegalStateException.class);
        assertThat(profile.getStatus()).isEqualTo(AgentStatus.DRAFT);
    }

    @Test
    void reject_pendingProfile_movesToNeedsRevisionWithReason() {
        profile.setStatus(AgentStatus.PENDING_APPROVAL);
        when(userRepository.getReferenceById(ADMIN_ID)).thenReturn(new User());

        service.rejectAgentProfile(PROFILE_ID, ADMIN_ID, "  Ảnh CCCD bị mờ  ");

        assertThat(profile.getStatus()).isEqualTo(AgentStatus.NEEDS_REVISION);
        assertThat(profile.getRejectionReason()).isEqualTo("Ảnh CCCD bị mờ");
        assertThat(profile.isAcceptingRequests()).isFalse();
    }
}
