package com.tripconnect.backend.repository;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StablePagingTest {

    @Test
    void addsIdAsTieBreaker_inSameDirectionAsMainSort() {
        Pageable result = StablePaging.of(PageRequest.of(2, 20, Sort.by(Sort.Direction.DESC, "createdAt")));

        assertThat(result.getPageNumber()).isEqualTo(2);
        assertThat(result.getPageSize()).isEqualTo(20);
        assertThat(result.getSort()).containsExactly(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));

        Pageable ascending = StablePaging.of(PageRequest.of(0, 20, Sort.by("submittedAt")));
        assertThat(ascending.getSort()).containsExactly(Sort.Order.asc("submittedAt"), Sort.Order.asc("id"));
    }

    @Test
    void unsortedRequest_isSortedById() {
        assertThat(StablePaging.of(PageRequest.of(0, 20)).getSort()).containsExactly(Sort.Order.desc("id"));
    }

    @Test
    void alreadySortedById_isUnchanged() {
        Pageable pageable = PageRequest.of(0, 20, Sort.by("id"));
        assertThat(StablePaging.of(pageable)).isSameAs(pageable);
    }

    @Test
    void tooDeepPage_isRejected() {
        assertThat(StablePaging.of(PageRequest.of(StablePaging.MAX_PAGE, 20))).isNotNull();
        assertThatThrownBy(() -> StablePaging.of(PageRequest.of(StablePaging.MAX_PAGE + 1, 20)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Số trang quá lớn");
    }
}
