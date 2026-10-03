package com.tripconnect.backend.dto;

import com.tripconnect.backend.enums.UserRole;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Locale;

@Getter
@Setter
public class RegisterRequest {

    @NotBlank(message = "Họ tên không được để trống")
    @Size(max = 100, message = "Họ tên tối đa 100 ký tự")
    private String fullName;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    @Size(max = 254, message = "Email quá dài")
    private String email;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^\\+?\\d{9,15}$", message = "Số điện thoại không hợp lệ")
    private String phone;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(max = 72, message = "Mật khẩu tối đa 72 ký tự")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$",
            message = "Mật khẩu phải có ít nhất 8 ký tự, gồm chữ hoa, chữ thường và số"
    )
    private String password;

    @NotNull(message = "Vui lòng chọn loại tài khoản")
    private UserRole role;

    @Valid
    private AgentProfileRequest agentProfile;

    public void setEmail(String email) {
        this.email = email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    @Getter
    @Setter
    public static class AgentProfileRequest {

        @NotBlank(message = "Tên công ty không được để trống")
        @Size(max = 255, message = "Tên công ty tối đa 255 ký tự")
        private String companyName;

        @NotBlank(message = "Mã số thuế không được để trống")
        @Pattern(regexp = ValidationPatterns.TAX_CODE, message = ValidationPatterns.TAX_CODE_MESSAGE)
        private String taxCode;

        @Size(max = 255, message = "Số giấy phép kinh doanh tối đa 255 ký tự")
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

        @Size(max = 50, message = "Chọn tối đa 50 địa điểm")
        private List<Long> locationIds;

        @Size(max = 50, message = "Chọn tối đa 50 loại hình tour")
        private List<Long> categoryIds;
    }
}
