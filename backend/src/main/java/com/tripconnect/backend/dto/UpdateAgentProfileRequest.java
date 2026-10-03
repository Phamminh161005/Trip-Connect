package com.tripconnect.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** Agent sửa thông tin pháp lý khi hồ sơ đang Nháp hoặc Cần bổ sung. */
@Getter
@Setter
public class UpdateAgentProfileRequest {

    @NotBlank(message = "Tên công ty không được để trống")
    @Size(max = 255, message = "Tên công ty tối đa 255 ký tự")
    private String companyName;

    @NotBlank(message = "Mã số thuế không được để trống")
    @Pattern(regexp = ValidationPatterns.TAX_CODE, message = ValidationPatterns.TAX_CODE_MESSAGE)
    private String taxCode;

    /** Số giấy phép lữ hành — chưa bắt buộc khi lưu nháp, nhưng bắt buộc khi nộp hồ sơ. */
    @Size(max = 255, message = "Số giấy phép lữ hành tối đa 255 ký tự")
    private String businessLicense;

    @NotNull(message = "Vui lòng chọn Tỉnh/Thành phố của trụ sở")
    private Long addressProvinceId;

    @NotBlank(message = "Địa chỉ trụ sở không được để trống")
    @Size(min = ValidationPatterns.ADDRESS_DETAIL_MIN_LENGTH, max = 500,
            message = "Địa chỉ chi tiết (số nhà, đường, phường/xã) từ 10 đến 500 ký tự")
    private String address;

    @NotBlank(message = ValidationPatterns.BANK_BIN_MESSAGE)
    @Pattern(regexp = ValidationPatterns.BANK_BIN, message = ValidationPatterns.BANK_BIN_MESSAGE)
    private String bankBin;

    @NotBlank(message = "Số tài khoản không được để trống")
    @Pattern(regexp = ValidationPatterns.BANK_ACCOUNT_NUMBER, message = ValidationPatterns.BANK_ACCOUNT_NUMBER_MESSAGE)
    private String bankAccountNumber;

    @NotBlank(message = "Tên chủ tài khoản không được để trống")
    @Size(max = 255, message = "Tên chủ tài khoản tối đa 255 ký tự")
    private String bankAccountHolder;
}
