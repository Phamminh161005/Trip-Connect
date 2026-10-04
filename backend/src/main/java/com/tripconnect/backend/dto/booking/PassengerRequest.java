package com.tripconnect.backend.dto.booking;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/** Thông tin một hành khách (lúc đặt tour hoặc bổ sung sau). */
@Getter
@Setter
public class PassengerRequest {

    @NotBlank(message = "Họ tên hành khách không được để trống")
    @Size(max = 100, message = "Họ tên tối đa 100 ký tự")
    private String fullName;

    @NotNull(message = "Vui lòng nhập ngày sinh")
    @Past(message = "Ngày sinh không hợp lệ")
    private LocalDate dateOfBirth;

    /** Bắt buộc với tour quốc tế. */
    @Pattern(regexp = "^$|^[A-Za-z0-9]{6,20}$", message = "Số hộ chiếu gồm 6-20 chữ cái hoặc chữ số")
    private String passportNumber;
}
