package com.tripconnect.backend.entity;

import com.tripconnect.backend.enums.PassengerType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/** Một hành khách trong đơn — công ty lữ hành cần họ tên + ngày sinh để mua bảo hiểm du lịch. */
@Entity
@Table(name = "booking_passengers")
@Getter
@Setter
@NoArgsConstructor
public class BookingPassenger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Column(nullable = false, length = 100)
    private String fullName;

    @Column(nullable = false)
    private LocalDate dateOfBirth;

    /** Tính theo tuổi vào ngày khởi hành. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private PassengerType type;

    /** Bắt buộc với tour quốc tế. */
    @Column(length = 20)
    private String passportNumber;
}
