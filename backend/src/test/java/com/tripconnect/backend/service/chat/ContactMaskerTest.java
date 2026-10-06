package com.tripconnect.backend.service.chat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class ContactMaskerTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', textBlock = """
            Gọi mình 0912345678 nhé                  | Gọi mình ••• nhé
            SĐT: 0912 345 678                        | SĐT: •••
            Zalo 091.234.5678 hoặc 091-234-5678      | Zalo ••• hoặc •••
            +84 912 345 678                          | •••
            84912345678                              | •••
            Email lan.nguyen@gmail.com nhé           | Email ••• nhé
            lan @ gmail . com                        | •••
            Giá 3.000.000 VNĐ / người                | Giá 3.000.000 VNĐ / người
            Tổng 300.000.000 VNĐ                     | Tổng 300.000.000 VNĐ
            Đoàn lớn 1.000.000.000 VNĐ               | Đoàn lớn 1.000.000.000 VNĐ
            Đón lúc 06:30 ngày 05/11/2026            | Đón lúc 06:30 ngày 05/11/2026
            Mã yêu cầu YC261006690460                | Mã yêu cầu YC261006690460
            """)
    void masksPhonesAndEmails_butNotPricesDatesOrCodes(String input, String expected) {
        assertThat(ContactMasker.mask(input)).isEqualTo(expected);
    }
}
