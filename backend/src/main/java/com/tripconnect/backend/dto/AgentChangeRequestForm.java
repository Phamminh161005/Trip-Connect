package com.tripconnect.backend.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Form yêu cầu cập nhật hồ sơ (multipart).
 * Trường nào để trống = không thay đổi trường đó. Phải có ít nhất 1 thay đổi.
 */
@Getter
@Setter
public class AgentChangeRequestForm {

    @Size(max = 255, message = "Tên công ty tối đa 255 ký tự")
    private String companyName;

    @Pattern(regexp = ValidationPatterns.TAX_CODE_OR_EMPTY, message = ValidationPatterns.TAX_CODE_MESSAGE)
    private String taxCode;

    @Size(max = 255, message = "Số giấy phép kinh doanh tối đa 255 ký tự")
    private String businessLicense;

    /** Tỉnh/Thành mới của trụ sở. */
    private Long addressProvinceId;

    @Size(max = 500, message = "Địa chỉ tối đa 500 ký tự")
    private String address;

    // ----- Đổi tài khoản ngân hàng: nhập ĐỦ cả 3 trường (hoặc để trống cả 3 nếu không đổi) -----

    @Pattern(regexp = ValidationPatterns.BANK_BIN_OR_EMPTY, message = ValidationPatterns.BANK_BIN_MESSAGE)
    private String bankBin;

    @Pattern(regexp = ValidationPatterns.BANK_ACCOUNT_NUMBER_OR_EMPTY, message = ValidationPatterns.BANK_ACCOUNT_NUMBER_MESSAGE)
    private String bankAccountNumber;

    @Size(max = 255, message = "Tên chủ tài khoản tối đa 255 ký tự")
    private String bankAccountHolder;

    /** Ghi chú cho quản trị viên (lý do thay đổi) — không bắt buộc. */
    @Size(max = 1000, message = "Ghi chú tối đa 1000 ký tự")
    private String note;

    /** Bắt buộc khi đổi tài khoản ngân hàng (chống kẻ chiếm phiên đăng nhập đổi nơi nhận tiền). */
    @Size(max = 72, message = "Mật khẩu tối đa 72 ký tự")
    private String currentPassword;

    // ----- Giấy tờ thay thế (không bắt buộc) -----
    private MultipartFile travelLicense;
    private MultipartFile businessRegistration;
    private MultipartFile representativeIdFront;
    private MultipartFile representativeIdBack;

    @Size(max = 5, message = "Tối đa 5 giấy tờ khác")
    private List<MultipartFile> otherDocuments;
}
