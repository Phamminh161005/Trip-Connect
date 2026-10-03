package com.tripconnect.backend.storage;

import com.tripconnect.backend.exception.InvalidFileException;
import org.apache.tika.Tika;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Kiểm tra file trước khi lưu: không rỗng, không vượt dung lượng, và loại file THẬT nằm trong danh sách cho phép.
 * Loại file được nhận diện qua nội dung (magic bytes) chứ không qua đuôi file hay Content-Type do client gửi,
 * vì hai thông tin đó client tự đặt được (ví dụ đổi "virus.exe" thành "cccd.pdf").
 */
@Component
public class FileValidator {

    private final Tika tika = new Tika();

    /** @return MIME type thật của file, ví dụ "application/pdf". */
    public String validate(MultipartFile file, FileRule rule) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("File không được để trống");
        }
        if (file.getSize() > rule.maxBytes()) {
            throw new InvalidFileException("File vượt quá dung lượng cho phép (" + rule.maxMegabytes() + " MB)");
        }

        String detectedType = detectMimeType(file);
        if (!rule.allowedMimeTypes().contains(detectedType)) {
            throw new InvalidFileException("Định dạng file không hợp lệ, chỉ chấp nhận " + rule.description());
        }
        return detectedType;
    }

    private String detectMimeType(MultipartFile file) {
        // Cố ý KHÔNG truyền tên file cho Tika để nó chỉ dựa vào nội dung
        try (InputStream in = new BufferedInputStream(file.getInputStream())) {
            return tika.detect(in);
        } catch (IOException e) {
            throw new InvalidFileException("Không đọc được file");
        }
    }
}
