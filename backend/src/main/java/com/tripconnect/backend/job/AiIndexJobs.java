package com.tripconnect.backend.job;

import com.tripconnect.backend.ai.AiUnavailableException;
import com.tripconnect.backend.ai.rag.KnowledgeBase;
import com.tripconnect.backend.ai.rag.TourEmbeddingIndexer;
import com.tripconnect.backend.repository.TourViewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

/** Tạo / làm mới véc-tơ cho kho kiến thức và tour đang bán; dọn lượt xem tour cũ. */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiIndexJobs {

    /** Lượt xem tour giữ ngần này ngày (đủ cho gợi ý theo sở thích gần đây). */
    static final int KEEP_VIEWS_DAYS = 180;

    private final KnowledgeBase knowledgeBase;
    private final TourEmbeddingIndexer tourIndexer;
    private final TourViewRepository tourViewRepository;
    private final Clock clock;

    /** Khởi động xong: chạy nền để không làm chậm lúc bật backend. */
    @Async
    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        try {
            int chunks = knowledgeBase.reindex();
            int tours = tourIndexer.reindex();
            log.info("Trợ lý AI: tạo véc-tơ cho {} đoạn kiến thức, {} tour", chunks, tours);
        } catch (AiUnavailableException e) {
            log.warn("Trợ lý AI: chưa tạo được véc-tơ ({}), dùng véc-tơ đã lưu", e.getMessage());
            knowledgeBase.reloadIndex();
            tourIndexer.reloadIndex();
        }
    }

    /** Mỗi 15 phút: tour mới được duyệt / đổi nội dung / ngừng bán. */
    @Scheduled(fixedDelay = 900_000, initialDelay = 900_000)
    public void refreshTours() {
        try {
            int tours = tourIndexer.reindex();
            if (tours > 0) log.info("Trợ lý AI: cập nhật véc-tơ cho {} tour", tours);
        } catch (AiUnavailableException e) {
            log.warn("Trợ lý AI: chưa cập nhật được véc-tơ tour ({})", e.getMessage());
        }
    }

    /** 04:00 hằng ngày: xóa lượt xem tour quá cũ. */
    @Scheduled(cron = "0 0 4 * * *", zone = "Asia/Ho_Chi_Minh")
    @Transactional
    public void cleanupViews() {
        int removed = tourViewRepository.deleteOlderThan(LocalDateTime.now(clock).minusDays(KEEP_VIEWS_DAYS));
        if (removed > 0) log.info("Đã xóa {} lượt xem tour cũ", removed);
    }
}
