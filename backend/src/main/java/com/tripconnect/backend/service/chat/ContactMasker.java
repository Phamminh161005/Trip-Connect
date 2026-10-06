package com.tripconnect.backend.service.chat;

import java.util.regex.Pattern;

/**
 * Che số điện thoại / email trong tin nhắn để khách và đơn vị tổ chức giao dịch qua TripConnect.
 * Chỉ che số dạng điện thoại Việt Nam (0xxx / +84 / 84, cho phép dấu cách, chấm, gạch) nên giá tiền
 * như "3.000.000" không bị che.
 */
public final class ContactMasker {

    private ContactMasker() {
    }

    public static final String MASK = "•••";

    private static final Pattern EMAIL = Pattern.compile("[A-Za-z0-9._%+-]+\\s*@\\s*[A-Za-z0-9-]+(\\s*\\.\\s*[A-Za-z0-9-]+)+");
    /** 0 hoặc +84 / 84 rồi 8–10 chữ số, xen dấu cách / chấm / gạch / ngoặc; không bắt đầu giữa một số tiền (sau "." / ","). */
    private static final Pattern PHONE = Pattern.compile("(?<![\\d.,])(?:\\+\\s*84|84|0)(?:[\\s.\\-()]*\\d){8,10}(?![\\d])");

    public static String mask(String text) {
        if (text == null || text.isEmpty()) return text;
        String masked = EMAIL.matcher(text).replaceAll(MASK);
        return PHONE.matcher(masked).replaceAll(MASK);
    }
}
