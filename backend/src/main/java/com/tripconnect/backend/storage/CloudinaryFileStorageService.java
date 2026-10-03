package com.tripconnect.backend.storage;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.tripconnect.backend.exception.FileStorageException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CloudinaryFileStorageService implements FileStorageService {

    private static final String ROOT_FOLDER = "tripconnect/";
    // Cloudinary coi cả ảnh lẫn PDF là resource_type "image" (PDF xem được từng trang như ảnh)
    private static final String RESOURCE_TYPE = "image";

    private final Cloudinary cloudinary;

    @Override
    public StoredFile upload(MultipartFile file, String folder, FileVisibility visibility) {
        try {
            Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                    "folder", ROOT_FOLDER + folder,
                    "type", deliveryType(visibility),
                    "resource_type", RESOURCE_TYPE,
                    "unique_filename", true,
                    "overwrite", false
            ));
            return new StoredFile(
                    (String) result.get("public_id"),
                    (String) result.get("format"),
                    file.getContentType(),
                    ((Number) result.get("bytes")).longValue()
            );
        } catch (Exception e) {
            log.error("Upload file lên Cloudinary thất bại (folder={}): {}", folder, e.getMessage());
            throw new FileStorageException("Không thể lưu file, vui lòng thử lại sau", e);
        }
    }

    @Override
    public void delete(String publicId, FileVisibility visibility) {
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.asMap(
                    "type", deliveryType(visibility),
                    "resource_type", RESOURCE_TYPE,
                    "invalidate", true // xóa luôn bản cache trên CDN
            ));
        } catch (Exception e) {
            // Không chặn nghiệp vụ vì file rác trên Cloudinary; chỉ ghi log để dọn sau
            log.warn("Xóa file trên Cloudinary thất bại (publicId={}): {}", publicId, e.getMessage());
        }
    }

    @Override
    public String publicUrl(String publicId, String format) {
        return cloudinary.url()
                .secure(true)
                .resourceType(RESOURCE_TYPE)
                .type(deliveryType(FileVisibility.PUBLIC))
                .format(format)
                .generate(publicId);
    }

    @Override
    public String temporaryUrl(String publicId, String format, Duration ttl) {
        try {
            long expiresAt = Instant.now().plus(ttl).getEpochSecond();
            return cloudinary.privateDownload(publicId, format, ObjectUtils.asMap(
                    "type", deliveryType(FileVisibility.PRIVATE),
                    "resource_type", RESOURCE_TYPE,
                    "expires_at", expiresAt
            ));
        } catch (Exception e) {
            log.error("Tạo link tạm thời thất bại (publicId={}): {}", publicId, e.getMessage());
            throw new FileStorageException("Không thể tạo link xem file, vui lòng thử lại sau", e);
        }
    }

    /** "upload" = công khai; "authenticated" = link gốc bị chặn, phải có chữ ký mới xem được. */
    private static String deliveryType(FileVisibility visibility) {
        return visibility == FileVisibility.PRIVATE ? "authenticated" : "upload";
    }
}
