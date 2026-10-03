package com.tripconnect.backend.storage;

/**
 * Kết quả sau khi upload. Trong DB lưu {@code publicId} + {@code format} (không lưu URL):
 * - File PUBLIC: từ publicId tạo được URL ở mọi kích thước (ảnh thu nhỏ, ảnh bìa...).
 * - File PRIVATE: URL phải tạo mới (có hạn dùng) mỗi lần xem.
 */
public record StoredFile(String publicId, String format, String mimeType, long sizeBytes) {
}
