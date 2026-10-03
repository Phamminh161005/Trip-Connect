package com.tripconnect.backend.storage;

import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;

/**
 * Lưu trữ file. Code nghiệp vụ chỉ phụ thuộc interface này, không biết bên dưới là Cloudinary hay S3.
 */
public interface FileStorageService {

    /**
     * Upload file. File phải được kiểm tra bằng {@link FileValidator} trước khi gọi.
     *
     * @param folder thư mục logic, ví dụ "agents/15/documents"
     */
    StoredFile upload(MultipartFile file, String folder, FileVisibility visibility);

    void delete(String publicId, FileVisibility visibility);

    /** URL cố định cho file PUBLIC. */
    String publicUrl(String publicId, String format);

    /** URL tạm thời, tự hết hạn sau {@code ttl}, cho file PRIVATE. Chỉ gọi sau khi đã kiểm tra quyền xem. */
    String temporaryUrl(String publicId, String format, Duration ttl);
}
