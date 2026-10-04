package com.tripconnect.backend.dto.tour;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/** Thêm / sửa lịch khởi hành. Giá tính bằng VNĐ. */
@Getter
@Setter
public class DepartureRequest {

    @NotNull(message = "Vui lòng chọn ngày khởi hành")
    private LocalDate startDate;

    @NotNull(message = "Vui lòng nhập số chỗ")
    @Min(value = 1, message = "Số chỗ ít nhất là 1")
    @Max(value = 100, message = "Số chỗ tối đa là 100")
    private Integer capacity;

    @NotNull(message = "Vui lòng nhập giá người lớn")
    @Min(value = 10_000, message = "Giá người lớn tối thiểu 10.000đ")
    @Max(value = 1_000_000_000, message = "Giá quá lớn")
    private Long adultPrice;

    @NotNull(message = "Vui lòng nhập giá trẻ em")
    @Min(value = 0, message = "Giá trẻ em không hợp lệ")
    @Max(value = 1_000_000_000, message = "Giá quá lớn")
    private Long childPrice;
}
