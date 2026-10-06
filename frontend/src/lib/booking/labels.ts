import type {
  BookingStatus,
  CancelledBy,
  PassengerType,
  PaymentPurpose,
  PaymentStatus,
  RefundRecordStatus,
  RefundStatus,
} from "@/types/booking";

// Giới hạn — khớp app.booking.* của Backend
export const BOOKING_RULES = {
  maxTravellers: 30,
  holdMinutes: 15,
  childMinAge: 2,
  adultMinAge: 12,
} as const;

export const BOOKING_STATUS: Record<BookingStatus, { label: string; className: string }> = {
  PENDING_PAYMENT: { label: "Chờ thanh toán", className: "bg-amber-100 text-amber-900" },
  DEPOSIT_PAID: { label: "Đã đặt cọc", className: "bg-violet-100 text-violet-900" },
  PAID: { label: "Đã thanh toán", className: "bg-emerald-100 text-emerald-900" },
  COMPLETED: { label: "Hoàn thành", className: "bg-sky-100 text-sky-900" },
  CANCELLED: { label: "Đã hủy", className: "bg-muted text-muted-foreground" },
};

export const REFUND_STATUS: Record<RefundStatus, { label: string; className: string }> = {
  NONE: { label: "Không hoàn tiền", className: "bg-muted text-muted-foreground" },
  PENDING: { label: "Đang hoàn tiền", className: "bg-amber-100 text-amber-900" },
  REFUNDED: { label: "Đã hoàn tiền", className: "bg-emerald-100 text-emerald-900" },
  MANUAL_REQUIRED: { label: "Chờ hoàn thủ công", className: "bg-red-100 text-red-900" },
};

export const PASSENGER_TYPE: Record<PassengerType, string> = {
  ADULT: "Người lớn",
  CHILD: "Trẻ em",
  INFANT: "Trẻ sơ sinh",
};

export const CANCELLED_BY: Record<CancelledBy, string> = {
  CUSTOMER: "Khách hàng",
  AGENT: "Đơn vị tổ chức",
  ADMIN: "Quản trị viên",
  SYSTEM: "Hệ thống",
};

export const PAYMENT_STATUS: Record<PaymentStatus, string> = {
  PENDING: "Chờ thanh toán",
  SUCCESS: "Thành công",
  FAILED: "Thất bại",
  EXPIRED: "Hết hạn",
};

export const PAYMENT_PURPOSE: Record<PaymentPurpose, string> = {
  FULL: "Toàn bộ",
  DEPOSIT: "Đặt cọc",
  BALANCE: "Phần còn lại",
};

export const REFUND_RECORD_STATUS: Record<RefundRecordStatus, string> = {
  PENDING: "Đang xử lý",
  SUCCESS: "Đã hoàn qua VNPay",
  MANUAL_REQUIRED: "Cần hoàn thủ công",
  MANUAL_DONE: "Đã hoàn thủ công",
};

/** Loại hành khách theo tuổi vào ngày khởi hành — giống BookingRules.classify của Backend. */
export function classifyPassenger(dateOfBirth: string, startDate: string): PassengerType | null {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(dateOfBirth)) return null;
  const [by, bm, bd] = dateOfBirth.split("-").map(Number);
  const [sy, sm, sd] = startDate.split("-").map(Number);
  let age = sy - by;
  if (sm < bm || (sm === bm && sd < bd)) age--;
  if (age < 0) return null;
  if (age < BOOKING_RULES.childMinAge) return "INFANT";
  if (age < BOOKING_RULES.adultMinAge) return "CHILD";
  return "ADULT";
}

/** "2 người lớn, 1 trẻ em, 1 trẻ sơ sinh" */
export function travellersText(adults: number, children: number, infants: number): string {
  return [
    adults && `${adults} người lớn`,
    children && `${children} trẻ em`,
    infants && `${infants} trẻ sơ sinh`,
  ]
    .filter(Boolean)
    .join(", ");
}
