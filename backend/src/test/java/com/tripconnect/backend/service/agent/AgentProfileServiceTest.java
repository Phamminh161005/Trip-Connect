package com.tripconnect.backend.service.agent;

import com.tripconnect.backend.dto.UpdateAgentProfileRequest;
import com.tripconnect.backend.entity.AgentProfile;
import com.tripconnect.backend.entity.User;
import com.tripconnect.backend.enums.AgentDocumentType;
import com.tripconnect.backend.enums.AgentStatus;
import com.tripconnect.backend.exception.ForbiddenException;
import com.tripconnect.backend.repository.AgentDocumentRepository;
import com.tripconnect.backend.repository.AgentProfileRepository;
import com.tripconnect.backend.service.NotificationEvents;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgentProfileServiceTest {

    private static final long USER_ID = 7L;
    private static final long PROFILE_ID = 3L;

    @Mock private AgentProfileRepository profileRepository;
    @Mock private AgentDocumentRepository documentRepository;
    @Mock private AgentProfileCompleteness completeness;
    @Mock private AgentDocumentStorage documentStorage;
    @Mock private AgentExpertiseWriter expertiseWriter;
    @Mock private AgentProfileAssembler assembler;
    @Mock private AgentReferenceResolver referenceResolver;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks private AgentProfileService service;

    private AgentProfile profile;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setId(USER_ID);
        user.setEmail("agent@example.com");

        profile = new AgentProfile();
        profile.setId(PROFILE_ID);
        profile.setUser(user);
        profile.setStatus(AgentStatus.DRAFT);
        profile.setCompanyName("Công ty Du lịch A");

        when(profileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(profile));
    }

    @Test
    void submit_whenItemsMissing_isRejectedWithTheList() {
        when(completeness.findMissingItems(profile))
                .thenReturn(List.of("số giấy phép lữ hành", "CCCD người đại diện (mặt trước)"));

        assertThatThrownBy(() -> service.submit(USER_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("số giấy phép lữ hành")
                .hasMessageContaining("CCCD người đại diện (mặt trước)");
        assertThat(profile.getStatus()).isEqualTo(AgentStatus.DRAFT);
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void submit_whenComplete_movesToPendingApprovalAndNotifiesAdmins() {
        when(completeness.findMissingItems(profile)).thenReturn(List.of());

        service.submit(USER_ID);

        assertThat(profile.getStatus()).isEqualTo(AgentStatus.PENDING_APPROVAL);
        assertThat(profile.getSubmittedAt()).isNotNull();
        verify(eventPublisher).publishEvent(any(NotificationEvents.AdminEmailEvent.class));
    }

    @Test
    void submit_whenAlreadyPending_isRejected() {
        profile.setStatus(AgentStatus.PENDING_APPROVAL);

        assertThatThrownBy(() -> service.submit(USER_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("đang chờ duyệt");
    }

    @Test
    void updateMyProfile_whenApproved_mustUseChangeRequestInstead() {
        profile.setStatus(AgentStatus.APPROVED);

        assertThatThrownBy(() -> service.updateMyProfile(USER_ID, new UpdateAgentProfileRequest()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("yêu cầu cập nhật");
    }

    @Test
    void updateBusinessLicense_whenDraft_savesTrimmedNumber() {
        service.updateBusinessLicense(USER_ID, "  01-123/2024/TCDL-GP LHQT  ");

        assertThat(profile.getBusinessLicense()).isEqualTo("01-123/2024/TCDL-GP LHQT");
    }

    @Test
    void updateBusinessLicense_whenPendingApproval_isRejected() {
        profile.setStatus(AgentStatus.PENDING_APPROVAL);

        assertThatThrownBy(() -> service.updateBusinessLicense(USER_ID, "01-123/2024/TCDL-GP LHQT"))
                .isInstanceOf(IllegalStateException.class);
        assertThat(profile.getBusinessLicense()).isNull();
    }

    @Test
    void uploadDocument_whenPendingApproval_isRejectedBeforeTouchingStorage() {
        profile.setStatus(AgentStatus.PENDING_APPROVAL);
        var file = new MockMultipartFile("file", "a.pdf", "application/pdf", new byte[]{1});

        assertThatThrownBy(() -> service.uploadDocument(USER_ID, AgentDocumentType.TRAVEL_LICENSE, file))
                .isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(documentStorage);
    }

    @Test
    void setAcceptingRequests_whenNotApproved_isForbidden() {
        profile.setStatus(AgentStatus.NEEDS_REVISION);

        assertThatThrownBy(() -> service.setAcceptingRequests(USER_ID, true))
                .isInstanceOf(ForbiddenException.class);
        assertThat(profile.isAcceptingRequests()).isFalse();
    }
}
