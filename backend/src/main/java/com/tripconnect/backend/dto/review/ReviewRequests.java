package com.tripconnect.backend.dto.review;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

public final class ReviewRequests {

    private ReviewRequests() {
    }

    /** Viết / sửa đánh giá (ảnh gửi bằng API riêng). */
    @Getter
    @Setter
    public static class Write {

        @NotNull(message = "Vui lòng chọn số sao")
        @Min(value = 1, message = "Số sao từ 1 đến 5")
        @Max(value = 5, message = "Số sao từ 1 đến 5")
        private Integer rating;

        @NotBlank(message = "Vui lòng viết nhận xét")
        @Size(min = 20, max = 2000, message = "Nhận xét từ 20 đến 2000 ký tự")
        private String comment;
    }

    @Getter
    @Setter
    public static class Reply {

        @NotBlank(message = "Vui lòng nhập nội dung trả lời")
        @Size(max = 2000, message = "Trả lời tối đa 2000 ký tự")
        private String reply;
    }

    @Getter
    @Setter
    public static class Hide {

        @NotBlank(message = "Vui lòng nhập lý do ẩn")
        @Size(max = 1000, message = "Lý do tối đa 1000 ký tự")
        private String reason;
    }
}
