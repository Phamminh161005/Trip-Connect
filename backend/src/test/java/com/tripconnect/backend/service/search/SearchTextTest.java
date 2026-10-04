package com.tripconnect.backend.service.search;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SearchTextTest {

    @Test
    void removesVietnameseAccentsAndPunctuation() {
        assertThat(SearchText.normalize("Hạ Long – Lan Hạ 3N2Đ!")).isEqualTo("ha long lan ha 3n2d");
        assertThat(SearchText.normalize("ĐÀ NẴNG")).isEqualTo("da nang");
        assertThat(SearchText.normalize("  Thành phố   Hồ Chí Minh ")).isEqualTo("thanh pho ho chi minh");
    }

    @Test
    void accentedAndUnaccentedInputGiveSameTokens() {
        assertThat(SearchText.tokens("Hạ Long")).isEqualTo(SearchText.tokens("ha long"));
        assertThat(SearchText.tokens("Đà Nẵng đà nẵng")).containsExactly("da", "nang");
    }

    @Test
    void likeWildcardsCannotBeInjected() {
        // % và _ bị bỏ khi chuẩn hóa -> từ khóa không thể biến thành mẫu "khớp mọi thứ"
        assertThat(SearchText.tokens("%_%")).isEmpty();
    }

    @Test
    void tokenCountIsLimited() {
        assertThat(SearchText.tokens("a b c d e f g h")).hasSize(SearchText.MAX_TOKENS);
    }

    @Test
    void joinSkipsEmptyParts() {
        assertThat(SearchText.join(List.of("Hạ Long", "", "Biển đảo"))).isEqualTo("ha long bien dao");
    }
}
