package com.tripconnect.backend.dto.tour;

import com.tripconnect.backend.enums.AccommodationType;
import com.tripconnect.backend.enums.TransportMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;
import java.util.List;
import java.util.Set;

/**
 * Toàn bộ nội dung tour (dùng cho cả tạo mới và sửa). Ảnh, file PDF, lịch khởi hành gửi bằng API riêng.
 * Các quy tắc liên quan nhiều trường (số đêm, số ngày lịch trình...) kiểm tra ở TourContentWriter.
 */
@Getter
@Setter
public class TourContentRequest {

    @NotBlank(message = "Tên tour không được để trống")
    @Size(min = 10, max = 150, message = "Tên tour từ 10 đến 150 ký tự")
    private String title;

    @NotEmpty(message = "Chọn ít nhất 1 loại hình tour")
    @Size(max = 3, message = "Chọn tối đa 3 loại hình tour")
    private List<@NotNull Long> categoryIds;

    @NotNull(message = "Vui lòng chọn nơi khởi hành")
    private Long departureLocationId;

    @NotEmpty(message = "Chọn ít nhất 1 điểm đến")
    @Size(max = 20, message = "Chọn tối đa 20 điểm đến")
    private List<@NotNull Long> destinationIds;

    @NotNull(message = "Vui lòng nhập số ngày")
    @Min(value = 1, message = "Tour ít nhất 1 ngày")
    @Max(value = 30, message = "Tour tối đa 30 ngày")
    private Integer durationDays;

    @NotNull(message = "Vui lòng nhập số đêm")
    @Min(value = 0, message = "Số đêm không hợp lệ")
    @Max(value = 30, message = "Số đêm không hợp lệ")
    private Integer durationNights;

    @NotNull(message = "Vui lòng nhập điểm nổi bật")
    @Size(min = 3, max = 6, message = "Điểm nổi bật gồm 3 đến 6 dòng")
    private List<@NotBlank(message = "Điểm nổi bật không được để trống")
    @Size(max = 150, message = "Mỗi điểm nổi bật tối đa 150 ký tự") String> highlights;

    /** Phần tử thứ i là lịch trình ngày thứ i+1; số phần tử phải bằng số ngày của tour. */
    @NotNull(message = "Vui lòng nhập lịch trình")
    @Size(min = 1, max = 30, message = "Lịch trình từ 1 đến 30 ngày")
    private List<@Valid @NotNull ItineraryDayRequest> itinerary;

    @NotEmpty(message = "Chọn ít nhất 1 phương tiện")
    private Set<@NotNull TransportMode> transportModes;

    @NotNull(message = "Vui lòng chọn tiêu chuẩn lưu trú")
    private AccommodationType accommodationType;

    @NotBlank(message = "Điểm đón không được để trống")
    @Size(max = 255, message = "Điểm đón tối đa 255 ký tự")
    private String meetingPoint;

    @NotNull(message = "Vui lòng nhập giờ tập trung")
    private LocalTime meetingTime;

    @NotEmpty(message = "Nhập ít nhất 1 dịch vụ bao gồm")
    @Size(max = 30, message = "Tối đa 30 dòng")
    private List<@NotBlank(message = "Dòng dịch vụ không được để trống")
    @Size(max = 255, message = "Mỗi dòng tối đa 255 ký tự") String> includedServices;

    @Size(max = 30, message = "Tối đa 30 dòng")
    private List<@NotBlank(message = "Dòng dịch vụ không được để trống")
    @Size(max = 255, message = "Mỗi dòng tối đa 255 ký tự") String> excludedServices;

    @Size(max = 2000, message = "Lưu ý tối đa 2000 ký tự")
    private String notes;

    @Getter
    @Setter
    public static class ItineraryDayRequest {

        @NotBlank(message = "Tiêu đề ngày không được để trống")
        @Size(max = 200, message = "Tiêu đề ngày tối đa 200 ký tự")
        private String title;

        @NotBlank(message = "Nội dung ngày không được để trống")
        @Size(max = 5000, message = "Nội dung mỗi ngày tối đa 5000 ký tự")
        private String description;

        private boolean breakfast;
        private boolean lunch;
        private boolean dinner;

        @Size(max = 255, message = "Nơi nghỉ tối đa 255 ký tự")
        private String accommodation;
    }
}
