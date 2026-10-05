package com.tripconnect.backend.dto.customrequest;

import com.tripconnect.backend.dto.tour.TourContentRequest.ItineraryDayRequest;
import com.tripconnect.backend.enums.AccommodationType;
import com.tripconnect.backend.enums.TransportMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

public final class CustomRequestRequests {

    private CustomRequestRequests() {
    }

    /** Khách gửi yêu cầu. Giới hạn chi tiết (ngày, số khách...) kiểm tra trong service theo CustomRequestRules. */
    @Getter
    @Setter
    public static class Create {

        @NotNull(message = "Vui lòng chọn nơi khởi hành")
        private Long departureLocationId;

        @NotEmpty(message = "Chọn ít nhất 1 điểm đến")
        @Size(max = 10, message = "Chọn tối đa 10 điểm đến")
        private List<@NotNull Long> destinationIds;

        @NotNull(message = "Vui lòng chọn ngày có thể khởi hành sớm nhất")
        private LocalDate earliestStart;

        @NotNull(message = "Vui lòng chọn ngày có thể khởi hành muộn nhất")
        private LocalDate latestStart;

        @NotNull(message = "Vui lòng nhập số ngày")
        @Min(value = 1, message = "Chuyến đi ít nhất 1 ngày")
        @Max(value = 30, message = "Chuyến đi tối đa 30 ngày")
        private Integer durationDays;

        @NotNull(message = "Vui lòng nhập số người lớn")
        @Min(value = 1, message = "Cần ít nhất 1 người lớn")
        private Integer adults;

        @NotNull(message = "Vui lòng nhập số trẻ em")
        @Min(value = 0, message = "Số trẻ em không hợp lệ")
        private Integer children;

        @NotNull(message = "Vui lòng nhập số trẻ sơ sinh")
        @Min(value = 0, message = "Số trẻ sơ sinh không hợp lệ")
        private Integer infants;

        @PositiveOrZero(message = "Ngân sách không hợp lệ")
        @Max(value = 1_000_000_000, message = "Ngân sách quá lớn")
        private Long budgetMin;

        @PositiveOrZero(message = "Ngân sách không hợp lệ")
        @Max(value = 1_000_000_000, message = "Ngân sách quá lớn")
        private Long budgetMax;

        @Size(max = 5, message = "Chọn tối đa 5 loại hình")
        private List<@NotNull Long> categoryIds;

        private Set<@NotNull TransportMode> transportModes;

        /** null = không yêu cầu */
        private AccommodationType accommodationType;

        @Size(max = 2000, message = "Ghi chú tối đa 2000 ký tự")
        private String notes;
    }

    @Getter
    @Setter
    public static class Assign {

        @NotNull(message = "Vui lòng chọn Agent")
        private Long agentId;
    }

    /** Lý do (bắt buộc với từ chối / đóng; khách hủy thì không bắt buộc). */
    @Getter
    @Setter
    public static class Reason {

        @Size(max = 1000, message = "Lý do tối đa 1000 ký tự")
        private String reason;
    }

    /**
     * Agent gửi đề xuất (bản đầu hoặc bản chỉnh sửa). Nơi khởi hành, điểm đến, số khách lấy theo yêu cầu.
     * Ngày khởi hành, số đêm, số ngày lịch trình, giá trẻ em kiểm tra trong CustomProposalService.
     */
    @Getter
    @Setter
    public static class Proposal {

        @NotBlank(message = "Tên chuyến đi không được để trống")
        @Size(min = 10, max = 150, message = "Tên chuyến đi từ 10 đến 150 ký tự")
        private String title;

        @NotNull(message = "Vui lòng chọn ngày khởi hành")
        private LocalDate startDate;

        @NotNull(message = "Vui lòng nhập số ngày")
        @Min(value = 1, message = "Chuyến đi ít nhất 1 ngày")
        @Max(value = 30, message = "Chuyến đi tối đa 30 ngày")
        private Integer durationDays;

        @NotNull(message = "Vui lòng nhập số đêm")
        @Min(value = 0, message = "Số đêm không hợp lệ")
        @Max(value = 30, message = "Số đêm không hợp lệ")
        private Integer durationNights;

        /** Phần tử thứ i là ngày thứ i+1; số phần tử phải bằng số ngày. */
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

        @NotNull(message = "Vui lòng nhập giá người lớn")
        @Min(value = 10_000, message = "Giá người lớn tối thiểu 10.000 VNĐ")
        @Max(value = 1_000_000_000, message = "Giá quá lớn")
        private Long adultPrice;

        @NotNull(message = "Vui lòng nhập giá trẻ em")
        @Min(value = 0, message = "Giá trẻ em không hợp lệ")
        @Max(value = 1_000_000_000, message = "Giá quá lớn")
        private Long childPrice;

        @Size(max = 2000, message = "Lời nhắn tối đa 2000 ký tự")
        private String message;
    }

    /** Khách đồng ý / yêu cầu chỉnh sửa: gửi kèm id đề xuất đang xem để không phản hồi nhầm bản cũ. */
    @Getter
    @Setter
    public static class ProposalResponse {

        @NotNull(message = "Thiếu đề xuất")
        private Long proposalId;

        @Size(max = 1000, message = "Nội dung tối đa 1000 ký tự")
        private String feedback;
    }
}
