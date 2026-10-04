package com.tripconnect.backend.dto.tour;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** Lý do bắt buộc: Admin yêu cầu sửa / đình chỉ tour, Agent hủy lịch khởi hành. */
@Getter
@Setter
public class ReasonRequest {

    @NotBlank(message = "Vui lòng nhập lý do")
    @Size(max = 1000, message = "Lý do tối đa 1000 ký tự")
    private String reason;
}
