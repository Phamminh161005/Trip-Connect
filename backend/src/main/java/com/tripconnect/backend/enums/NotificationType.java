package com.tripconnect.backend.enums;

/** Loại thông báo trên web — Frontend dựa vào đây để chọn biểu tượng. */
public enum NotificationType {
    // Khách
    BOOKING_PAID,
    BOOKING_CANCELLED,
    REFUND_COMPLETED,
    TRIP_REMINDER,
    REVIEW_INVITE,
    REVIEW_REPLIED,
    REVIEW_HIDDEN,
    REVIEW_UNHIDDEN,
    // Agent
    NEW_BOOKING,
    TOUR_APPROVED,
    TOUR_NEEDS_REVISION,
    TOUR_SUSPENDED,
    TOUR_UNSUSPENDED,
    AGENT_PROFILE_APPROVED,
    AGENT_PROFILE_NEEDS_REVISION,
    CHANGE_REQUEST_APPROVED,
    CHANGE_REQUEST_REJECTED,
    // Đơn vị tổ chức (Agent, hoặc Admin với tour của TripConnect)
    NEW_REVIEW,
    TOUR_REVIEW_HIDDEN,
    TOUR_REVIEW_UNHIDDEN,
    DEPARTURE_REMINDER,
    LOW_BOOKINGS,
    // Admin
    AGENT_PROFILE_SUBMITTED,
    CHANGE_REQUEST_SUBMITTED,
    TOUR_SUBMITTED,
    MANUAL_REFUND_NEEDED
}
