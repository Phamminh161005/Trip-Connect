package com.tripconnect.backend.controller;

import com.tripconnect.backend.dto.tour.*;
import com.tripconnect.backend.security.AuthenticatedUser;
import com.tripconnect.backend.service.tour.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Các API quản lý tour dùng chung cho Agent (/api/agent/tours) và Admin (/api/admin/tours, tour của TripConnect).
 * Quyền sở hữu tour được kiểm tra trong service (TourAccess) theo người đang đăng nhập.
 */
public abstract class TourManagementEndpoints {

    protected final TourService tourService;
    protected final TourMediaService mediaService;
    protected final TourDepartureService departureService;

    protected TourManagementEndpoints(TourService tourService, TourMediaService mediaService,
                                      TourDepartureService departureService) {
        this.tourService = tourService;
        this.mediaService = mediaService;
        this.departureService = departureService;
    }

    @PostMapping
    public ResponseEntity<TourResponses.Detail> create(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                                       @Valid @RequestBody TourContentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tourService.create(TourActor.of(currentUser), request));
    }

    /** Agent sửa tour đang công khai / tạm ẩn -> tour về Nháp, phải gửi duyệt lại. */
    @PutMapping("/{id}")
    public TourResponses.Detail update(@AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable Long id,
                                       @Valid @RequestBody TourContentRequest request) {
        return tourService.update(TourActor.of(currentUser), id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable Long id) {
        tourService.delete(TourActor.of(currentUser), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/hide")
    public TourResponses.Detail hide(@AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable Long id) {
        return tourService.hide(TourActor.of(currentUser), id);
    }

    @PostMapping("/{id}/unhide")
    public TourResponses.Detail unhide(@AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable Long id) {
        return tourService.unhide(TourActor.of(currentUser), id);
    }

    // ===================== Ảnh =====================

    @PostMapping(value = "/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TourResponses.Image> uploadImage(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                                           @PathVariable Long id,
                                                           @RequestParam("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mediaService.uploadImage(TourActor.of(currentUser), id, file));
    }

    @DeleteMapping("/{id}/images/{imageId}")
    public ResponseEntity<Void> deleteImage(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                            @PathVariable Long id, @PathVariable Long imageId) {
        mediaService.deleteImage(TourActor.of(currentUser), id, imageId);
        return ResponseEntity.noContent().build();
    }

    /** Ảnh đầu tiên trong danh sách là ảnh bìa. */
    @PutMapping("/{id}/images/order")
    public List<TourResponses.Image> reorderImages(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                                   @PathVariable Long id,
                                                   @Valid @RequestBody ReorderImagesRequest request) {
        return mediaService.reorderImages(TourActor.of(currentUser), id, request.getImageIds());
    }

    // ===================== File chương trình tour (PDF) =====================

    @PutMapping(value = "/{id}/itinerary-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public TourResponses.ItineraryFile uploadItineraryFile(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                                           @PathVariable Long id,
                                                           @RequestParam("file") MultipartFile file) {
        return mediaService.uploadItineraryFile(TourActor.of(currentUser), id, file);
    }

    @DeleteMapping("/{id}/itinerary-file")
    public ResponseEntity<Void> deleteItineraryFile(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                                    @PathVariable Long id) {
        mediaService.deleteItineraryFile(TourActor.of(currentUser), id);
        return ResponseEntity.noContent().build();
    }

    // ===================== Lịch khởi hành =====================

    @PostMapping("/{id}/departures")
    public ResponseEntity<TourResponses.Departure> addDeparture(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                                                @PathVariable Long id,
                                                                @Valid @RequestBody DepartureRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(departureService.add(TourActor.of(currentUser), id, request));
    }

    @PutMapping("/{id}/departures/{departureId}")
    public TourResponses.Departure updateDeparture(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                                   @PathVariable Long id, @PathVariable Long departureId,
                                                   @Valid @RequestBody DepartureRequest request) {
        return departureService.update(TourActor.of(currentUser), id, departureId, request);
    }

    @PostMapping("/{id}/departures/{departureId}/close")
    public TourResponses.Departure closeDeparture(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                                  @PathVariable Long id, @PathVariable Long departureId) {
        return departureService.close(TourActor.of(currentUser), id, departureId);
    }

    @PostMapping("/{id}/departures/{departureId}/reopen")
    public TourResponses.Departure reopenDeparture(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                                   @PathVariable Long id, @PathVariable Long departureId) {
        return departureService.reopen(TourActor.of(currentUser), id, departureId);
    }

    @PostMapping("/{id}/departures/{departureId}/cancel")
    public TourResponses.Departure cancelDeparture(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                                   @PathVariable Long id, @PathVariable Long departureId,
                                                   @Valid @RequestBody ReasonRequest request) {
        return departureService.cancel(TourActor.of(currentUser), id, departureId, request.getReason());
    }

    @DeleteMapping("/{id}/departures/{departureId}")
    public ResponseEntity<Void> deleteDeparture(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                                @PathVariable Long id, @PathVariable Long departureId) {
        departureService.delete(TourActor.of(currentUser), id, departureId);
        return ResponseEntity.noContent().build();
    }
}
