package com.tripconnect.backend.dto.tour;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/** Thứ tự ảnh mới — phải gồm đúng tất cả ảnh của tour; ảnh đầu tiên là ảnh bìa. */
@Getter
@Setter
public class ReorderImagesRequest {

    @NotEmpty(message = "Danh sách ảnh không được để trống")
    private List<@NotNull Long> imageIds;
}
