package com.tripconnect.backend.ai.rag;

import com.tripconnect.backend.ai.LlmClient;
import com.tripconnect.backend.entity.*;
import com.tripconnect.backend.enums.TourStatus;
import com.tripconnect.backend.repository.TourEmbeddingRepository;
import com.tripconnect.backend.repository.TourRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Tạo véc-tơ cho tour đang bán: dựng một đoạn văn mô tả tour (tên, nơi đi, điểm đến, loại hình, điểm nổi bật, lịch trình),
 * so mã băm với lần trước — chỉ gọi Gemini cho tour mới / đổi nội dung. Tour không còn bán thì bỏ véc-tơ.
 */
@Slf4j
@Component
public class TourEmbeddingIndexer {

    /** Giới hạn độ dài đoạn mô tả (lịch trình dài chỉ lấy phần đầu). */
    static final int MAX_CHARS = 6000;

    private final TourRepository tourRepository;
    private final TourEmbeddingRepository embeddingRepository;
    private final LlmClient llm;
    private final VectorIndex index;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;
    private final String model;

    public TourEmbeddingIndexer(TourRepository tourRepository, TourEmbeddingRepository embeddingRepository, LlmClient llm,
                                VectorIndex index, TransactionTemplate transactionTemplate, Clock clock,
                                @Value("${ai.gemini.embedding-model}") String model) {
        this.tourRepository = tourRepository;
        this.embeddingRepository = embeddingRepository;
        this.llm = llm;
        this.index = index;
        this.transactionTemplate = transactionTemplate;
        this.clock = clock;
        this.model = model;
    }

    private record Doc(Long tourId, String text, String hash) {
    }

    /** @return số tour vừa tạo lại véc-tơ */
    public synchronized int reindex() {
        List<Doc> docs = transactionTemplate.execute(status -> tourRepository.findWithContentByStatus(TourStatus.PUBLISHED).stream()
                .map(t -> {
                    String text = describe(t);
                    return new Doc(t.getId(), text, sha256(model + "\n" + text));
                }).toList());
        if (docs == null) return 0;
        Map<Long, TourEmbedding> existing = embeddingRepository.findAll().stream()
                .collect(Collectors.toMap(TourEmbedding::getTourId, e -> e));
        List<Doc> stale = docs.stream().filter(d -> {
            TourEmbedding e = existing.get(d.tourId());
            return e == null || !e.getContentHash().equals(d.hash());
        }).toList();

        if (!stale.isEmpty() && llm.isConfigured()) {
            List<float[]> vectors = llm.embed(stale.stream().map(Doc::text).toList(), LlmClient.EmbedTask.DOCUMENT);
            LocalDateTime now = LocalDateTime.now(clock);
            List<TourEmbedding> rows = new ArrayList<>();
            for (int i = 0; i < stale.size(); i++) {
                TourEmbedding e = existing.getOrDefault(stale.get(i).tourId(), new TourEmbedding());
                e.setTourId(stale.get(i).tourId());
                e.setModel(model);
                e.setContentHash(stale.get(i).hash());
                e.setVector(vectors.get(i));
                e.setUpdatedAt(now);
                rows.add(e);
            }
            embeddingRepository.saveAll(rows);
        }
        Set<Long> published = docs.stream().map(Doc::tourId).collect(Collectors.toSet());
        transactionTemplate.executeWithoutResult(status -> {
            if (published.isEmpty()) embeddingRepository.deleteAllInBatch();
            else embeddingRepository.deleteAllExcept(published);
        });
        reloadIndex();
        return llm.isConfigured() ? stale.size() : 0;
    }

    /** Nạp véc-tơ từ DB vào bộ nhớ (chỉ của model hiện tại). */
    public void reloadIndex() {
        Map<Long, float[]> vectors = embeddingRepository.findAll().stream()
                .filter(e -> model.equals(e.getModel()))
                .collect(Collectors.toMap(TourEmbedding::getTourId, TourEmbedding::getVector));
        index.replaceTours(vectors);
    }

    /** Đoạn văn mô tả tour để tạo véc-tơ (gọi trong transaction để đọc lịch trình). */
    static String describe(Tour t) {
        StringBuilder sb = new StringBuilder();
        sb.append("Tour: ").append(t.getTitle()).append('\n');
        sb.append("Khởi hành từ: ").append(place(t.getDepartureLocation())).append('\n');
        sb.append("Điểm đến: ").append(t.getDestinations().stream().map(TourEmbeddingIndexer::place).sorted()
                .collect(Collectors.joining(", "))).append('\n');
        if (!t.getCategories().isEmpty()) {
            sb.append("Loại hình: ").append(t.getCategories().stream().map(TourCategory::getName).sorted()
                    .collect(Collectors.joining(", "))).append('\n');
        }
        sb.append("Thời lượng: ").append(t.getDurationDays()).append(" ngày ").append(t.getDurationNights()).append(" đêm\n");
        if (!t.getHighlights().isEmpty()) sb.append("Điểm nổi bật: ").append(String.join("; ", t.getHighlights())).append('\n');
        sb.append("Lưu trú: ").append(t.getAccommodationType().label()).append('\n');
        sb.append("Phương tiện: ").append(t.getTransportModes().stream().map(m -> m.label()).sorted()
                .collect(Collectors.joining(", "))).append('\n');
        sb.append("Lịch trình:\n");
        for (TourItineraryDay day : t.getItineraryDays()) {
            sb.append("Ngày ").append(day.getDayNumber()).append(": ").append(day.getTitle()).append(". ")
                    .append(day.getDescription().replaceAll("\\s+", " ")).append('\n');
        }
        return sb.length() > MAX_CHARS ? sb.substring(0, MAX_CHARS) : sb.toString();
    }

    private static String place(Location l) {
        return l.getProvince() != null ? l.getProvince() + " (" + l.getCountry() + ")" : l.getCountry();
    }

    static String sha256(String text) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
