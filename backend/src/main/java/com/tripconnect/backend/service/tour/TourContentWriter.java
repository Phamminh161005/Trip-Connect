package com.tripconnect.backend.service.tour;

import com.tripconnect.backend.dto.tour.TourContentRequest;
import com.tripconnect.backend.entity.Location;
import com.tripconnect.backend.entity.Tour;
import com.tripconnect.backend.entity.TourCategory;
import com.tripconnect.backend.entity.TourItineraryDay;
import com.tripconnect.backend.repository.LocationRepository;
import com.tripconnect.backend.repository.TourCategoryRepository;
import com.tripconnect.backend.service.search.SearchText;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/** Kiểm tra các quy tắc liên quan nhiều trường rồi ghi nội dung tour từ request vào entity. */
@Component
@RequiredArgsConstructor
public class TourContentWriter {

    private final LocationRepository locationRepository;
    private final TourCategoryRepository categoryRepository;

    public void apply(Tour tour, TourContentRequest request) {
        int days = request.getDurationDays();
        int nights = request.getDurationNights();
        if (nights != days && nights != days - 1) {
            throw new IllegalArgumentException("Số đêm phải bằng số ngày hoặc ít hơn 1 (vd 3 ngày 2 đêm)");
        }
        if (request.getItinerary().size() != days) {
            throw new IllegalArgumentException("Lịch trình phải có đúng " + days + " ngày, bằng số ngày của tour");
        }

        Location departure = locationRepository.findById(request.getDepartureLocationId())
                .filter(Location::isVietnamProvince)
                .orElseThrow(() -> new IllegalArgumentException("Nơi khởi hành phải là một Tỉnh/Thành của Việt Nam"));
        List<Location> destinations = loadDestinations(request.getDestinationIds());

        tour.setTitle(request.getTitle().trim());
        tour.setCategories(new HashSet<>(loadCategories(request.getCategoryIds())));
        tour.setDepartureLocation(departure);
        tour.setDestinations(new HashSet<>(destinations));
        tour.setInternational(destinations.stream().anyMatch(l -> !l.isVietnam()));
        tour.setDurationDays((short) days);
        tour.setDurationNights((short) nights);
        tour.setHighlights(trimAll(request.getHighlights()));
        tour.setTransportModes(new HashSet<>(request.getTransportModes()));
        tour.setAccommodationType(request.getAccommodationType());
        tour.setMeetingPoint(request.getMeetingPoint().trim());
        tour.setMeetingTime(request.getMeetingTime());
        tour.setIncludedServices(trimAll(request.getIncludedServices()));
        tour.setExcludedServices(trimAll(request.getExcludedServices()));
        tour.setNotes(trimToNull(request.getNotes()));
        writeItinerary(tour, request.getItinerary());
        tour.setSearchText(buildSearchText(tour));
    }

    /** Chuỗi tìm kiếm không dấu: tên tour + điểm đến + loại hình + điểm nổi bật. */
    public static String buildSearchText(Tour tour) {
        List<String> parts = new ArrayList<>();
        parts.add(tour.getTitle());
        tour.getDestinations().forEach(l -> parts.add(l.getProvince() != null ? l.getProvince() : l.getCountry()));
        tour.getCategories().forEach(c -> parts.add(c.getName()));
        parts.addAll(tour.getHighlights());
        return SearchText.join(parts);
    }

    /**
     * Sửa tại chỗ theo số thứ tự ngày thay vì xóa hết rồi thêm lại: Hibernate chạy lệnh INSERT trước DELETE,
     * nếu thay toàn bộ sẽ vướng ràng buộc UNIQUE (tour_id, day_number).
     */
    private void writeItinerary(Tour tour, List<TourContentRequest.ItineraryDayRequest> items) {
        List<TourItineraryDay> days = tour.getItineraryDays();
        days.removeIf(day -> day.getDayNumber() > items.size());

        for (int i = 0; i < items.size(); i++) {
            short dayNumber = (short) (i + 1);
            TourItineraryDay day = days.stream().filter(d -> d.getDayNumber() == dayNumber).findFirst().orElseGet(() -> {
                TourItineraryDay created = new TourItineraryDay();
                created.setTour(tour);
                created.setDayNumber(dayNumber);
                days.add(created);
                return created;
            });
            TourContentRequest.ItineraryDayRequest item = items.get(i);
            day.setTitle(item.getTitle().trim());
            day.setDescription(item.getDescription().trim());
            day.setHasBreakfast(item.isBreakfast());
            day.setHasLunch(item.isLunch());
            day.setHasDinner(item.isDinner());
            day.setAccommodation(trimToNull(item.getAccommodation()));
        }
    }

    private List<Location> loadDestinations(List<Long> ids) {
        List<Long> distinctIds = ids.stream().distinct().toList();
        List<Location> locations = locationRepository.findAllById(distinctIds);
        if (locations.size() != distinctIds.size()) {
            throw new IllegalArgumentException("Có điểm đến không tồn tại");
        }
        return locations;
    }

    private List<TourCategory> loadCategories(List<Long> ids) {
        List<Long> distinctIds = ids.stream().distinct().toList();
        List<TourCategory> categories = categoryRepository.findAllById(distinctIds);
        if (categories.size() != distinctIds.size()) {
            throw new IllegalArgumentException("Có loại hình tour không tồn tại");
        }
        return categories;
    }

    private static List<String> trimAll(List<String> values) {
        return values == null ? new ArrayList<>() : new ArrayList<>(values.stream().map(String::trim).toList());
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
