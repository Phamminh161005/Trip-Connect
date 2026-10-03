package com.tripconnect.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** Người dùng tự sửa thông tin cá nhân. Không cho đổi email ở đây (đổi email cần xác thực lại qua OTP). */
@Getter
@Setter
public class UpdateMyProfileRequest {

    @NotBlank(message = "Họ tên không được để trống")
    @Size(max = 100, message = "Họ tên tối đa 100 ký tự")
    private String fullName;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^\\+?\\d{9,15}$", message = "Số điện thoại không hợp lệ")
    private String phone;
}
