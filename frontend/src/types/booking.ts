// Khớp các DTO đặt tour của Backend (BookingResponses, CreateBookingRequest)

export type BookingStatus = "PENDING_PAYMENT" | "PAID" | "COMPLETED" | "CANCELLED";
export type RefundStatus = "NONE" | "PENDING" | "REFUNDED" | "MANUAL_REQUIRED";
export type PassengerType = "ADULT" | "CHILD" | "INFANT";
export type CancelledBy = "CUSTOMER" | "AGENT" | "ADMIN" | "SYSTEM";
export type PaymentStatus = "PENDING" | "SUCCESS" | "FAILED" | "EXPIRED";
export type RefundRecordStatus = "PENDING" | "SUCCESS" | "MANUAL_REQUIRED" | "MANUAL_DONE";

export interface BookingSummary {
  id: number;
  code: string;
  status: BookingStatus;
  refundStatus: RefundStatus;
  tourId: number;
  tourTitle: string;
  coverImageUrl: string | null;
  startDate: string;
  endDate: string;
  adults: number;
  children: number;
  infants: number;
  totalAmount: number;
  refundAmount: number;
  customerName: string;
  customerEmail: string;
  holdExpiresAt: string;
  createdAt: string;
}

export interface BookingPassenger {
  id: number;
  fullName: string;
  dateOfBirth: string;
  type: PassengerType;
  passportNumber: string | null;
}

export interface PaymentView {
  id: number;
  txnRef: string;
  amount: number;
  status: PaymentStatus;
  bankCode: string | null;
  transactionNo: string | null;
  responseCode: string | null;
  createdAt: string;
}

export interface RefundView {
  id: number;
  amount: number;
  status: RefundRecordStatus;
  reason: string;
  message: string | null;
  processedAt: string | null;
  createdAt: string;
}

export interface BookingDetail {
  id: number;
  code: string;
  status: BookingStatus;
  tourId: number;
  tourTitle: string;
  coverImageUrl: string | null;
  international: boolean;
  providerName: string;
  departureId: number;
  startDate: string;
  endDate: string;
  meetingPoint: string;
  meetingTime: string;
  adults: number;
  children: number;
  infants: number;
  adultPrice: number;
  childPrice: number;
  totalAmount: number;
  /** Chỉ có khi Agent / Admin xem */
  commissionRate: number | null;
  commissionAmount: number | null;
  contactName: string;
  contactPhone: string;
  contactEmail: string;
  note: string | null;
  passengers: BookingPassenger[];
  /** Khách sửa được danh sách hành khách tới hết ngày này */
  passengerListDeadline: string;
  canEditPassengers: boolean;
  refundPolicy: { fullRefundDays: number; partialRefundDays: number; partialRefundPercent: number };
  holdExpiresAt: string;
  paidAt: string | null;
  completedAt: string | null;
  cancelledAt: string | null;
  cancelledBy: CancelledBy | null;
  cancelReason: string | null;
  refundAmount: number;
  refundStatus: RefundStatus;
  payments: PaymentView[];
  refunds: RefundView[];
  canPay: boolean;
  canCancel: boolean;
  createdAt: string;
}

export interface PassengerInput {
  fullName: string;
  dateOfBirth: string;
  passportNumber: string | null;
}

export interface CreateBookingRequest {
  departureId: number;
  contactName: string;
  contactPhone: string;
  contactEmail: string;
  note: string | null;
  adults: number;
  children: number;
  infants: number;
  /** Đủ từng khách, khớp số người lớn / trẻ em / trẻ sơ sinh */
  passengers: PassengerInput[];
}

export interface CancellationQuote {
  cancellable: boolean;
  paidAmount: number;
  refundPercent: number;
  refundAmount: number;
  daysBeforeDeparture: number;
  explanation: string;
}

export interface PaymentResult {
  success: boolean;
  bookingId: number | null;
  bookingCode: string | null;
  bookingStatus: BookingStatus | null;
  message: string;
}

export interface Manifest {
  departureId: number;
  tourTitle: string;
  startDate: string;
  endDate: string;
  capacity: number;
  seatsBooked: number;
  bookings: {
    bookingId: number;
    code: string;
    status: BookingStatus;
    contactName: string;
    contactPhone: string;
    contactEmail: string;
    note: string | null;
    adults: number;
    children: number;
    infants: number;
    passengers: BookingPassenger[];
  }[];
}
