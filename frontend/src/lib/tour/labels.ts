import type { AccommodationType, DepartureStatus, TourDeparture, TourStatus, TransportMode } from "@/types/tour";

// Giới hạn nghiệp vụ — khớp TourRules.java của Backend
export const TOUR_RULES = {
  maxCategories: 3,
  maxDestinations: 20,
  maxDurationDays: 30,
  minHighlights: 3,
  maxHighlights: 6,
  maxServiceItems: 30,
  minImages: 3,
  maxImages: 10,
  maxImageBytes: 5 * 1024 * 1024,
  maxPdfBytes: 10 * 1024 * 1024,
  minDepartureLeadDays: 3,
  maxCapacity: 100,
  minAdultPrice: 10_000,
  maxPrice: 1_000_000_000,
} as const;

export const ACCEPTED_IMAGE_TYPES = ["image/jpeg", "image/png", "image/webp"];

export const TOUR_STATUS: Record<TourStatus, { label: string; className: string }> = {
  DRAFT: { label: "Nháp", className: "bg-sky-100 text-sky-900" },
  PENDING_APPROVAL: { label: "Chờ duyệt", className: "bg-amber-100 text-amber-900" },
  NEEDS_REVISION: { label: "Cần chỉnh sửa", className: "bg-red-100 text-red-900" },
  PUBLISHED: { label: "Đang bán", className: "bg-emerald-100 text-emerald-900" },
  HIDDEN: { label: "Tạm ẩn", className: "bg-muted text-muted-foreground" },
  SUSPENDED: { label: "Bị đình chỉ", className: "bg-red-200 text-red-950" },
};

export const TRANSPORT_LABELS: Record<TransportMode, string> = {
  BUS: "Xe du lịch",
  PLANE: "Máy bay",
  TRAIN: "Tàu hỏa",
  BOAT: "Tàu thủy / Du thuyền",
  MOTORBIKE: "Xe máy",
  SELF_ARRANGED: "Tự túc",
};

export const ACCOMMODATION_LABELS: Record<AccommodationType, string> = {
  NONE: "Không lưu trú (tour trong ngày)",
  HOMESTAY: "Homestay",
  HOTEL_2_3_STAR: "Khách sạn 2-3 sao",
  HOTEL_4_STAR: "Khách sạn 4 sao",
  HOTEL_5_STAR: "Khách sạn 5 sao",
  RESORT: "Resort",
  CRUISE: "Du thuyền",
};

export const DEPARTURE_STATUS: Record<DepartureStatus | "DEPARTED", { label: string; className: string }> = {
  OPEN: { label: "Đang mở bán", className: "bg-emerald-100 text-emerald-900" },
  CLOSED: { label: "Ngừng bán", className: "bg-muted text-muted-foreground" },
  CANCELLED: { label: "Đã hủy", className: "bg-red-100 text-red-900" },
  DEPARTED: { label: "Đã khởi hành", className: "bg-sky-100 text-sky-900" },
};

/** Trạng thái hiển thị của lịch: lịch đã tới ngày đi (và chưa hủy) hiện là "Đã khởi hành". */
export const departureDisplayStatus = (departure: TourDeparture): DepartureStatus | "DEPARTED" =>
  departure.status !== "CANCELLED" && departure.departed ? "DEPARTED" : departure.status;

const priceFormat = new Intl.NumberFormat("vi-VN");

/** 3490000 -> "3.490.000 VNĐ" */
export const formatPrice = (value: number) => `${priceFormat.format(value)} VNĐ`;

/** "2 ngày 1 đêm" / "1 ngày" */
export const formatDuration = (days: number, nights: number) => (nights > 0 ? `${days} ngày ${nights} đêm` : `${days} ngày`);

/** "2026-10-15" -> "15/10/2026" (tách chuỗi, không đổi múi giờ). */
export function formatDay(date: string): string {
  const [year, month, day] = date.split("-");
  return `${day}/${month}/${year}`;
}

const WEEKDAYS = ["CN", "T2", "T3", "T4", "T5", "T6", "T7"];

/** "2026-10-15" -> "T5, 15/10/2026" */
export function formatDayWithWeekday(date: string): string {
  const [year, month, day] = date.split("-").map(Number);
  return `${WEEKDAYS[new Date(year, month - 1, day).getDay()]}, ${formatDay(date)}`;
}

/** Ngày (yyyy-MM-dd) theo giờ máy người dùng, cộng thêm `plusDays`. */
export function localDateString(plusDays = 0): string {
  const date = new Date();
  date.setDate(date.getDate() + plusDays);
  const pad = (n: number) => String(n).padStart(2, "0");
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}

export function mealsText(day: { breakfast: boolean; lunch: boolean; dinner: boolean }): string | null {
  const meals = [day.breakfast && "Sáng", day.lunch && "Trưa", day.dinner && "Tối"].filter(Boolean);
  return meals.length ? `Bữa ăn: ${meals.join(", ")}` : null;
}
