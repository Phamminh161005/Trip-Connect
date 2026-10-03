package com.tripconnect.backend.dto;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class ValidationPatternsTest {

    @ParameterizedTest
    @ValueSource(strings = {"0101234567", "0101234567-001", "001203004567"})
    void taxCode_acceptsValidFormats(String taxCode) {
        assertThat(taxCode).matches(ValidationPatterns.TAX_CODE);
    }

    @ParameterizedTest
    @ValueSource(strings = {"093984382SHSD", "123456789", "01012345678", "0101234567-01", "0101234567001", "", " 0101234567"})
    void taxCode_rejectsInvalidFormats(String taxCode) {
        assertThat(taxCode).doesNotMatch(ValidationPatterns.TAX_CODE);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "0101234567"})
    void taxCodeOrEmpty_allowsBlankForNoChange(String taxCode) {
        assertThat(taxCode).matches(ValidationPatterns.TAX_CODE_OR_EMPTY);
    }
}
