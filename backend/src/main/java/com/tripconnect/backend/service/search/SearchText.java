package com.tripconnect.backend.service.search;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Chuẩn hóa chữ để tìm kiếm tiếng Việt: chữ thường, bỏ dấu, "đ" -> "d", gộp khoảng trắng.
 * "Hạ Long – Lan Hạ" -> "ha long lan ha". Dùng CẢ khi lưu tour (cột search_text) lẫn khi tìm,
 * nên gõ có dấu hay không dấu đều ra cùng kết quả.
 */
public final class SearchText {

    private SearchText() {
    }

    private static final Pattern MARKS = Pattern.compile("\\p{M}+");
    private static final Pattern NON_WORD = Pattern.compile("[^a-z0-9]+");
    /** Giới hạn số từ khóa để câu truy vấn không quá dài. */
    static final int MAX_TOKENS = 6;

    public static String normalize(String text) {
        if (text == null) return "";
        String noMarks = MARKS.matcher(Normalizer.normalize(text, Normalizer.Form.NFD)).replaceAll("");
        String lower = noMarks.replace('đ', 'd').replace('Đ', 'd').toLowerCase(Locale.ROOT);
        return NON_WORD.matcher(lower).replaceAll(" ").trim();
    }

    /** Các từ khóa (đã chuẩn hóa) — tour phải chứa TẤT CẢ các từ. */
    public static List<String> tokens(String keyword) {
        String normalized = normalize(keyword);
        if (normalized.isEmpty()) return List.of();
        return Arrays.stream(normalized.split(" ")).distinct().limit(MAX_TOKENS).toList();
    }

    /** Ghép nhiều đoạn chữ thành một chuỗi tìm kiếm. */
    public static String join(List<String> parts) {
        return parts.stream().map(SearchText::normalize).filter(s -> !s.isEmpty())
                .reduce((a, b) -> a + " " + b).orElse("");
    }
}
