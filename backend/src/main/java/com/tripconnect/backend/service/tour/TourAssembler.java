package com.tripconnect.backend.service.tour;

import com.tripconnect.backend.dto.LocationResponse;
import com.tripconnect.backend.dto.TourCategoryResponse;
import com.tripconnect.backend.dto.tour.TourResponses;
import com.tripconnect.backend.entity.*;
import com.tripconnect.backend.enums.DepartureStatus;
import com.tripconnect.backend.enums.TourStatus;
import com.tripconnect.backend.repository.AgentProfileRepository;
import com.tripconnect.backend.repository.TourDepartureRepository;
import com.tripconnect.backend.repository.TourImageRepository;
import com.tripconnect.backend.storage.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Chuyển entity tour thành DTO. Danh sách tour gom dữ liệu phụ (ảnh bìa, giá, Agent) bằng vài câu query
 * cho cả trang, không query từng dòng.
 * Phải được gọi trong transaction (có truy cập quan hệ LAZY).
 */
@Component
@RequiredArgsConstructor
public class TourAssembler {

    private final TourImageRepository imageRepository;
    private final TourDepartureRepository departureRepository;
    private final AgentProfileRepository agentProfileRepository;
    private final FileStorageService fileStorageService;
    private final TourBookingStats bookingStats;
    private final Clock clock;

    /** Chi tiết cho Agent / Admin quản lý tour: đủ mọi lịch khởi hành + việc còn thiếu. */
    public TourResponses.Detail toDetail(Tour tour) {
        List<TourImage> images = imageRepository.findByTourIdOrderBySortOrderAscIdAsc(tour.getId());
        List<TourDeparture> departures = departureRepository.findByTourIdOrderByStartDateAsc(tour.getId());
        return buildDetail(tour, images, departures, missingItems(tour, images.size()), true);
    }

    /** Chi tiết cho khách: chỉ lịch chưa khởi hành và chưa hủy, không có thông tin nội bộ. */
    public TourResponses.Detail toPublicDetail(Tour tour) {
        List<TourImage> images = imageRepository.findByTourIdOrderBySortOrderAscIdAsc(tour.getId());
        LocalDate today = LocalDate.now(clock);
        List<TourDeparture> departures = departureRepository.findByTourIdOrderByStartDateAsc(tour.getId()).stream()
                .filter(d -> d.getStatus() != DepartureStatus.CANCELLED && d.getStartDate().isAfter(today))
                .toList();
        return buildDetail(tour, images, departures, List.of(), false);
    }

    public TourResponses.Image toImage(TourImage image) {
        return new TourResponses.Image(image.getId(),
                fileStorageService.publicUrl(image.getPublicId(), image.getFormat()), image.getSortOrder());
    }

    public TourResponses.Departure toDeparture(TourDeparture departure, int durationDays, int seatsBooked) {
        LocalDate today = LocalDate.now(clock);
        boolean departed = !departure.getStartDate().isAfter(today);
        int available = Math.max(departure.getCapacity() - seatsBooked, 0);
        boolean bookable = !departed && departure.getStatus() == DepartureStatus.OPEN && available > 0;
        return new TourResponses.Departure(
                departure.getId(), departure.getStartDate(), departure.endDate(durationDays),
                departure.getCapacity(), seatsBooked, available, departure.getAdultPrice(), departure.getChildPrice(),
                departure.getStatus(), departure.getCancelReason(), departed, bookable);
    }

    /** Việc còn thiếu trước khi gửi duyệt / công khai. */
    public List<String> missingItems(Tour tour, long imageCount) {
        List<String> missing = new ArrayList<>();
        if (imageCount < TourRules.MIN_IMAGES) {
            missing.add("Cần ít nhất " + TourRules.MIN_IMAGES + " ảnh tour (hiện có " + imageCount + ")");
        }
        if (tour.getItineraryDays().size() != tour.getDurationDays()) {
            missing.add("Lịch trình phải có đúng " + tour.getDurationDays() + " ngày");
        }
        return missing;
    }

