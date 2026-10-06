package com.tripconnect.backend.service.customrequest;

import com.tripconnect.backend.dto.customrequest.CustomRequestResponses;
import com.tripconnect.backend.entity.Booking;
import com.tripconnect.backend.entity.CustomRequest;
import com.tripconnect.backend.entity.CustomRequestAssignment;
import com.tripconnect.backend.entity.TourDeparture;
import com.tripconnect.backend.enums.CancelledBy;
import com.tripconnect.backend.enums.CustomRequestStatus;
import com.tripconnect.backend.enums.DepartureStatus;
import com.tripconnect.backend.exception.ResourceNotFoundException;
import com.tripconnect.backend.repository.BookingRepository;
import com.tripconnect.backend.repository.CustomRequestAssignmentRepository;
import com.tripconnect.backend.repository.TourDepartureRepository;
import com.tripconnect.backend.service.booking.BookingCancellation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;

/** Agent hủy chuyến tour riêng đã chốt: khách được hoàn 100% số đã trả, yêu cầu đóng theo (PrivateBookingListener). */
@Service
@RequiredArgsConstructor
public class PrivateTripService {

    private final CustomRequestAssignmentRepository assignmentRepository;
    private final BookingRepository bookingRepository;
    private final TourDepartureRepository departureRepository;
    private final BookingCancellation bookingCancellation;
    private final CustomRequestAssembler assembler;
    private final Clock clock;

    @Transactional
    public CustomRequestResponses.Detail cancelTrip(Long agentId, Long requestId, String reason) {
        if (reason == null || reason.isBlank()) throw new IllegalArgumentException("Vui lòng nhập lý do hủy chuyến");
        CustomRequestAssignment mine = assignmentRepository.findFirstByRequestIdAndAgentIdOrderByIdDesc(requestId, agentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu"));
        CustomRequest request = mine.getRequest();
        if (request.getStatus() != CustomRequestStatus.AGREED || request.getAgent() == null
                || !request.getAgent().getId().equals(agentId)) {
            throw new IllegalStateException("Chỉ hủy được chuyến đi đã chốt do bạn tổ chức");
        }
        Booking booking = bookingRepository.findByCustomRequestId(requestId).stream()
                .filter(b -> BookingCancellation.ACTIVE.contains(b.getStatus())).findFirst()
                .orElseThrow(() -> new IllegalStateException("Không có đơn đặt tour còn hiệu lực"));
        // Khóa lịch trước rồi mới tới đơn (cùng thứ tự với Hủy chuyến của tour thường)
        TourDeparture departure = departureRepository.findByIdForUpdate(booking.getDeparture().getId()).orElseThrow();
        if (!departure.getStartDate().isAfter(LocalDate.now(clock))) {
            throw new IllegalStateException("Chuyến đi đã tới ngày khởi hành nên không thể hủy trên hệ thống");
        }
        departure.setStatus(DepartureStatus.CANCELLED);
        departure.setCancelReason(reason.trim());
        bookingCancellation.cancelAllForDeparture(departure.getId(), CancelledBy.AGENT, reason.trim());
        return assembler.toDetail(request, CustomRequestAssembler.Viewer.AGENT, mine, false, false);
    }
}
