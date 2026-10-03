package com.tripconnect.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.Locale;

@Getter
@Setter
public class LoginRequest {

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    @Size(max = 254, message = "Email quá dài")
    private String email;

    // Không áp @Pattern ở đây: tài khoản cũ có thể có mật khẩu yếu hơn quy tắc mới
    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(max = 72, message = "Mật khẩu tối đa 72 ký tự")
    private String password;

    public void setEmail(String email) {
        this.email = email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
