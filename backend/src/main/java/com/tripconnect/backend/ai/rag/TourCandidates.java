package com.tripconnect.backend.ai.rag;

import com.tripconnect.backend.dto.PageResponse;
import com.tripconnect.backend.dto.search.SearchResponses;
import com.tripconnect.backend.dto.search.TourSearchRequest;
import com.tripconnect.backend.service.search.TourSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Tour đang bán (còn lịch đặt được) khớp bộ lọc — tập ứng viên để xếp hạng theo véc-tơ.
 * Dùng chung truy vấn của trang tìm kiếm nên tour hết lịch / ngừng bán tự bị loại.
 */
@Component
@RequiredArgsConstructor
public class TourCandidates {

    private static final int PAGE_SIZE = 50;

    private final TourSearchService searchService;

    public record Result(List<SearchResponses.TourCard> cards, long total) {
    }

    /** Tối đa max tour, theo thứ tự gợi ý mặc định của trang tìm kiếm. filters bị đổi page / size. */
    public Result find(TourSearchRequest filters, int max) {
        filters.setSize(PAGE_SIZE);
        List<SearchResponses.TourCard> cards = new ArrayList<>();
        long total = 0;
        for (int page = 0; cards.size() < max; page++) {
            filters.setPage(page);
            PageResponse<SearchResponses.TourCard> result = searchService.search(filters);
            cards.addAll(result.content());
            total = result.totalElements();
            if (page + 1 >= result.totalPages()) break;
        }
        return new Result(cards.size() > max ? cards.subList(0, max) : cards, total);
    }
}
