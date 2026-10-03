package com.tripconnect.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateBusinessLicenseRequest {

    @NotBlank(message = "Số giấy phép lữ hành không được để trống")
    @Size(max = 255, message = "Số giấy phép lữ hành tối đa 255 ký tự")
    private String businessLicense;
}
