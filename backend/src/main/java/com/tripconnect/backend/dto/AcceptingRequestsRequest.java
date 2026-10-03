package com.tripconnect.backend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AcceptingRequestsRequest {

    @NotNull(message = "Vui lòng chọn bật hoặc tắt")
    private Boolean accepting;
}
