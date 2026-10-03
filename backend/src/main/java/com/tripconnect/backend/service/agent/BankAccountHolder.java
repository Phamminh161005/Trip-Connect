package com.tripconnect.backend.service.agent;

import java.text.Normalizer;
import java.util.Locale;

/** Chuẩn hóa tên chủ tài khoản giống cách ngân hàng hiển thị: CHỮ IN HOA, KHÔNG DẤU, 1 khoảng trắng giữa các từ. */
public final class BankAccountHolder {

    private BankAccountHolder() {
    }

    public static String normalize(String name) {
        if (name == null) return null;
        String noMarks = Normalizer.normalize(name, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")      // bỏ dấu thanh, dấu mũ...
                .replace('đ', 'd').replace('Đ', 'D');
        return noMarks.trim().replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
    }
}
