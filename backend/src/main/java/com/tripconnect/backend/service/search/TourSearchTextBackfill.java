package com.tripconnect.backend.service.search;

import com.tripconnect.backend.entity.Tour;
import com.tripconnect.backend.repository.TourRepository;
import com.tripconnect.backend.service.tour.TourContentWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Tour tạo trước khi có tính năng tìm kiếm (migration V9) có search_text rỗng -> điền khi khởi động.
 * Tour mới / vừa sửa đã được TourContentWriter điền sẵn nên lần sau không còn gì để làm.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TourSearchTextBackfill implements ApplicationRunner {

    private final TourRepository tourRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<Tour> tours = tourRepository.findBySearchText("");
        tours.forEach(tour -> tour.setSearchText(TourContentWriter.buildSearchText(tour)));
        if (!tours.isEmpty()) {
            log.info("Đã tạo chữ tìm kiếm cho {} tour", tours.size());
        }
    }
}
