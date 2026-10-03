package com.tripconnect.backend.dto;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * Định dạng phân trang thống nhất cho mọi API danh sách.
 * (Không trả thẳng Page của Spring vì cấu trúc JSON của nó không ổn định giữa các phiên bản.)
 *
 * @param page số trang, bắt đầu từ 0
 */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public static <E, T> PageResponse<T> from(Page<E> page, Function<E, T> mapper) {
        return new PageResponse<>(
                page.getContent().stream().map(mapper).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    /** Khi nội dung trang đã được chuyển đổi sẵn theo lô (gom dữ liệu phụ cho cả trang bằng vài câu query). */
    public static <T> PageResponse<T> of(Page<?> page, List<T> content) {
        return new PageResponse<>(content, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
