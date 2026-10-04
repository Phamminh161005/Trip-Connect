package com.tripconnect.backend.dto.booking;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Đặt tour: người đại diện + số khách theo độ tuổi (giá tính theo số này) + danh sách đủ từng hành khách,
 * khớp số khách từng loại (loại tính từ ngày sinh so với ngày khởi hành).
 */
@Getter
@Setter
public class CreateBookingRequest {

    @NotNull(message = "Vui lòng chọn lịch khởi hành")
    private Long departureId;

    @NotBlank(message = "Họ tên người liên hệ không được để trống")
    @Size(max = 100, message = "Họ tên tối đa 100 ký tự")
    private String contactName;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^\\+?\\d{9,15}$", message = "Số điện thoại không hợp lệ")
    private String contactPhone;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    @Size(max = 254, message = "Email quá dài")
    private String contactEmail;

    @Size(max = 1000, message = "Ghi chú tối đa 1000 ký tự")
    private String note;

    // Giới hạn tổng số khách (30) kiểm tra trong service theo cấu hình
    @NotNull(message = "Vui lòng nhập số người lớn")
    @Min(value = 1, message = "Cần ít nhất 1 người lớn")
    @Max(value = 100, message = "Số người lớn không hợp lệ")
    private Integer adults;

    @NotNull(message = "Vui lòng nhập số trẻ em")
    @Min(value = 0, message = "Số trẻ em không hợp lệ")
    @Max(value = 100, message = "Số trẻ em không hợp lệ")
    private Integer children;

    @NotNull(message = "Vui lòng nhập số trẻ sơ sinh")
    @Min(value = 0, message = "Số trẻ sơ sinh không hợp lệ")
    @Max(value = 100, message = "Số trẻ sơ sinh không hợp lệ")
    private Integer infants;

    @NotEmpty(message = "Vui lòng nhập thông tin hành khách")
    @Size(max = 100, message = "Quá nhiều hành khách")
    private List<@Valid @NotNull PassengerRequest> passengers;
}
