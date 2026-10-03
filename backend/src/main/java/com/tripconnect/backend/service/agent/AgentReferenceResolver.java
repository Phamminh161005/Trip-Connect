package com.tripconnect.backend.service.agent;

import com.tripconnect.backend.entity.Bank;
import com.tripconnect.backend.entity.Location;
import com.tripconnect.backend.repository.BankRepository;
import com.tripconnect.backend.repository.LocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Kiểm tra + nạp các dữ liệu tham chiếu của hồ sơ Agent (Tỉnh/Thành trụ sở, ngân hàng). */
@Component
@RequiredArgsConstructor
public class AgentReferenceResolver {

    private final LocationRepository locationRepository;
    private final BankRepository bankRepository;

    /** Trụ sở phải là một Tỉnh/Thành của Việt Nam (không nhận địa điểm cấp quốc gia hay nước ngoài). */
    public Location requireVietnamProvince(Long locationId) {
        return locationRepository.findById(locationId)
                .filter(Location::isVietnamProvince)
                .orElseThrow(() -> new IllegalArgumentException("Tỉnh/Thành phố của trụ sở không hợp lệ"));
    }

    public Bank requireBank(String bin) {
        return bankRepository.findByBinAndActiveTrue(bin)
                .orElseThrow(() -> new IllegalArgumentException("Ngân hàng không hợp lệ, vui lòng chọn trong danh sách"));
    }
}
