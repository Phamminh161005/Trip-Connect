package com.tripconnect.backend.dto.booking;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** Khách hủy đơn — lý do không bắt buộc. */
@Getter
@Setter
public class CancelBookingRequest {

    @Size(max = 500, message = "Lý do tối đa 500 ký tự")
    private String reason;
}
