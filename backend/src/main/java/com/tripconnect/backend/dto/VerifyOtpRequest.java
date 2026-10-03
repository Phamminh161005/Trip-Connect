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
public class VerifyOtpRequest {

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    @Size(max = 254, message = "Email quá dài")
    private String email;

    @NotBlank(message = "Mã OTP không được để trống")
    @Pattern(regexp = "\\d{6}", message = "Mã OTP gồm 6 chữ số")
    private String otp;

    public void setEmail(String email) {
        this.email = email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
