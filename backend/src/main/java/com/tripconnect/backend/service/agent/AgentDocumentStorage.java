package com.tripconnect.backend.service.agent;

import com.tripconnect.backend.dto.TemporaryUrlResponse;
import com.tripconnect.backend.entity.AgentDocument;
import com.tripconnect.backend.entity.AgentProfile;
import com.tripconnect.backend.entity.AgentProfileChangeRequest;
import com.tripconnect.backend.enums.AgentDocumentStatus;
import com.tripconnect.backend.enums.AgentDocumentType;
import com.tripconnect.backend.repository.AgentDocumentRepository;
import com.tripconnect.backend.storage.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Lưu / xóa / xem giấy tờ Agent. Giấy tờ luôn là file PRIVATE.
 * Phải được gọi trong transaction (để dọn file trên Cloudinary khớp với DB).
 */
@Component
@RequiredArgsConstructor
public class AgentDocumentStorage {

    /** Link xem giấy tờ chỉ sống 5 phút. */
    public static final Duration VIEW_URL_TTL = Duration.ofMinutes(5);

    private final AgentDocumentRepository documentRepository;
    private final FileStorageService fileStorageService;
    private final FileValidator fileValidator;
    private final TransactionalFileCleanup fileCleanup;

    /** Kiểm tra file (định dạng, dung lượng). Gọi cho TẤT CẢ file trước khi upload file nào. */
    public String validate(MultipartFile file) {
        return fileValidator.validate(file, FileRule.DOCUMENT);
    }

    /** Upload file đã được {@link #validate} và lưu bản ghi giấy tờ. */
    public AgentDocument store(AgentProfile profile, AgentProfileChangeRequest changeRequest,
                               AgentDocumentType type, AgentDocumentStatus status,
                               MultipartFile file, String detectedMimeType) {
        String folder = changeRequest == null
                ? "agents/" + profile.getId() + "/documents"
                : "agents/" + profile.getId() + "/change-requests/" + changeRequest.getId();

        StoredFile stored = fileStorageService.upload(file, folder, FileVisibility.PRIVATE);
        // Nếu phần còn lại của transaction lỗi -> xóa file vừa upload, không để lại file rác
        fileCleanup.deleteOnRollback(stored.publicId(), FileVisibility.PRIVATE);

        AgentDocument document = new AgentDocument();
        document.setAgentProfile(profile);
        document.setChangeRequest(changeRequest);
        document.setType(type);
        document.setStatus(status);
        document.setPublicId(stored.publicId());
        document.setFormat(stored.format());
        document.setMimeType(detectedMimeType);
        document.setSizeBytes(stored.sizeBytes());
        document.setOriginalFilename(safeFilename(file.getOriginalFilename()));
        return documentRepository.save(document);
    }

    /** Xóa hẳn bản ghi + file (file chỉ bị xóa sau khi DB commit thành công). */
    public void deletePermanently(AgentDocument document) {
        documentRepository.delete(document);
        fileCleanup.deleteAfterCommit(document.getPublicId(), FileVisibility.PRIVATE);
    }

    public void deletePermanently(List<AgentDocument> documents) {
        documents.forEach(this::deletePermanently);
    }

    public TemporaryUrlResponse temporaryUrl(AgentDocument document) {
        String url = fileStorageService.temporaryUrl(document.getPublicId(), document.getFormat(), VIEW_URL_TTL);
        return new TemporaryUrlResponse(url, LocalDateTime.now().plus(VIEW_URL_TTL));
    }

    /** Bỏ phần đường dẫn (một số trình duyệt gửi kèm) và giới hạn độ dài. */
    private static String safeFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) return null;
        String name = originalFilename.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1).trim();
        return name.length() > 255 ? name.substring(name.length() - 255) : name;
    }
}
