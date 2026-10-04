// Khớp NotificationResponses / NotificationType của Backend

export type NotificationType =
  | "BOOKING_PAID"
  | "BOOKING_CANCELLED"
  | "REFUND_COMPLETED"
  | "TRIP_REMINDER"
  | "NEW_BOOKING"
  | "TOUR_APPROVED"
  | "TOUR_NEEDS_REVISION"
  | "TOUR_SUSPENDED"
  | "TOUR_UNSUSPENDED"
  | "AGENT_PROFILE_APPROVED"
  | "AGENT_PROFILE_NEEDS_REVISION"
  | "CHANGE_REQUEST_APPROVED"
  | "CHANGE_REQUEST_REJECTED"
  | "DEPARTURE_REMINDER"
  | "LOW_BOOKINGS"
  | "AGENT_PROFILE_SUBMITTED"
  | "CHANGE_REQUEST_SUBMITTED"
  | "TOUR_SUBMITTED"
  | "MANUAL_REFUND_NEEDED";

export interface NotificationItem {
  id: number;
  type: NotificationType;
  title: string;
  body: string | null;
  link: string | null;
  read: boolean;
  createdAt: string;
}

/** nextCursor = null khi đã hết */
export interface NotificationPage {
  items: NotificationItem[];
  nextCursor: number | null;
}
