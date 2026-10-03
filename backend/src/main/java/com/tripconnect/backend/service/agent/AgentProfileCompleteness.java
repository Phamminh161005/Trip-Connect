package com.tripconnect.backend.service.agent;

import com.tripconnect.backend.entity.AgentDocument;
import com.tripconnect.backend.entity.AgentProfile;
import com.tripconnect.backend.enums.AgentDocumentStatus;
import com.tripconnect.backend.enums.AgentDocumentType;
import com.tripconnect.backend.repository.AgentDocumentRepository;
import com.tripconnect.backend.repository.AgentServiceAreaRepository;
import com.tripconnect.backend.repository.AgentSpecialtyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Luật "hồ sơ đã đủ để nộp chưa" — MỘT nơi duy nhất.
 * Dùng khi Agent bấm Nộp hồ sơ, và trả về cho Frontend để hiển thị danh sách mục còn thiếu
 * (Frontend không tự tính lại -> không bao giờ lệch nhau).
 */
@Component
@RequiredArgsConstructor
public class AgentProfileCompleteness {

    private final AgentDocumentRepository documentRepository;
    private final AgentServiceAreaRepository serviceAreaRepository;
    private final AgentSpecialtyRepository specialtyRepository;

    /** Danh sách mục còn thiếu (rỗng = đủ điều kiện nộp). */
    public List<String> findMissingItems(AgentProfile profile) {
        List<String> missing = new ArrayList<>();
        if (isBlank(profile.getCompanyName())) missing.add("tên công ty");
        if (isBlank(profile.getTaxCode())) missing.add("mã số thuế");
        // Không hỏi lúc đăng ký; Agent nhập ở trang Hồ sơ kinh doanh, cạnh ô tải file giấy phép
        if (isBlank(profile.getBusinessLicense())) missing.add("số giấy phép lữ hành");
        if (profile.getAddressProvince() == null || isBlank(profile.getAddress())) missing.add("địa chỉ trụ sở");
        if (profile.getBank() == null || isBlank(profile.getBankAccountNumber()) || isBlank(profile.getBankAccountHolder())) {
            missing.add("tài khoản ngân hàng");
        }

        Long agentId = profile.getUser().getId();
        if (serviceAreaRepository.findByAgentId(agentId).isEmpty()) missing.add("khu vực phụ trách");
        if (specialtyRepository.findByAgentId(agentId).isEmpty()) missing.add("loại hình tour thế mạnh");

        Set<AgentDocumentType> uploadedTypes = documentRepository
                .findByAgentProfileIdAndStatusOrderByUploadedAtAsc(profile.getId(), AgentDocumentStatus.ACTIVE)
                .stream().map(AgentDocument::getType)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(AgentDocumentType.class)));
        for (AgentDocumentType type : AgentDocumentType.values()) {
            if (type.required() && !uploadedTypes.contains(type)) {
                missing.add(type.label());
            }
        }
        return missing;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
