package com.tripconnect.backend.dto.booking;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** Admin xác nhận đã hoàn tiền thủ công (ghi chú: số tham chiếu chuyển khoản...). */
@Getter
@Setter
public class ManualRefundRequest {

    @Size(max = 500, message = "Ghi chú tối đa 500 ký tự")
    private String note;
}
