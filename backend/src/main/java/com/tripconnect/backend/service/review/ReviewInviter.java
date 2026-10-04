package com.tripconnect.backend.service.review;

import com.tripconnect.backend.entity.Booking;
import com.tripconnect.backend.service.EmailTemplates;
import com.tripconnect.backend.service.NotificationEvents;
import com.tripconnect.backend.service.WebNotifications;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/** Mời khách đánh giá khi đơn vừa chuyển "Hoàn thành" (email + thông báo trên web). */
@Component
public class ReviewInviter {

    private final ApplicationEventPublisher eventPublisher;
    private final String frontendUrl;

    public ReviewInviter(ApplicationEventPublisher eventPublisher, @Value("${app.frontend-url}") String frontendUrl) {
        this.eventPublisher = eventPublisher;
        this.frontendUrl = frontendUrl.replaceAll("/+$", "");
    }

    public void invite(Booking booking) {
        var message = WebNotifications.reviewInvite(booking.getId(), booking.getTour().getTitle());
        eventPublisher.publishEvent(new NotificationEvents.UserWebEvent(booking.getCustomer().getId(), message));
        eventPublisher.publishEvent(new NotificationEvents.UserEmailEvent(booking.getContactEmail(),
                EmailTemplates.reviewInvite(booking.getCode(), booking.getTour().getTitle(), ReviewRules.WRITE_WINDOW_DAYS,
                        frontendUrl + message.link())));
    }
}
