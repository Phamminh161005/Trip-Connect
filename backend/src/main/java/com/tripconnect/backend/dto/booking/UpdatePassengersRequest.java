package com.tripconnect.backend.dto.booking;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/** Sửa danh sách hành khách của đơn (vẫn phải đủ số khách từng loại đã đặt). */
@Getter
@Setter
public class UpdatePassengersRequest {

    @NotNull(message = "Vui lòng gửi danh sách hành khách")
    @Size(max = 100, message = "Quá nhiều hành khách")
    private List<@Valid @NotNull PassengerRequest> passengers;
}
