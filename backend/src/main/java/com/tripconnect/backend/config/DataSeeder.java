package com.tripconnect.backend.config;

import com.tripconnect.backend.entity.Location;
import com.tripconnect.backend.repository.LocationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    // Loại hình tour được seed bằng Flyway (db/migration/V2__seed_tour_categories.sql).
    // Ở đây chỉ còn địa điểm, vì dữ liệu này tải từ API ngoài lúc khởi động.
    private final LocationRepository locationRepository;

    @Override
    public void run(String... args) {
        seedLocations();
    }

    private void seedLocations() {
        if (locationRepository.count() > 0) {
            log.info("Bảng locations đã có dữ liệu, bỏ qua seed.");
            return;
        }

        RestClient restClient = RestClient.create();
        List<Location> locations = new ArrayList<>();

        seedVietnamProvinces(restClient, locations);
        seedOtherCountries(restClient, locations);

        if (!locations.isEmpty()) {
            locationRepository.saveAll(locations);
            log.info("Đã khởi tạo {} địa điểm.", locations.size());
        } else {
            log.warn("Không seed được địa điểm nào — kiểm tra kết nối mạng tới API ngoài.");
        }
    }

    private void seedVietnamProvinces(RestClient restClient, List<Location> locations) {
        try {
            VnProvinceDto[] provinces = restClient.get()
                    .uri("https://provinces.open-api.vn/api/v1/")
                    .retrieve()
                    .body(VnProvinceDto[].class);

            if (provinces != null) {
                for (VnProvinceDto p : provinces) {
                    Location location = new Location();
                    location.setCountry("Việt Nam");
                    location.setProvince(p.name());
                    locations.add(location);
                }
            }
        } catch (Exception e) {
            log.warn("Không lấy được danh sách tỉnh/thành Việt Nam từ provinces.open-api.vn: {}", e.getMessage());
        }
    }

    private void seedOtherCountries(RestClient restClient, List<Location> locations) {
        try {
            CountriesNowResponse response = restClient.get()
                    .uri("https://countriesnow.space/api/v0.1/countries")
                    .retrieve()
                    .body(CountriesNowResponse.class);

            if (response != null && !response.error() && response.data() != null) {
                for (CountryEntry entry : response.data()) {
                    if (entry.country() == null || "Vietnam".equalsIgnoreCase(entry.country())) {
                        continue; // Việt Nam đã được thêm chi tiết theo từng tỉnh/thành ở trên
                    }
                    Location location = new Location();
                    location.setCountry(entry.country());
                    location.setProvince(null);
                    locations.add(location);
                }
            }
        } catch (Exception e) {
            log.warn("Không lấy được danh sách quốc gia từ countriesnow.space: {}", e.getMessage());
        }
    }

    private record VnProvinceDto(String name, Integer code) {
    }

    // Chỉ cần error + data
    private record CountriesNowResponse(boolean error, List<CountryEntry> data) {
    }

    // Chỉ lấy field "country", bỏ qua "iso2", "iso3", "cities" (không cần dùng)
    private record CountryEntry(String country) {
    }
}