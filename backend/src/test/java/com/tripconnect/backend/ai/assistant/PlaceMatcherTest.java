package com.tripconnect.backend.ai.assistant;

import com.tripconnect.backend.entity.Location;
import com.tripconnect.backend.entity.TourCategory;
import com.tripconnect.backend.repository.LocationRepository;
import com.tripconnect.backend.repository.TourCategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;

/** Đổi tên địa điểm / loại hình mô hình đưa ra thành bản ghi trong hệ thống. */
@ExtendWith(MockitoExtension.class)
class PlaceMatcherTest {

    @Mock private LocationRepository locationRepository;
    @Mock private TourCategoryRepository categoryRepository;

    private PlaceMatcher matcher;

    @BeforeEach
    void setUp() {
        lenient().when(locationRepository.findAll()).thenReturn(List.of(
                location(1, "Việt Nam", "Thành phố Hà Nội"),
                location(2, "Việt Nam", "Tỉnh Quảng Ninh"),
                location(3, "Việt Nam", "Tỉnh Lâm Đồng"),
                location(4, "Việt Nam", "Tỉnh Nghệ An"),
                location(5, "Việt Nam", "Tỉnh Hoà Bình"),
                location(6, "Việt Nam", "Tỉnh Bà Rịa - Vũng Tàu"),
                location(7, "Nhật Bản", null),
                location(8, "Việt Nam", "Thành phố Hồ Chí Minh"),
                location(9, "Việt Nam", "Thành phố Huế")));
        lenient().when(categoryRepository.findAll()).thenReturn(List.of(category(7, "Biển đảo"), category(10, "Gia đình")));
        matcher = new PlaceMatcher(locationRepository, categoryRepository);
    }

    @Test
    void matchesProvinceIgnoringPrefixAndAccents() {
        assertThat(matcher.location("Hà Nội")).map(Location::getId).contains(1L);
        assertThat(matcher.location("tinh quang ninh")).map(Location::getId).contains(2L);
        // "Hòa" và "Hoà" (hai cách bỏ dấu) là một
        assertThat(matcher.location("Hòa Bình")).map(Location::getId).contains(5L);
        assertThat(matcher.location("TP Hồ Chí Minh")).map(Location::getId).contains(8L);
    }

    @Test
    void landmarksMapToTheirProvince() {
        assertThat(matcher.location("Hạ Long")).map(Location::getId).contains(2L);
        assertThat(matcher.location("Đà Lạt")).map(Location::getId).contains(3L);
        assertThat(matcher.location("Sài Gòn")).map(Location::getId).contains(8L);
        assertThat(matcher.location("Vũng Tàu")).map(Location::getId).contains(6L);
        // Tên mới và tên cũ của Huế
        assertThat(matcher.location("Huế")).map(Location::getId).contains(9L);
        assertThat(matcher.location("Thừa Thiên Huế")).map(Location::getId).contains(9L);
    }

    @Test
    void matchesCountryAndPartialNames() {
        assertThat(matcher.location("Nhật Bản")).map(Location::getId).contains(7L);
        assertThat(matcher.location("Bà Rịa")).map(Location::getId).contains(6L);
    }

    @Test
    void shortOrUnknownNamesDoNotMatch() {
        // "an" nằm trong "Nghệ An" nhưng quá ngắn để đoán
        assertThat(matcher.location("an")).isEmpty();
        assertThat(matcher.location("Atlantis")).isEmpty();
        assertThat(matcher.location("  ")).isEmpty();
        assertThat(matcher.location(null)).isEmpty();
    }

    @Test
    void matchesCategories() {
        assertThat(matcher.category("biển")).map(TourCategory::getId).contains(7L);
        assertThat(matcher.category("Gia Đình")).map(TourCategory::getId).contains(10L);
        assertThat(matcher.category("Vũ trụ")).isEmpty();
        assertThat(matcher.categoryNames()).containsExactly("Biển đảo", "Gia đình");
    }

    @Test
    void argsToleratesLooseTypes() {
        AssistantTools.Args args = new AssistantTools.Args(Map.of(
                "price", "5.000.000", "days", 3.0, "intl", "true", "list", List.of("A", " ", "B"), "single", "Huế"));
        assertThat(args.longValue("price")).isEqualTo(5_000_000L);
        assertThat(args.intValue("days")).isEqualTo(3);
        assertThat(args.bool("intl")).isTrue();
        assertThat(args.textList("list")).containsExactly("A", "B");
        assertThat(args.textList("single")).containsExactly("Huế");
        assertThat(args.text("missing", 10)).isNull();
    }

    private static Location location(long id, String country, String province) {
        Location l = new Location();
        l.setId(id);
        l.setCountry(country);
        l.setProvince(province);
        return l;
    }

    private static TourCategory category(long id, String name) {
        TourCategory c = new TourCategory();
        c.setId(id);
        c.setName(name);
        return c;
    }
}
