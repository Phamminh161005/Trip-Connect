package com.tripconnect.backend.service.agent;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BankAccountHolderTest {

    @Test
    void normalize_removesVietnameseDiacriticsAndUppercases() {
        assertThat(BankAccountHolder.normalize("  Công ty TNHH   Du lịch Đồng Hành ")).isEqualTo("CONG TY TNHH DU LICH DONG HANH");
    }

    @Test
    void normalize_keepsNull() {
        assertThat(BankAccountHolder.normalize(null)).isNull();
    }
}
