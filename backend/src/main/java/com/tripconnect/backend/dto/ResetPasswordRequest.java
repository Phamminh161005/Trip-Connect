package com.tripconnect.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.Locale;

@Getter
@Setter
public class ResetPasswordRequest {

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    @Size(max = 254, message = "Email quá dài")
    private String email;

    @NotBlank(message = "Mã OTP không được để trống")
    @Pattern(regexp = "\\d{6}", message = "Mã OTP gồm 6 chữ số")
    private String otp;

    @NotBlank(message = "Mật khẩu mới không được để trống")
    @Size(max = 72, message = "Mật khẩu tối đa 72 ký tự")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$",
            message = "Mật khẩu phải có ít nhất 8 ký tự, gồm chữ hoa, chữ thường và số"
    )
    private String newPassword;

    public void setEmail(String email) {
        this.email = email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
