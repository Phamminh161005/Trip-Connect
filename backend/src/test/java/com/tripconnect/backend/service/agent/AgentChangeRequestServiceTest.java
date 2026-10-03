package com.tripconnect.backend.service.agent;

import com.tripconnect.backend.dto.AgentChangeRequestForm;
import com.tripconnect.backend.entity.AgentDocument;
import com.tripconnect.backend.entity.AgentProfile;
import com.tripconnect.backend.entity.AgentProfileChangeRequest;
import com.tripconnect.backend.entity.Bank;
import com.tripconnect.backend.entity.User;
import com.tripconnect.backend.enums.AgentDocumentStatus;
import com.tripconnect.backend.enums.AgentDocumentType;
import com.tripconnect.backend.enums.AgentStatus;
import com.tripconnect.backend.enums.ChangeRequestStatus;
import com.tripconnect.backend.repository.AgentDocumentRepository;
import com.tripconnect.backend.repository.AgentProfileChangeRequestRepository;
import com.tripconnect.backend.repository.AgentProfileRepository;
import com.tripconnect.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgentChangeRequestServiceTest {

    private static final long USER_ID = 7L;
    private static final long PROFILE_ID = 3L;

    @Mock private AgentProfileRepository profileRepository;
    @Mock private AgentProfileChangeRequestRepository changeRequestRepository;
    @Mock private AgentDocumentRepository documentRepository;
    @Mock private UserRepository userRepository;
    @Mock private AgentDocumentStorage documentStorage;
    @Mock private AgentProfileAssembler assembler;
    @Mock private AgentReferenceResolver referenceResolver;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks private AgentChangeRequestService service;

    private AgentProfile profile;
    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(USER_ID);
        user.setEmail("agent@example.com");
        user.setPasswordHash("hashed");

        profile = new AgentProfile();
        profile.setId(PROFILE_ID);
        profile.setUser(user);
        profile.setStatus(AgentStatus.APPROVED);
        profile.setCompanyName("Công ty A");
        profile.setBank(bank("970436"));
        profile.setBankAccountNumber("0011001234567");
        profile.setBankAccountHolder("CONG TY A");
    }

    @Test
    void create_whenProfileNotApprovedYet_isRejected() {
        profile.setStatus(AgentStatus.NEEDS_REVISION);
        givenProfile();

        assertThatThrownBy(() -> service.create(USER_ID, formWithCompanyName("Công ty B")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("đã được duyệt");
    }

    @Test
    void create_whenAnotherRequestIsPending_isRejected() {
        givenProfile();
        when(changeRequestRepository.existsByAgentProfileIdAndStatus(PROFILE_ID, ChangeRequestStatus.PENDING))
                .thenReturn(true);

        assertThatThrownBy(() -> service.create(USER_ID, formWithCompanyName("Công ty B")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("chờ duyệt");
    }

    @Test
    void create_whenNothingActuallyChanges_isRejected() {
        givenProfile();
        // Gửi lại đúng tên công ty hiện tại (kèm khoảng trắng) -> coi như không đổi
        assertThatThrownBy(() -> service.create(USER_ID, formWithCompanyName("  Công ty A  ")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("không có thay đổi");
        verify(changeRequestRepository, never()).save(any());
    }

    @Test
    void create_whenChangingBankAccountWithWrongPassword_isRejected() {
        givenProfile();
        AgentChangeRequestForm form = bankForm("970407", "19031234567890", "Công ty A");
        form.setCurrentPassword("sai-mat-khau");
        when(referenceResolver.requireBank("970407")).thenReturn(bank("970407"));
        when(passwordEncoder.matches("sai-mat-khau", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> service.create(USER_ID, form))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Mật khẩu hiện tại không đúng");
        verify(changeRequestRepository, never()).save(any());
    }

    @Test
    void create_whenChangingBankAccountWithoutPassword_isRejected() {
        givenProfile();
        when(referenceResolver.requireBank("970407")).thenReturn(bank("970407"));

        assertThatThrownBy(() -> service.create(USER_ID, bankForm("970407", "19031234567890", "Công ty A")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("mật khẩu hiện tại");
    }

    @Test
    void create_whenBankAccountFieldsIncomplete_isRejected() {
        givenProfile();
        // Chỉ nhập số tài khoản, thiếu ngân hàng + tên chủ tài khoản
        assertThatThrownBy(() -> service.create(USER_ID, bankForm(null, "19031234567890", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nhập đủ");
    }

    @Test
    void create_whenSameBankAccountTypedWithDiacritics_isTreatedAsNoChange() {
        givenProfile();
        // Tên gõ có dấu + chữ thường nhưng sau chuẩn hóa trùng tên hiện tại -> không đổi -> không đòi mật khẩu
        assertThatThrownBy(() -> service.create(USER_ID, bankForm("970436", "0011001234567", "công ty a")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("không có thay đổi");
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void approve_appliesChangedFieldsAndArchivesReplacedDocument() {
        AgentProfileChangeRequest request = new AgentProfileChangeRequest();
        request.setId(11L);
        request.setAgentProfile(profile);
        request.setStatus(ChangeRequestStatus.PENDING);
        request.setCompanyName("Công ty B"); // đổi tên
        // bank = null -> giữ nguyên tài khoản ngân hàng

        AgentDocument newLicense = document(AgentDocumentType.TRAVEL_LICENSE, AgentDocumentStatus.PENDING);
        AgentDocument oldLicense = document(AgentDocumentType.TRAVEL_LICENSE, AgentDocumentStatus.ACTIVE);

        when(changeRequestRepository.findWithProfileById(11L)).thenReturn(Optional.of(request));
        when(documentRepository.findByChangeRequestIdOrderByUploadedAtAsc(11L)).thenReturn(List.of(newLicense));
        when(documentRepository.findByAgentProfileIdAndTypeAndStatus(
                PROFILE_ID, AgentDocumentType.TRAVEL_LICENSE, AgentDocumentStatus.ACTIVE)).thenReturn(List.of(oldLicense));

        service.approve(11L, 1L);

        assertThat(profile.getCompanyName()).isEqualTo("Công ty B");
        assertThat(profile.getBank().getBin()).isEqualTo("970436");
        assertThat(profile.getBankAccountNumber()).isEqualTo("0011001234567");
        assertThat(newLicense.getStatus()).isEqualTo(AgentDocumentStatus.ACTIVE);
        assertThat(oldLicense.getStatus()).isEqualTo(AgentDocumentStatus.ARCHIVED);
        assertThat(request.getStatus()).isEqualTo(ChangeRequestStatus.APPROVED);
        assertThat(request.getReviewedAt()).isNotNull();
    }

    @Test
    void approve_whenAlreadyProcessed_isRejected() {
        AgentProfileChangeRequest request = new AgentProfileChangeRequest();
        request.setAgentProfile(profile);
        request.setStatus(ChangeRequestStatus.REJECTED);
        when(changeRequestRepository.findWithProfileById(11L)).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> service.approve(11L, 1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("đã được xử lý");
    }

    @Test
    void changedValue_treatsBlankAndUnchangedAsNoChange() {
        assertThat(AgentChangeRequestService.changedValue(null, "A")).isNull();
        assertThat(AgentChangeRequestService.changedValue("   ", "A")).isNull();
        assertThat(AgentChangeRequestService.changedValue(" A ", "A")).isNull();
        assertThat(AgentChangeRequestService.changedValue(" B ", "A")).isEqualTo("B");
    }

    private void givenProfile() {
        when(profileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(profile));
    }

    private static AgentChangeRequestForm formWithCompanyName(String companyName) {
        AgentChangeRequestForm form = new AgentChangeRequestForm();
        form.setCompanyName(companyName);
        return form;
    }

    private static AgentChangeRequestForm bankForm(String bin, String accountNumber, String holder) {
        AgentChangeRequestForm form = new AgentChangeRequestForm();
        form.setBankBin(bin);
        form.setBankAccountNumber(accountNumber);
        form.setBankAccountHolder(holder);
        return form;
    }

    private static Bank bank(String bin) {
        Bank bank = new Bank();
        bank.setBin(bin);
        return bank;
    }

    private static AgentDocument document(AgentDocumentType type, AgentDocumentStatus status) {
        AgentDocument document = new AgentDocument();
        document.setType(type);
        document.setStatus(status);
        return document;
    }
}
