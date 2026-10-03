package com.tripconnect.backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/** Khu vực phụ trách + loại hình tour thế mạnh — Agent tự sửa, có hiệu lực ngay. Gửi danh sách đầy đủ (thay thế toàn bộ). */
@Getter
@Setter
public class UpdateAgentExpertiseRequest {

    @NotNull(message = "Danh sách khu vực phụ trách không được để trống")
    @Size(max = 50, message = "Chọn tối đa 50 địa điểm")
    private List<Long> locationIds;

    @NotNull(message = "Danh sách loại hình tour không được để trống")
    @Size(max = 50, message = "Chọn tối đa 50 loại hình tour")
    private List<Long> categoryIds;
}
