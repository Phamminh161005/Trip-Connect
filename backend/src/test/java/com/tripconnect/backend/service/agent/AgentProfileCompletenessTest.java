package com.tripconnect.backend.service.agent;

import com.tripconnect.backend.entity.*;
import com.tripconnect.backend.enums.AgentDocumentStatus;
import com.tripconnect.backend.enums.AgentDocumentType;
import com.tripconnect.backend.repository.AgentDocumentRepository;
import com.tripconnect.backend.repository.AgentServiceAreaRepository;
import com.tripconnect.backend.repository.AgentSpecialtyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgentProfileCompletenessTest {

    private static final long USER_ID = 7L;
    private static final long PROFILE_ID = 3L;

    @Mock private AgentDocumentRepository documentRepository;
    @Mock private AgentServiceAreaRepository serviceAreaRepository;
    @Mock private AgentSpecialtyRepository specialtyRepository;

    @InjectMocks private AgentProfileCompleteness completeness;

    private AgentProfile profile;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setId(USER_ID);

        profile = new AgentProfile();
        profile.setId(PROFILE_ID);
        profile.setUser(user);
        profile.setCompanyName("Công ty Du lịch A");
        profile.setTaxCode("0101234567");
        profile.setBusinessLicense("01-123/2024/TCDL-GP LHQT");
        profile.setAddressProvince(new Location());
        profile.setAddress("1 Tràng Tiền, Hoàn Kiếm");
        profile.setBank(new Bank());
        profile.setBankAccountNumber("0011001234567");
        profile.setBankAccountHolder("CONG TY DU LICH A");

        when(serviceAreaRepository.findByAgentId(USER_ID)).thenReturn(List.of(new AgentServiceArea()));
        when(specialtyRepository.findByAgentId(USER_ID)).thenReturn(List.of(new AgentSpecialty()));
    }

    @Test
    void completeProfile_hasNothingMissing() {
        givenActiveDocuments(AgentDocumentType.TRAVEL_LICENSE, AgentDocumentType.BUSINESS_REGISTRATION,
                AgentDocumentType.REPRESENTATIVE_ID_FRONT, AgentDocumentType.REPRESENTATIVE_ID_BACK);

        assertThat(completeness.findMissingItems(profile)).isEmpty();
    }

    @Test
    void missingRequiredDocuments_areListedByLabel() {
        givenActiveDocuments(AgentDocumentType.TRAVEL_LICENSE, AgentDocumentType.OTHER);

        assertThat(completeness.findMissingItems(profile)).containsExactly(
                "Giấy chứng nhận đăng ký kinh doanh",
                "CCCD người đại diện (mặt trước)",
                "CCCD người đại diện (mặt sau)");
    }

    @Test
    void missingTravelLicenseNumberAndBankAccount_areListed() {
        profile.setBusinessLicense(null); // đăng ký xong chưa nhập số giấy phép
        profile.setBank(null);            // dữ liệu ngân hàng cũ bị xóa ở V5
        givenActiveDocuments(AgentDocumentType.TRAVEL_LICENSE, AgentDocumentType.BUSINESS_REGISTRATION,
                AgentDocumentType.REPRESENTATIVE_ID_FRONT, AgentDocumentType.REPRESENTATIVE_ID_BACK);

        assertThat(completeness.findMissingItems(profile))
                .containsExactly("số giấy phép lữ hành", "tài khoản ngân hàng");
    }

    private void givenActiveDocuments(AgentDocumentType... types) {
        List<AgentDocument> documents = Arrays.stream(types).map(type -> {
            AgentDocument document = new AgentDocument();
            document.setType(type);
            document.setStatus(AgentDocumentStatus.ACTIVE);
            return document;
        }).toList();
        when(documentRepository.findByAgentProfileIdAndStatusOrderByUploadedAtAsc(PROFILE_ID, AgentDocumentStatus.ACTIVE))
                .thenReturn(documents);
    }
}
