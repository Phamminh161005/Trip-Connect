package com.tripconnect.backend.controller;

import com.tripconnect.backend.dto.BankResponse;
import com.tripconnect.backend.repository.BankRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.List;

/** Danh sách ngân hàng cho ô chọn ngân hàng (công khai — form đăng ký Agent dùng khi chưa đăng nhập). */
@RestController
@RequestMapping("/api/banks")
@RequiredArgsConstructor
public class BankController {

    private final BankRepository bankRepository;

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<List<BankResponse>> getBanks() {
        List<BankResponse> banks = bankRepository.findByActiveTrueOrderByShortNameAsc().stream()
                .map(BankResponse::from)
                .toList();
        // Danh sách gần như không đổi -> cho trình duyệt lưu tạm 1 ngày
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(Duration.ofDays(1)).cachePublic()).body(banks);
    }
}
