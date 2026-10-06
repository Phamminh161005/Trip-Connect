package com.tripconnect.backend.service.customrequest;

import com.tripconnect.backend.entity.CustomRequest;
import com.tripconnect.backend.enums.CancelledBy;
import com.tripconnect.backend.enums.CustomRequestStatus;
import com.tripconnect.backend.repository.CustomRequestRepository;
import com.tripconnect.backend.service.NotificationEvents;
import com.tripconnect.backend.service.WebNotifications;
import com.tripconnect.backend.service.booking.BookingCancellation;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Đơn tour riêng bị hủy (khách hủy, quá hạn thanh toán, Agent hủy chuyến, Admin hủy) -> yêu cầu đã chốt kết thúc theo.
 * Chạy đồng bộ trong transaction hủy đơn. Khách đã được báo qua email hủy đơn, chỉ cần báo thêm Agent.
 */
@Component
@RequiredArgsConstructor
public class PrivateBookingListener {

    private final CustomRequestRepository requestRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    @EventListener
    public void onCancelled(BookingCancellation.PrivateBookingCancelled event) {
        CustomRequest request = requestRepository.findByIdForUpdate(event.customRequestId()).orElse(null);
        if (request == null || request.getStatus() != CustomRequestStatus.AGREED) return;
        request.setStatus(event.by() == CancelledBy.CUSTOMER ? CustomRequestStatus.CANCELLED : CustomRequestStatus.CLOSED);
        request.setClosedAt(LocalDateTime.now(clock));
        request.setClosedReason(event.reason());
        if (event.by() != CancelledBy.AGENT && request.getAgent() != null) {
            eventPublisher.publishEvent(new NotificationEvents.UserWebEvent(request.getAgent().getId(),
                    WebNotifications.customRequestCancelled(request.getId(), request.getCode(), event.reason())));
        }
    }
}
