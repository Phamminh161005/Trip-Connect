package com.tripconnect.backend.service.tour;

import com.tripconnect.backend.dto.TemporaryUrlResponse;
import com.tripconnect.backend.dto.tour.TourResponses;
import com.tripconnect.backend.entity.Tour;
import com.tripconnect.backend.enums.AgentStatus;
import com.tripconnect.backend.enums.TourStatus;
import com.tripconnect.backend.exception.ResourceNotFoundException;
import com.tripconnect.backend.repository.AgentProfileRepository;
import com.tripconnect.backend.repository.TourRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Tour dành cho khách (không cần đăng nhập): chỉ tour đang công khai của đơn vị còn hoạt động. */
@Service
@RequiredArgsConstructor
public class PublicTourService {

    private final TourRepository tourRepository;
    private final AgentProfileRepository agentProfileRepository;
    private final TourAssembler assembler;
    private final TourMediaService mediaService;

    @Transactional(readOnly = true)
    public TourResponses.Detail get(Long tourId) {
        return assembler.toPublicDetail(requireVisible(tourId));
    }

    @Transactional(readOnly = true)
    public TemporaryUrlResponse itineraryFileUrl(Long tourId) {
        return mediaService.itineraryFileUrl(requireVisible(tourId));
    }

    private Tour requireVisible(Long tourId) {
        // Tour ẩn / chưa duyệt trả 404 giống tour không tồn tại
        Tour tour = tourRepository.findById(tourId)
                .filter(t -> t.getStatus() == TourStatus.PUBLISHED)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tour"));
        if (!tour.isPlatformTour()) {
            boolean agentActive = tour.getAgent().isActive() && agentProfileRepository.findByUserId(tour.getAgent().getId())
                    .map(p -> p.getStatus() == AgentStatus.APPROVED).orElse(false);
            if (!agentActive) {
                throw new ResourceNotFoundException("Không tìm thấy tour");
            }
        }
        return tour;
    }
}
