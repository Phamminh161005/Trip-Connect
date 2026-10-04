import type {
  BookingDetail,
  BookingStatus,
  BookingSummary,
  CancellationQuote,
  CreateBookingRequest,
  Manifest,
  PassengerInput,
  PaymentResult,
  RefundStatus,
} from "@/types/booking";
import type { PageResponse } from "@/types/common";
import { apiRequest } from "./client";
import { query } from "./query";

// ----- Khách (và Agent đặt tour như khách) -----

export const createBooking = (data: CreateBookingRequest) =>
  apiRequest<{ booking: BookingDetail; paymentUrl: string }>("/api/bookings", { method: "POST", body: data });

export const listMyBookings = (params: { status?: BookingStatus; page?: number; size?: number }) =>
  apiRequest<PageResponse<BookingSummary>>(`/api/bookings${query({ ...params })}`);

export const getMyBooking = (id: number) => apiRequest<BookingDetail>(`/api/bookings/${id}`);

/** Tạo link VNPay mới cho đơn còn hạn giữ chỗ. */
export const payBooking = (id: number) => apiRequest<{ paymentUrl: string }>(`/api/bookings/${id}/pay`, { method: "POST" });

/** Thay danh sách hành khách (được nhập thiếu, tới hạn chót trước ngày đi). */
export const updateBookingPassengers = (id: number, passengers: PassengerInput[]) =>
  apiRequest<BookingDetail>(`/api/bookings/${id}/passengers`, { method: "PUT", body: { passengers } });

export const getCancellationQuote = (id: number) => apiRequest<CancellationQuote>(`/api/bookings/${id}/cancellation-quote`);

export const cancelMyBooking = (id: number, reason: string | null) =>
  apiRequest<BookingDetail>(`/api/bookings/${id}/cancel`, { method: "POST", body: { reason } });

/** Trang quay về từ VNPay: chuyển nguyên tham số VNPay gắn trên link cho Backend kiểm chữ ký + ghi nhận. */
export const confirmVnPayReturn = (search: string) =>
  apiRequest<PaymentResult>(`/api/payments/vnpay/return${search.startsWith("?") ? search : `?${search}`}`, { auth: false });

// ----- Quản lý (Agent: đơn tour của mình; Admin: mọi đơn) -----

export type BookingScope = "agent" | "admin";

export interface ManagedBookingListParams {
  status?: BookingStatus;
  refundStatus?: RefundStatus;
  tourId?: number;
  departureId?: number;
  q?: string;
  page?: number;
  size?: number;
}

export const listManagedBookings = (scope: BookingScope, params: ManagedBookingListParams) =>
  apiRequest<PageResponse<BookingSummary>>(`/api/${scope}/bookings${query({ ...params })}`);

export const getManagedBooking = (scope: BookingScope, id: number) => apiRequest<BookingDetail>(`/api/${scope}/bookings/${id}`);

export const getManifest = (scope: BookingScope, departureId: number) =>
  apiRequest<Manifest>(`/api/${scope}/bookings/departures/${departureId}/manifest`);

/** Admin hủy đơn vì bất khả kháng (hoàn 100%). */
export const cancelBookingByAdmin = (id: number, reason: string) =>
  apiRequest<BookingDetail>(`/api/admin/bookings/${id}/cancel`, { method: "POST", body: { reason } });

export const retryRefund = (refundId: number) => apiRequest<void>(`/api/admin/refunds/${refundId}/retry`, { method: "POST" });

export const markRefundDone = (refundId: number, note: string) =>
  apiRequest<void>(`/api/admin/refunds/${refundId}/mark-done`, { method: "POST", body: { note } });
