package com.tripconnect.backend.ai.assistant;

import com.tripconnect.backend.entity.Location;
import com.tripconnect.backend.entity.TourCategory;
import com.tripconnect.backend.repository.LocationRepository;
import com.tripconnect.backend.repository.TourCategoryRepository;
import com.tripconnect.backend.service.search.SearchText;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Đổi tên địa điểm / loại hình mô hình đưa ra (chữ tự do) thành bản ghi trong hệ thống.
 * So khớp không dấu, bỏ tiền tố "Tỉnh" / "Thành phố"; một số địa danh du lịch quen thuộc được quy về tỉnh.
 */
@Component
@RequiredArgsConstructor
public class PlaceMatcher {

    private static final Duration CACHE_TTL = Duration.ofMinutes(30);
    private static final Pattern PREFIX = Pattern.compile("^(tinh|thanh pho|tp)\\s+");

    /** Địa danh (không dấu) -> tỉnh (không dấu, đã bỏ tiền tố). */
    private static final Map<String, String> LANDMARKS = Map.ofEntries(
            Map.entry("sai gon", "ho chi minh"),
            Map.entry("ha long", "quang ninh"),
            Map.entry("vinh ha long", "quang ninh"),
            Map.entry("da lat", "lam dong"),
            Map.entry("sa pa", "lao cai"),
            Map.entry("sapa", "lao cai"),
            Map.entry("phu quoc", "kien giang"),
            Map.entry("nha trang", "khanh hoa"),
            Map.entry("hoi an", "quang nam"),
            // Huế thành thành phố trực thuộc trung ương từ 2025: dữ liệu cũ / mới đều tìm được
            Map.entry("hue", "thua thien hue"),
            Map.entry("thua thien hue", "hue"),
            Map.entry("mui ne", "binh thuan"),
            Map.entry("phan thiet", "binh thuan"),
            Map.entry("vung tau", "ba ria vung tau"),
            Map.entry("con dao", "ba ria vung tau"),
            Map.entry("quy nhon", "binh dinh"),
            Map.entry("moc chau", "son la"),
            Map.entry("tam dao", "vinh phuc"),
            Map.entry("cat ba", "hai phong"),
            Map.entry("mai chau", "hoa binh"),
            Map.entry("trang an", "ninh binh"),
            Map.entry("tam coc", "ninh binh"),
            Map.entry("phong nha", "quang binh"),
            Map.entry("buon ma thuot", "dak lak"),
            Map.entry("mien tay", "can tho"),
            Map.entry("ly son", "quang ngai"),
            Map.entry("co to", "quang ninh"),
            Map.entry("mu cang chai", "yen bai"),
            Map.entry("dong van", "ha giang"),
            Map.entry("pu luong", "thanh hoa"),
            Map.entry("sam son", "thanh hoa"),
            Map.entry("cua lo", "nghe an"),
            Map.entry("ba na", "da nang"));

    private final LocationRepository locationRepository;
    private final TourCategoryRepository categoryRepository;

    private volatile List<Location> locations = List.of();
    private volatile List<TourCategory> categories = List.of();
    private volatile Instant loadedAt = Instant.EPOCH;

    /** Địa điểm khớp tên nhất (khớp hẳn ưu tiên hơn khớp một phần). */
    public Optional<Location> location(String name) {
        String raw = key(name);
        if (raw.isEmpty()) return Optional.empty();
        List<Location> all = snapshotLocations();
        // Khớp hẳn tên gốc trước, rồi tới tỉnh của địa danh (Hạ Long -> Quảng Ninh)
        String alias = LANDMARKS.get(raw);
        for (String candidate : alias == null ? List.of(raw) : List.of(raw, alias)) {
            for (Location l : all) {
                if (key(label(l)).equals(candidate)) return Optional.of(l);
            }
        }
        String k = alias != null ? alias : raw;
        // Khớp một phần: chỉ chấp nhận khi đủ dài để tránh "an" khớp "Nghệ An"
        if (k.length() < 4) return Optional.empty();
        return all.stream()
                .filter(l -> {
                    String n = key(label(l));
                    return n.contains(k) || (n.length() >= 4 && k.contains(n));
                })
                // Ưu tiên tỉnh thành Việt Nam (có province) khi trùng tên với quốc gia
                .min((a, b) -> Boolean.compare(a.getProvince() == null, b.getProvince() == null));
    }

    public Optional<TourCategory> category(String name) {
        String key = SearchText.normalize(name);
        if (key.isEmpty()) return Optional.empty();
        snapshotLocations();
        return categories.stream()
                .filter(c -> {
                    String n = SearchText.normalize(c.getName());
                    return n.equals(key) || n.contains(key) || key.contains(n);
                })
                .findFirst();
    }

    /** Tên các loại hình tour (đưa vào mô tả công cụ để mô hình chọn đúng). */
    public List<String> categoryNames() {
        snapshotLocations();
        return categories.stream().map(TourCategory::getName).toList();
    }

    public static String label(Location l) {
        return l.getProvince() != null ? l.getProvince() : l.getCountry();
    }

    private static String key(String name) {
        return PREFIX.matcher(SearchText.normalize(name)).replaceFirst("");
    }

    private List<Location> snapshotLocations() {
        if (Instant.now().isAfter(loadedAt.plus(CACHE_TTL))) {
            synchronized (this) {
                if (Instant.now().isAfter(loadedAt.plus(CACHE_TTL))) {
                    locations = List.copyOf(locationRepository.findAll());
                    categories = List.copyOf(categoryRepository.findAll());
                    loadedAt = Instant.now();
                }
            }
        }
        return locations;
    }
}
