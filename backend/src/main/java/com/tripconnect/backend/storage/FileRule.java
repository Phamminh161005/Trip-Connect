package com.tripconnect.backend.storage;

import java.util.Set;

/**
 * Bộ quy tắc kiểm tra file theo mục đích sử dụng: dung lượng tối đa + các loại file (MIME) được phép.
 */
public enum FileRule {

    IMAGE(5L * 1024 * 1024, Set.of("image/jpeg", "image/png", "image/webp"), "ảnh JPG, PNG hoặc WebP"),
    DOCUMENT(10L * 1024 * 1024, Set.of("application/pdf", "image/jpeg", "image/png"), "file PDF, JPG hoặc PNG"),
    PDF(10L * 1024 * 1024, Set.of("application/pdf"), "file PDF");

    private final long maxBytes;
    private final Set<String> allowedMimeTypes;
    private final String description;

    FileRule(long maxBytes, Set<String> allowedMimeTypes, String description) {
        this.maxBytes = maxBytes;
        this.allowedMimeTypes = allowedMimeTypes;
        this.description = description;
    }

    public long maxBytes() {
        return maxBytes;
    }

    public Set<String> allowedMimeTypes() {
        return allowedMimeTypes;
    }

    public String description() {
        return description;
    }

    public long maxMegabytes() {
        return maxBytes / (1024 * 1024);
    }
}
