package com.tripconnect.backend.service.tour;

import com.tripconnect.backend.dto.tour.TourContentRequest;
import com.tripconnect.backend.entity.Location;
import com.tripconnect.backend.entity.Tour;
import com.tripconnect.backend.entity.TourCategory;
import com.tripconnect.backend.entity.TourItineraryDay;
import com.tripconnect.backend.enums.AccommodationType;
import com.tripconnect.backend.enums.TransportMode;
import com.tripconnect.backend.repository.LocationRepository;
import com.tripconnect.backend.repository.TourCategoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class TourContentWriterTest {

    private static final Location HANOI = location(1L, Location.VIETNAM, "Thành phố Hà Nội");
    private static final Location QUANG_NINH = location(14L, Location.VIETNAM, "Tỉnh Quảng Ninh");
    private static final Location JAPAN = location(171L, "Nhật Bản", null);

    @Mock private LocationRepository locationRepository;
    @Mock private TourCategoryRepository categoryRepository;

    @InjectMocks private TourContentWriter writer;

    @Test
    void nightsMustEqualDaysOrOneLess() {
        TourContentRequest request = request(3, 1, List.of(14L));
        assertThatThrownBy(() -> writer.apply(new Tour(), request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Số đêm");
    }

    @Test
    void itineraryMustHaveExactlyOneEntryPerDay() {
        TourContentRequest request = request(3, 2, List.of(14L));
        request.setItinerary(request.getItinerary().subList(0, 2));
        assertThatThrownBy(() -> writer.apply(new Tour(), request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("đúng 3 ngày");
    }

    @Test
    void departureMustBeVietnameseProvince() {
        stubLocations();
        TourContentRequest request = request(2, 1, List.of(14L));
        request.setDepartureLocationId(JAPAN.getId());
        assertThatThrownBy(() -> writer.apply(new Tour(), request))
                .hasMessageContaining("Nơi khởi hành");
    }

    @Test
    void tourWithForeignDestination_isInternational() {
        stubLocations();
        Tour domestic = new Tour();
        writer.apply(domestic, request(2, 1, List.of(14L)));
        assertThat(domestic.isInternational()).isFalse();

        Tour international = new Tour();
        writer.apply(international, request(2, 1, List.of(14L, 171L)));
        assertThat(international.isInternational()).isTrue();
    }

    @Test
    void shorteningTour_updatesDaysInPlaceAndRemovesExtraDays() {
        stubLocations();
        Tour tour = new Tour();
        writer.apply(tour, request(3, 2, List.of(14L)));
        TourItineraryDay firstDay = tour.getItineraryDays().get(0);

        TourContentRequest shorter = request(2, 1, List.of(14L));
        shorter.getItinerary().get(0).setTitle("Ngày 1 mới");
        writer.apply(tour, shorter);

        assertThat(tour.getItineraryDays()).hasSize(2);
        // Cùng một đối tượng (sửa tại chỗ) -> không vướng UNIQUE (tour_id, day_number) khi lưu
        assertThat(tour.getItineraryDays().get(0)).isSameAs(firstDay);
        assertThat(firstDay.getTitle()).isEqualTo("Ngày 1 mới");
    }

    private void stubLocations() {
        lenient().when(locationRepository.findById(HANOI.getId())).thenReturn(Optional.of(HANOI));
        lenient().when(locationRepository.findById(JAPAN.getId())).thenReturn(Optional.of(JAPAN));
        lenient().when(locationRepository.findAllById(anyList())).thenAnswer(inv -> {
            List<Long> ids = inv.getArgument(0);
            return List.of(QUANG_NINH, JAPAN).stream().filter(l -> ids.contains(l.getId())).toList();
        });
        TourCategory category = new TourCategory();
        category.setId(7L);
        category.setName("Biển đảo");
        lenient().when(categoryRepository.findAllById(anyList())).thenReturn(List.of(category));
    }

    private static TourContentRequest request(int days, int nights, List<Long> destinationIds) {
        TourContentRequest request = new TourContentRequest();
        request.setTitle("Tour thử nghiệm đủ dài");
        request.setCategoryIds(List.of(7L));
        request.setDepartureLocationId(HANOI.getId());
        request.setDestinationIds(destinationIds);
        request.setDurationDays(days);
        request.setDurationNights(nights);
        request.setHighlights(List.of("A", "B", "C"));
        request.setItinerary(IntStream.rangeClosed(1, days).mapToObj(i -> {
            TourContentRequest.ItineraryDayRequest day = new TourContentRequest.ItineraryDayRequest();
            day.setTitle("Ngày " + i);
            day.setDescription("Nội dung ngày " + i);
            return day;
        }).toList());
        request.setTransportModes(Set.of(TransportMode.BUS));
        request.setAccommodationType(AccommodationType.HOTEL_4_STAR);
        request.setMeetingPoint("Nhà hát Lớn");
        request.setMeetingTime(LocalTime.of(7, 30));
        request.setIncludedServices(List.of("Xe đưa đón"));
        return request;
    }

    private static Location location(Long id, String country, String province) {
        Location location = new Location();
        location.setId(id);
        location.setCountry(country);
        location.setProvince(province);
        return location;
    }
}
