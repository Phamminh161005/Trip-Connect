package com.tripconnect.backend.repository;

import java.util.Locale;

/** Tạo mẫu tìm kiếm gần đúng cho câu LIKE (dùng chung cho các trang tìm kiếm của Admin). */
public final class SearchPatterns {

    /** Ký tự thoát dùng trong LIKE ... ESCAPE '\'. */
    public static final char ESCAPE = '\\';

    private SearchPatterns() {
    }

    /**
     * "Nguyễn_A%" -> "%nguyễn\_a\%%": chữ thường + bọc % hai đầu.
     * Ký tự % và _ người dùng gõ được hiểu là chữ bình thường, không phải ký tự đại diện của LIKE.
     */
    public static String contains(String keyword) {
        String escaped = keyword.trim().toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + escaped + "%";
    }
}
