package com.tripconnect.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeactivateUserRequest {

    @NotBlank(message = "Lý do vô hiệu hóa không được để trống")
    @Size(max = 1000, message = "Lý do tối đa 1000 ký tự")
    private String reason;
}
