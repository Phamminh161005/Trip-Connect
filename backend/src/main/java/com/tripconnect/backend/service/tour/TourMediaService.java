package com.tripconnect.backend.service.tour;

import com.tripconnect.backend.dto.TemporaryUrlResponse;
import com.tripconnect.backend.dto.tour.TourResponses;
import com.tripconnect.backend.entity.Tour;
import com.tripconnect.backend.entity.TourImage;
import com.tripconnect.backend.enums.TourStatus;
import com.tripconnect.backend.exception.ResourceNotFoundException;
import com.tripconnect.backend.repository.TourImageRepository;
import com.tripconnect.backend.storage.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Ảnh tour (PUBLIC) và file chương trình tour PDF (PRIVATE, xem qua link tạm thời).
 * Thêm nội dung mới (ảnh, file) vào tour đang bán -> phải duyệt lại; chỉ sắp xếp lại / bớt đi thì không.
 */
@Service
@RequiredArgsConstructor
public class TourMediaService {

    private static final Duration FILE_URL_TTL = Duration.ofMinutes(5);

    private final TourImageRepository imageRepository;
    private final TourAccess access;
    private final TourAssembler assembler;
    private final FileStorageService fileStorageService;
    private final FileValidator fileValidator;
    private final TransactionalFileCleanup fileCleanup;

    // ===================== Ảnh =====================

    @Transactional
    public TourResponses.Image uploadImage(TourActor actor, Long tourId, MultipartFile file) {
        // Khóa dòng tour: 2 lần upload cùng lúc không thể cùng vượt qua bước đếm số ảnh
        Tour tour = access.requireManageableForUpdate(tourId, actor);
        access.requireContentEditable(tour);
        if (imageRepository.countByTourId(tourId) >= TourRules.MAX_IMAGES) {
            throw new IllegalStateException("Mỗi tour có tối đa " + TourRules.MAX_IMAGES + " ảnh");
        }
        fileValidator.validate(file, FileRule.IMAGE);

        StoredFile stored = fileStorageService.upload(file, "tours/" + tourId + "/images", FileVisibility.PUBLIC);
        fileCleanup.deleteOnRollback(stored.publicId(), FileVisibility.PUBLIC);

        TourImage image = new TourImage();
        image.setTour(tour);
        image.setPublicId(stored.publicId());
        image.setFormat(stored.format());
        image.setSizeBytes(stored.sizeBytes());
        image.setSortOrder(imageRepository.findMaxSortOrder(tourId) + 1);
        imageRepository.save(image);

        access.onContentChanged(tour, actor);
        return assembler.toImage(image);
    }

    /** Xóa ảnh không cần duyệt lại, nhưng tour đang bán / đang ẩn phải còn đủ số ảnh tối thiểu. */
    @Transactional
    public void deleteImage(TourActor actor, Long tourId, Long imageId) {
        Tour tour = access.requireManageableForUpdate(tourId, actor);
        access.requireContentEditable(tour);
        TourImage image = imageRepository.findByIdAndTourId(imageId, tourId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ảnh"));

        boolean live = tour.getStatus() == TourStatus.PUBLISHED || tour.getStatus() == TourStatus.HIDDEN;
        if (live && imageRepository.countByTourId(tourId) <= TourRules.MIN_IMAGES) {
            throw new IllegalStateException("Tour đang bán phải có ít nhất " + TourRules.MIN_IMAGES
                    + " ảnh. Hãy thêm ảnh mới trước khi xóa ảnh này");
        }
        imageRepository.delete(image);
        fileCleanup.deleteAfterCommit(image.getPublicId(), FileVisibility.PUBLIC);
    }

    /** Sắp xếp lại ảnh (ảnh đầu tiên là ảnh bìa). Chỉ đổi thứ tự ảnh đã duyệt nên không cần duyệt lại. */
    @Transactional
    public List<TourResponses.Image> reorderImages(TourActor actor, Long tourId, List<Long> imageIds) {
        Tour tour = access.requireManageableForUpdate(tourId, actor);
        access.requireContentEditable(tour);

        Map<Long, TourImage> images = imageRepository.findByTourIdOrderBySortOrderAscIdAsc(tourId).stream()
                .collect(Collectors.toMap(TourImage::getId, Function.identity()));
        if (imageIds.size() != images.size() || !new HashSet<>(imageIds).equals(images.keySet())) {
            throw new IllegalArgumentException("Danh sách ảnh không khớp với ảnh hiện có của tour, vui lòng tải lại trang");
        }
        for (int i = 0; i < imageIds.size(); i++) {
            images.get(imageIds.get(i)).setSortOrder(i);
        }
        return imageIds.stream().map(id -> assembler.toImage(images.get(id))).toList();
    }

    // ===================== File chương trình tour (PDF) =====================

    @Transactional
    public TourResponses.ItineraryFile uploadItineraryFile(TourActor actor, Long tourId, MultipartFile file) {
        Tour tour = access.requireManageableForUpdate(tourId, actor);
        access.requireContentEditable(tour);
        fileValidator.validate(file, FileRule.PDF);

        StoredFile stored = fileStorageService.upload(file, "tours/" + tourId + "/program", FileVisibility.PRIVATE);
        fileCleanup.deleteOnRollback(stored.publicId(), FileVisibility.PRIVATE);
        if (tour.hasItineraryFile()) {
            fileCleanup.deleteAfterCommit(tour.getItineraryFilePublicId(), FileVisibility.PRIVATE);
        }

        tour.setItineraryFilePublicId(stored.publicId());
        tour.setItineraryFileFormat(stored.format());
        tour.setItineraryFileSize(stored.sizeBytes());
        tour.setItineraryFileName(safeFilename(file.getOriginalFilename()));
        access.onContentChanged(tour, actor);
        return new TourResponses.ItineraryFile(tour.getItineraryFileName(), tour.getItineraryFileSize());
    }

    @Transactional
    public void deleteItineraryFile(TourActor actor, Long tourId) {
        Tour tour = access.requireManageableForUpdate(tourId, actor);
        access.requireContentEditable(tour);
        if (!tour.hasItineraryFile()) {
            throw new ResourceNotFoundException("Tour chưa có file chương trình");
        }
        fileCleanup.deleteAfterCommit(tour.getItineraryFilePublicId(), FileVisibility.PRIVATE);
        tour.setItineraryFilePublicId(null);
        tour.setItineraryFileFormat(null);
        tour.setItineraryFileSize(null);
        tour.setItineraryFileName(null);
    }

    /** Link xem file cho người quản lý tour (ở mọi trạng thái). */
    @Transactional(readOnly = true)
    public TemporaryUrlResponse itineraryFileUrlForOwner(TourActor actor, Long tourId) {
        return itineraryFileUrl(access.requireManageable(tourId, actor));
    }

    public TemporaryUrlResponse itineraryFileUrl(Tour tour) {
        if (!tour.hasItineraryFile()) {
            throw new ResourceNotFoundException("Tour chưa có file chương trình");
        }
        String url = fileStorageService.temporaryUrl(tour.getItineraryFilePublicId(), tour.getItineraryFileFormat(),
                FILE_URL_TTL);
        return new TemporaryUrlResponse(url, LocalDateTime.now().plus(FILE_URL_TTL));
    }

    /** Bỏ phần đường dẫn (một số trình duyệt gửi kèm) và giới hạn độ dài. */
    private static String safeFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) return "chuong-trinh-tour.pdf";
        String name = originalFilename.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1).trim();
        return name.length() > 255 ? name.substring(name.length() - 255) : name;
    }
}
