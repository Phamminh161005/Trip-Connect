package com.tripconnect.backend.repository;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Thêm id làm tiêu chí sắp xếp phụ để phân trang ổn định; chặn OFFSET quá sâu.
 * Kích thước trang tối đa: spring.data.web.pageable.max-page-size.
 */
public final class StablePaging {

    private StablePaging() {
    }

    /** Trang tối đa (bắt đầu từ 0). 500 trang x 20 dòng = 10.000 dòng — đủ xa so với nhu cầu thật. */
    public static final int MAX_PAGE = 500;

    public static Pageable of(Pageable pageable) {
        if (pageable.isUnpaged()) return pageable;
        if (pageable.getPageNumber() > MAX_PAGE) {
            throw new IllegalArgumentException("Số trang quá lớn, vui lòng thu hẹp bộ lọc tìm kiếm");
        }
        Sort sort = pageable.getSort();
        if (sort.getOrderFor("id") != null) return pageable;
        // Hướng sắp xếp phụ theo hướng của tiêu chí chính (mới nhất trước -> id lớn trước)
        Sort.Direction direction = sort.stream().findFirst().map(Sort.Order::getDirection).orElse(Sort.Direction.DESC);
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort.and(Sort.by(direction, "id")));
    }
}
