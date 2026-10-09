package com.tripconnect.backend.ai.rag;

import com.tripconnect.backend.ai.VectorMath;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Véc-tơ của tour đang bán và của kho kiến thức, giữ trong bộ nhớ để tìm nhanh (vài trăm tour: vài mili giây).
 * Nguồn gốc là bảng tour_embeddings / knowledge_chunks; các bộ tạo véc-tơ cập nhật chỉ mục sau khi lưu DB.
 */
@Component
public class VectorIndex {

    public record Chunk(Long id, String source, String title, String content, float[] vector) {
    }

    public record Hit<T>(T item, double score) {
    }

    private final Map<Long, float[]> tours = new ConcurrentHashMap<>();
    private volatile List<Chunk> knowledge = List.of();

    // ----- Tour -----

    public void replaceTours(Map<Long, float[]> vectors) {
        tours.keySet().retainAll(vectors.keySet());
        tours.putAll(vectors);
    }

    public Optional<float[]> tourVector(Long tourId) {
        return Optional.ofNullable(tours.get(tourId));
    }

    public int tourCount() {
        return tours.size();
    }

    /**
     * Tour giống câu hỏi / véc-tơ sở thích nhất.
     *
     * @param among  chỉ xét các tour này (null = mọi tour có véc-tơ)
     * @param minScore bỏ kết quả quá xa (độ giống cosine)
     */
    public List<Hit<Long>> nearestTours(float[] query, Collection<Long> among, int limit, double minScore) {
        Iterable<Map.Entry<Long, float[]>> source = among == null ? tours.entrySet()
                : among.stream().filter(tours::containsKey).map(id -> Map.entry(id, tours.get(id))).toList();
        List<Hit<Long>> hits = new ArrayList<>();
        for (Map.Entry<Long, float[]> e : source) {
            double score = VectorMath.dot(query, e.getValue());
            if (score >= minScore) hits.add(new Hit<>(e.getKey(), score));
        }
        hits.sort(Comparator.comparingDouble((Hit<Long> h) -> h.score()).reversed());
        return hits.size() > limit ? hits.subList(0, limit) : hits;
    }

    // ----- Kho kiến thức -----

    public void replaceKnowledge(List<Chunk> chunks) {
        knowledge = List.copyOf(chunks);
    }

    public int knowledgeCount() {
        return knowledge.size();
    }

    public List<Hit<Chunk>> nearestKnowledge(float[] query, int limit, double minScore) {
        List<Hit<Chunk>> hits = new ArrayList<>();
        for (Chunk c : knowledge) {
            double score = VectorMath.dot(query, c.vector());
            if (score >= minScore) hits.add(new Hit<>(c, score));
        }
        hits.sort(Comparator.comparingDouble((Hit<Chunk> h) -> h.score()).reversed());
        return hits.size() > limit ? hits.subList(0, limit) : hits;
    }
}
