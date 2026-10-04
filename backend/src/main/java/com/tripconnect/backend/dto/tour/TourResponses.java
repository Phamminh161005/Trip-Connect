package com.tripconnect.backend.dto.tour;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.tripconnect.backend.dto.LocationResponse;
import com.tripconnect.backend.dto.TourCategoryResponse;
import com.tripconnect.backend.enums.AccommodationType;
import com.tripconnect.backend.enums.DepartureStatus;
import com.tripconnect.backend.enums.TourStatus;
import com.tripconnect.backend.enums.TransportMode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/** Các DTO trả về của module tour (gom một file vì chúng đi cùng nhau). */
public final class TourResponses {

    private TourResponses() {
    }

    /** Đơn vị tổ chức tour. null ở tour của TripConnect. */
    public record Provider(Long agentProfileId, String companyName, BigDecimal rating, int ratingCount) {
    }

    public record Image(Long id, String url, int sortOrder) {
    }

    public record ItineraryDay(int dayNumber, String title, String description,
                               boolean breakfast, boolean lunch, boolean dinner, String accommodation) {
    }

    public record ItineraryFile(String fileName, long sizeBytes) {
    }

    /**
     * @param departed đã tới ngày khởi hành (không sửa / bán được nữa)
     * @param bookable khách đặt được ngay lúc này
     */
    public record Departure(Long id, LocalDate startDate, LocalDate endDate, int capacity,
                            int seatsBooked, int seatsAvailable, long adultPrice, long childPrice,
                            DepartureStatus status, String cancelReason, boolean departed, boolean bookable) {
    }

    /** Chi tiết tour. Ở trang công khai: không có statusReason / missingItems, chỉ có lịch còn bán. */
    public record Detail(
            Long id,
            String title,
            TourStatus status,
            String statusReason,
            boolean platformTour,
            Provider provider,
            List<TourCategoryResponse> categories,
            LocationResponse departureLocation,
            List<LocationResponse> destinations,
            boolean international,
            int durationDays,
            int durationNights,
            List<String> highlights,
            List<ItineraryDay> itinerary,
            List<TransportMode> transportModes,
            AccommodationType accommodationType,
            String meetingPoint,
            @JsonFormat(pattern = "HH:mm") LocalTime meetingTime,
            List<String> includedServices,
            List<String> excludedServices,
            String notes,
            List<Image> images,
            ItineraryFile itineraryFile,
            List<Departure> departures,
            BigDecimal rating,
            int ratingCount,
            LocalDateTime submittedAt,
            LocalDateTime reviewedAt,
            LocalDateTime publishedAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            /* Việc còn thiếu trước khi gửi duyệt / công khai (rỗng = đủ điều kiện) */
            List<String> missingItems
    ) {
    }

    /** Một dòng trong danh sách tour (trang quản lý của Agent / Admin). */
    public record Summary(
            Long id,
            String title,
            TourStatus status,
            boolean platformTour,
            Provider provider,
            String coverImageUrl,
            String departureLocation,
            boolean international,
            int durationDays,
            int durationNights,
            long openDepartureCount,
            /* Giá người lớn thấp nhất trong các lịch đang mở bán; null = chưa có lịch nào */
            Long minAdultPrice,
            LocalDateTime submittedAt,
            LocalDateTime publishedAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }
}
