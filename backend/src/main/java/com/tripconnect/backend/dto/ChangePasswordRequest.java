package com.tripconnect.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChangePasswordRequest {

    @NotBlank(message = "Mật khẩu cũ không được để trống")
    @Size(max = 72, message = "Mật khẩu tối đa 72 ký tự")
    private String oldPassword;

    @NotBlank(message = "Mật khẩu mới không được để trống")
    @Size(max = 72, message = "Mật khẩu tối đa 72 ký tự")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$",
            message = "Mật khẩu phải có ít nhất 8 ký tự, gồm chữ hoa, chữ thường và số"
    )
    private String newPassword;
}
