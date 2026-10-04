package com.tripconnect.backend.dto.search;

import com.tripconnect.backend.repository.StablePaging;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

/**
 * Tham số tìm tour (đọc từ query string, vd ?q=ha long&destinationId=14&dateFrom=2026-10-15&sort=PRICE_ASC).
 * Mọi tham số đều không bắt buộc.
 */
@Getter
@Setter
public class TourSearchRequest {

    public enum Sort {
        /** Đánh giá tốt + mới công khai lên trước. */
        RECOMMENDED,
        PRICE_ASC,
        PRICE_DESC,
        DEPARTURE_SOON,
        NEWEST,
        RATING
    }

    @Size(max = 200, message = "Từ khóa tối đa 200 ký tự")
    private String q;

    /** Tỉnh/Thành hoặc quốc gia (bảng locations). */
    private Long destinationId;

    private Long departureLocationId;

    /** Có lịch khởi hành trong khoảng ngày này. */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateTo;

    /** Có lịch mà giá người lớn nằm trong khoảng này (VNĐ). */
    @Min(value = 0, message = "Giá không hợp lệ")
    private Long priceMin;

    @Min(value = 0, message = "Giá không hợp lệ")
    private Long priceMax;

    @Min(value = 1, message = "Số ngày không hợp lệ")
    @Max(value = 30, message = "Số ngày không hợp lệ")
    private Integer durationMin;

    @Min(value = 1, message = "Số ngày không hợp lệ")
    @Max(value = 30, message = "Số ngày không hợp lệ")
    private Integer durationMax;

    /** Tour thuộc ÍT NHẤT MỘT trong các loại hình này. */
    @Size(max = 20, message = "Chọn tối đa 20 loại hình")
    private List<Long> categoryIds;

    /** true = quốc tế, false = trong nước, bỏ trống = cả hai. */
    private Boolean international;

    private Sort sort = Sort.RECOMMENDED;

    @Min(0)
    @Max(value = StablePaging.MAX_PAGE, message = "Số trang quá lớn, vui lòng thu hẹp bộ lọc tìm kiếm")
    private int page = 0;

    @Min(1)
    @Max(value = 50, message = "Tối đa 50 tour mỗi trang")
    private int size = 12;

    /** Ghi vào lịch sử tìm kiếm (trang kết quả gửi true; các khối tour ở trang chủ không gửi). */
    private boolean track;

    @AssertTrue(message = "Ngày bắt đầu phải trước ngày kết thúc")
    public boolean isDateRangeValid() {
        return dateFrom == null || dateTo == null || !dateFrom.isAfter(dateTo);
    }

    @AssertTrue(message = "Giá thấp nhất phải nhỏ hơn giá cao nhất")
    public boolean isPriceRangeValid() {
        return priceMin == null || priceMax == null || priceMin <= priceMax;
    }

    @AssertTrue(message = "Số ngày ít nhất phải nhỏ hơn số ngày nhiều nhất")
    public boolean isDurationRangeValid() {
        return durationMin == null || durationMax == null || durationMin <= durationMax;
    }

    /** Có lọc gì không (không lọc gì = xem tất cả, không cần ghi lịch sử). */
    public boolean hasCriteria() {
        return (q != null && !q.isBlank()) || destinationId != null || departureLocationId != null
                || dateFrom != null || dateTo != null || priceMin != null || priceMax != null
                || durationMin != null || durationMax != null
                || (categoryIds != null && !categoryIds.isEmpty()) || international != null;
    }
}
