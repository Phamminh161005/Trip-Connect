package com.tripconnect.backend.config;

import java.util.Map;

/**
 * Tên tiếng Việt của các quốc gia hay đi du lịch (API seed trả tên tiếng Anh).
 * Quốc gia có tên giống nhau (Singapore, Malaysia, Canada...) không cần liệt kê.
 * Danh sách giống hệt migration V8__vietnamese_country_names.sql — sửa một nơi thì sửa cả nơi kia.
 */
public final class CountryNames {

    private CountryNames() {
    }

    private static final Map<String, String> VIETNAMESE = Map.ofEntries(
            Map.entry("Thailand", "Thái Lan"),
            Map.entry("Japan", "Nhật Bản"),
            Map.entry("South Korea", "Hàn Quốc"),
            Map.entry("North Korea", "Triều Tiên"),
            Map.entry("China", "Trung Quốc"),
            Map.entry("Taiwan", "Đài Loan"),
            Map.entry("Hong Kong", "Hồng Kông"),
            Map.entry("Macau", "Ma Cao"),
            Map.entry("Cambodia", "Campuchia"),
            Map.entry("Laos", "Lào"),
            Map.entry("Timor-Leste", "Đông Timor"),
            Map.entry("India", "Ấn Độ"),
            Map.entry("Mongolia", "Mông Cổ"),
            Map.entry("Turkey", "Thổ Nhĩ Kỳ"),
            Map.entry("United Arab Emirates", "Các Tiểu vương quốc Ả Rập Thống nhất"),
            Map.entry("Saudi Arabia", "Ả Rập Xê Út"),
            Map.entry("Egypt", "Ai Cập"),
            Map.entry("Morocco", "Ma-rốc"),
            Map.entry("South Africa", "Nam Phi"),
            Map.entry("France", "Pháp"),
            Map.entry("Germany", "Đức"),
            Map.entry("Italy", "Ý"),
            Map.entry("Spain", "Tây Ban Nha"),
            Map.entry("Portugal", "Bồ Đào Nha"),
            Map.entry("United Kingdom", "Anh"),
            Map.entry("Netherlands", "Hà Lan"),
            Map.entry("Belgium", "Bỉ"),
            Map.entry("Switzerland", "Thụy Sĩ"),
            Map.entry("Austria", "Áo"),
            Map.entry("Czech Republic", "Séc"),
            Map.entry("Poland", "Ba Lan"),
            Map.entry("Greece", "Hy Lạp"),
            Map.entry("Norway", "Na Uy"),
            Map.entry("Sweden", "Thụy Điển"),
            Map.entry("Finland", "Phần Lan"),
            Map.entry("Denmark", "Đan Mạch"),
            Map.entry("Russia", "Nga"),
            Map.entry("Ukraine", "Ukraina"),
            Map.entry("United States", "Hoa Kỳ"),
            Map.entry("Australia", "Úc"),
            Map.entry("Vatican City State (Holy See)", "Vatican")
    );

    public static String toVietnamese(String englishName) {
        return VIETNAMESE.getOrDefault(englishName, englishName);
    }
}