    public List<TourResponses.Summary> toSummaries(Page<Tour> page) {
        List<Tour> tours = page.getContent();
        if (tours.isEmpty()) return List.of();
        List<Long> tourIds = tours.stream().map(Tour::getId).toList();

        Map<Long, TourImage> covers = imageRepository.findCoverImages(tourIds).stream()
                .collect(Collectors.toMap(i -> i.getTour().getId(), Function.identity(),
                        (a, b) -> a.getId() < b.getId() ? a : b));
        Map<Long, TourDepartureRepository.OpenDepartureStats> stats = departureRepository
                .findOpenStats(tourIds, DepartureStatus.OPEN, LocalDate.now(clock)).stream()
                .collect(Collectors.toMap(TourDepartureRepository.OpenDepartureStats::getTourId, Function.identity()));
        Map<Long, TourResponses.Provider> providers = providersOf(tours);

        return tours.stream().map(tour -> {
            TourImage cover = covers.get(tour.getId());
            TourDepartureRepository.OpenDepartureStats stat = stats.get(tour.getId());
            return new TourResponses.Summary(
                    tour.getId(), tour.getTitle(), tour.getStatus(), tour.isPlatformTour(),
                    tour.isPlatformTour() ? null : providers.get(tour.getAgent().getId()),
                    cover == null ? null : fileStorageService.publicUrl(cover.getPublicId(), cover.getFormat()),
                    tour.getDepartureLocation().getProvince(), tour.isInternational(),
                    tour.getDurationDays(), tour.getDurationNights(),
                    stat == null ? 0 : stat.getOpenCount(), stat == null ? null : stat.getMinAdultPrice(),
                    tour.getSubmittedAt(), tour.getPublishedAt(), tour.getCreatedAt(), tour.getUpdatedAt());
        }).toList();
    }

    private TourResponses.Detail buildDetail(Tour tour, List<TourImage> images, List<TourDeparture> departures,
                                             List<String> missingItems, boolean internal) {
        Map<Long, Integer> booked = bookingStats.seatsBooked(departures.stream().map(TourDeparture::getId).toList());
        boolean draftLike = tour.getStatus() == TourStatus.DRAFT || tour.getStatus() == TourStatus.NEEDS_REVISION;

        return new TourResponses.Detail(
                tour.getId(), tour.getTitle(), tour.getStatus(),
                internal ? tour.getStatusReason() : null,
                tour.isPlatformTour(),
                tour.isPlatformTour() ? null : providersOf(List.of(tour)).get(tour.getAgent().getId()),
                tour.getCategories().stream()
                        .sorted(Comparator.comparing(TourCategory::getName))
                        .map(c -> new TourCategoryResponse(c.getId(), c.getName())).toList(),
                toLocation(tour.getDepartureLocation()),
                tour.getDestinations().stream()
                        .sorted(Comparator.comparing(Location::isVietnam).reversed()
                                .thenComparing(l -> l.getProvince() == null ? l.getCountry() : l.getProvince()))
                        .map(TourAssembler::toLocation).toList(),
                tour.isInternational(),
                tour.getDurationDays(), tour.getDurationNights(),
                List.copyOf(tour.getHighlights()),
                tour.getItineraryDays().stream().map(d -> new TourResponses.ItineraryDay(
                        d.getDayNumber(), d.getTitle(), d.getDescription(),
                        d.isHasBreakfast(), d.isHasLunch(), d.isHasDinner(), d.getAccommodation())).toList(),
                tour.getTransportModes().stream().sorted().toList(),
                tour.getAccommodationType(),
                tour.getMeetingPoint(), tour.getMeetingTime(),
                List.copyOf(tour.getIncludedServices()), List.copyOf(tour.getExcludedServices()),
                tour.getNotes(),
                images.stream().map(this::toImage).toList(),
                tour.hasItineraryFile()
                        ? new TourResponses.ItineraryFile(tour.getItineraryFileName(), tour.getItineraryFileSize())
                        : null,
                departures.stream()
                        .map(d -> toDeparture(d, tour.getDurationDays(), booked.getOrDefault(d.getId(), 0)))
                        .toList(),
                tour.getRating(), tour.getRatingCount(),
                tour.getSubmittedAt(), tour.getReviewedAt(), tour.getPublishedAt(),
                tour.getCreatedAt(), tour.getUpdatedAt(),
                internal && draftLike ? missingItems : List.of()
        );
    }

    /** agent userId -> thông tin công ty (1 câu query cho cả danh sách). */
    private Map<Long, TourResponses.Provider> providersOf(List<Tour> tours) {
        Set<Long> agentIds = tours.stream().filter(t -> !t.isPlatformTour())
                .map(t -> t.getAgent().getId()).collect(Collectors.toSet());
        if (agentIds.isEmpty()) return Map.of();
        return agentProfileRepository.findByUserIdIn(agentIds).stream().collect(Collectors.toMap(
                p -> p.getUser().getId(),
                p -> new TourResponses.Provider(p.getId(), p.getCompanyName(), p.getRating(), p.getRatingCount())));
    }

    private static LocationResponse toLocation(Location location) {
        return new LocationResponse(location.getId(), location.getCountry(), location.getProvince());
    }
}
