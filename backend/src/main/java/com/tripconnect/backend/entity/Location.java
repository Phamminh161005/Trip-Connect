package com.tripconnect.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "locations")
@Getter
@Setter
@NoArgsConstructor
public class Location {

    /** Việt Nam có đủ 63 Tỉnh/Thành (province khác null); nước ngoài chỉ có dòng cấp quốc gia (province = null). */
    public static final String VIETNAM = "Việt Nam";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String country;

    private String province;

    public boolean isVietnam() {
        return VIETNAM.equals(country);
    }

    public boolean isVietnamProvince() {
        return isVietnam() && province != null;
    }
}