package com.tripconnect.backend.service;

import java.util.Arrays;

/** Tên hiển thị cho người ngoài (trang tour, Agent xem yêu cầu của khách). */
public final class DisplayNames {

    private DisplayNames() {
    }

    /** "Phạm Văn Minh" -> "Phạm V. Minh"; giữ họ và tên, tên đệm chỉ lấy chữ cái đầu. */
    public static String masked(String fullName) {
        if (fullName == null || fullName.isBlank()) return "Khách hàng";
        String[] words = fullName.trim().split("\s+");
        if (words.length <= 2) return String.join(" ", words);
        String middle = Arrays.stream(words, 1, words.length - 1)
                .map(w -> w.substring(0, 1).toUpperCase() + ".")
                .reduce((a, b) -> a + " " + b).orElse("");
        return words[0] + " " + middle + " " + words[words.length - 1];
    }
}
