package com.tripconnect.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Ngân hàng trong hệ thống chuyển khoản liên ngân hàng Việt Nam (Napas / VietQR).
 * Dữ liệu cố định, nạp bằng Flyway (V5) từ API công khai của VietQR.
 */
@Entity
@Table(name = "banks")
@Getter
@Setter
@NoArgsConstructor
public class Bank {

    /** Mã định danh ngân hàng 6 số, vd 970436 = Vietcombank. Kết hợp với số tài khoản để tạo mã QR chuyển khoản. */
    @Id
    @Column(length = 8)
    private String bin;

    @Column(nullable = false, length = 20)
    private String code;

    @Column(nullable = false, length = 100)
    private String shortName;

    @Column(nullable = false)
    private String name;

    @Column(length = 500)
    private String logoUrl;

    @Column(nullable = false)
    private boolean transferSupported = true;

    @Column(nullable = false)
    private boolean active = true;
}
