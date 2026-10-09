package com.tripconnect.backend.ai.rag;

import com.tripconnect.backend.ai.LlmClient;
import com.tripconnect.backend.entity.KnowledgeChunk;
import com.tripconnect.backend.repository.KnowledgeChunkRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Kho kiến thức của trợ lý: các file Markdown trong resources/knowledge (chính sách, hướng dẫn).
 * Mỗi mục "## ..." là một đoạn; đoạn mới / đổi nội dung thì tạo lại véc-tơ, đoạn đã bỏ khỏi file thì xóa.
 */
@Slf4j
@Component
public class KnowledgeBase {

    private static final String LOCATION = "classpath:knowledge/*.md";

    private final KnowledgeChunkRepository repository;
    private final LlmClient llm;
    private final VectorIndex index;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;
    private final String model;

    public KnowledgeBase(KnowledgeChunkRepository repository, LlmClient llm, VectorIndex index,
                         TransactionTemplate transactionTemplate, Clock clock, @Value("${ai.gemini.embedding-model}") String model) {
        this.repository = repository;
        this.llm = llm;
        this.index = index;
        this.transactionTemplate = transactionTemplate;
        this.clock = clock;
        this.model = model;
    }

    /** Một đoạn đọc từ file. */
    public record Section(String source, String title, String content) {
    }

    /** @return số đoạn vừa tạo lại véc-tơ */
    public synchronized int reindex() {
        List<Section> sections = loadSections();
        Map<String, KnowledgeChunk> existing = new HashMap<>();
        repository.findAll().forEach(c -> existing.put(key(c.getSource(), c.getTitle()), c));

        List<Section> stale = new ArrayList<>();
        Map<Section, String> hashes = new HashMap<>();
        for (Section s : sections) {
            String hash = TourEmbeddingIndexer.sha256(model + "\n" + s.title() + "\n" + s.content());
            hashes.put(s, hash);
            KnowledgeChunk c = existing.get(key(s.source(), s.title()));
            if (c == null || !c.getContentHash().equals(hash)) stale.add(s);
        }
        if (!stale.isEmpty() && llm.isConfigured()) {
            List<float[]> vectors = llm.embed(stale.stream().map(s -> s.title() + "\n" + s.content()).toList(),
                    LlmClient.EmbedTask.DOCUMENT);
            LocalDateTime now = LocalDateTime.now(clock);
            List<KnowledgeChunk> rows = new ArrayList<>();
            for (int i = 0; i < stale.size(); i++) {
                Section s = stale.get(i);
                KnowledgeChunk c = existing.getOrDefault(key(s.source(), s.title()), new KnowledgeChunk());
                c.setSource(s.source());
                c.setTitle(s.title());
                c.setContent(s.content());
                c.setModel(model);
                c.setContentHash(hashes.get(s));
                c.setVector(vectors.get(i));
                c.setUpdatedAt(now);
                rows.add(c);
            }
            repository.saveAll(rows);
        }
        Set<String> current = new HashSet<>();
        sections.forEach(s -> current.add(key(s.source(), s.title())));
        List<KnowledgeChunk> removed = existing.values().stream().filter(c -> !current.contains(key(c.getSource(), c.getTitle()))).toList();
        if (!removed.isEmpty()) transactionTemplate.executeWithoutResult(status -> repository.deleteAllInBatch(removed));
        reloadIndex();
        return llm.isConfigured() ? stale.size() : 0;
    }

    public void reloadIndex() {
        index.replaceKnowledge(repository.findAll().stream()
                .filter(c -> model.equals(c.getModel()))
                .map(c -> new VectorIndex.Chunk(c.getId(), c.getSource(), c.getTitle(), c.getContent(), c.getVector()))
                .toList());
    }

    List<Section> loadSections() {
        List<Section> sections = new ArrayList<>();
        try {
            Resource[] files = new PathMatchingResourcePatternResolver().getResources(LOCATION);
            Arrays.sort(files, Comparator.comparing(r -> Objects.requireNonNullElse(r.getFilename(), "")));
            for (Resource file : files) {
                try (InputStream in = file.getInputStream()) {
                    sections.addAll(split(file.getFilename(), new String(in.readAllBytes(), StandardCharsets.UTF_8)));
                }
            }
        } catch (IOException e) {
            log.warn("Không đọc được kho kiến thức: {}", e.getMessage());
        }
        return sections;
    }

    /** "# Tiêu đề tài liệu" + các mục "## ..." -> mỗi mục một đoạn, tiêu đề dạng "Tài liệu › Mục". */
    static List<Section> split(String source, String markdown) {
        String docTitle = source;
        List<Section> sections = new ArrayList<>();
        String current = null;
        StringBuilder body = new StringBuilder();
        for (String line : markdown.replace("\r", "").split("\n")) {
            if (line.startsWith("# ")) {
                docTitle = line.substring(2).trim();
            } else if (line.startsWith("## ")) {
                flush(sections, source, docTitle, current, body);
                current = line.substring(3).trim();
                body.setLength(0);
            } else {
                body.append(line).append('\n');
            }
        }
        flush(sections, source, docTitle, current, body);
        return sections;
    }

    private static void flush(List<Section> sections, String source, String docTitle, String section, StringBuilder body) {
        String content = body.toString().trim();
        if (content.isEmpty()) return;
        sections.add(new Section(source, section == null ? docTitle : docTitle + " › " + section, content));
    }

    private static String key(String source, String title) {
        return source + "\u0000" + title;
    }
}
