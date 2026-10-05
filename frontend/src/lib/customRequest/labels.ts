import { formatDay, formatPrice } from "@/lib/tour/labels";
import type { LocationResponse } from "@/types/catalog";
import type { AssignmentStatus, CustomRequestStatus, ProposalStatus, ProposalView, RequestStage } from "@/types/customRequest";

// Khớp CustomRequestRules của Backend
export const CUSTOM_REQUEST_RULES = {
  minLeadDays: 14,
  maxStartWindowDays: 60,
  maxDurationDays: 30,
  maxTravellers: 100,
  maxDestinations: 10,
  maxCategories: 5,
  acceptHours: 48,
  proposalHours: 72,
  revisionHours: 48,
  responseDays: 3,
  maxRevisions: 5,
  inactiveCloseDays: 5,
  minProposalLeadDays: 7,
  depositPercent: 30,
} as const;

/** Nhãn theo người xem: khách không cần biết chi tiết "đang chờ Agent nhận". */
export const REQUEST_STATUS: Record<CustomRequestStatus, { label: string; customerLabel: string; className: string }> = {
  NEW: { label: "Chờ giao", customerLabel: "Đang tìm đơn vị phù hợp", className: "bg-amber-100 text-amber-900" },
  WAITING_AGENT: { label: "Chờ Agent nhận", customerLabel: "Đang tìm đơn vị phù hợp", className: "bg-sky-100 text-sky-900" },
  IN_PROGRESS: { label: "Đang xử lý", customerLabel: "Đang thiết kế tour", className: "bg-emerald-100 text-emerald-900" },
  AGREED: { label: "Đã chốt", customerLabel: "Đã chốt lịch trình", className: "bg-primary/15 text-primary" },
  CANCELLED: { label: "Đã hủy", customerLabel: "Đã hủy", className: "bg-muted text-muted-foreground" },
  CLOSED: { label: "Đã đóng", customerLabel: "Đã đóng", className: "bg-muted text-muted-foreground" },
};

export const ASSIGNMENT_STATUS: Record<AssignmentStatus, { label: string; className: string }> = {
  PENDING: { label: "Chờ phản hồi", className: "bg-amber-100 text-amber-900" },
  ACCEPTED: { label: "Đã nhận", className: "bg-emerald-100 text-emerald-900" },
  DECLINED: { label: "Đã từ chối", className: "bg-red-100 text-red-900" },
  EXPIRED: { label: "Hết hạn", className: "bg-muted text-muted-foreground" },
  REVOKED: { label: "Đã thu hồi", className: "bg-muted text-muted-foreground" },
  OVERDUE: { label: "Quá hạn đề xuất", className: "bg-red-100 text-red-900" },
};

export const PROPOSAL_STATUS: Record<ProposalStatus, { label: string; className: string }> = {
  SENT: { label: "Chờ khách phản hồi", className: "bg-amber-100 text-amber-900" },
  ACCEPTED: { label: "Đã đồng ý", className: "bg-emerald-100 text-emerald-900" },
  REVISION_REQUESTED: { label: "Yêu cầu chỉnh sửa", className: "bg-sky-100 text-sky-900" },
  DECLINED: { label: "Khách đã hủy", className: "bg-muted text-muted-foreground" },
  EXPIRED: { label: "Hết hạn phản hồi", className: "bg-muted text-muted-foreground" },
  WITHDRAWN: { label: "Đã gỡ", className: "bg-muted text-muted-foreground" },
};

/** Nhãn ngắn "đang chờ ai" cho danh sách. */
export const STAGE_LABEL: Record<RequestStage, { agent: string; customer: string }> = {
  DRAFTING: { agent: "Cần gửi đề xuất", customer: "Đang soạn đề xuất" },
  REVISING: { agent: "Cần gửi bản chỉnh sửa", customer: "Đang chỉnh sửa theo góp ý" },
  WAITING_CUSTOMER: { agent: "Chờ khách phản hồi", customer: "Có đề xuất chờ bạn xem" },
};

/** Những gì khác so với phiên bản trước (để khách / Agent thấy nhanh bản mới sửa gì). */
export function proposalChanges(current: ProposalView, previous: ProposalView | undefined): string[] {
  if (!previous) return [];
  const changes: string[] = [];
  if (current.totalPrice !== previous.totalPrice) changes.push(current.totalPrice < previous.totalPrice ? "Giảm giá" : "Tăng giá");
  if (current.startDate !== previous.startDate) changes.push("Ngày khởi hành");
  if (current.durationDays !== previous.durationDays || current.durationNights !== previous.durationNights) changes.push("Thời lượng");
  if (JSON.stringify(current.itinerary) !== JSON.stringify(previous.itinerary)) changes.push("Lịch trình");
  if (current.accommodationType !== previous.accommodationType) changes.push("Lưu trú");
  if ([...current.transportModes].sort().join() !== [...previous.transportModes].sort().join()) changes.push("Phương tiện");
  if (current.meetingPoint !== previous.meetingPoint || current.meetingTime !== previous.meetingTime) changes.push("Điểm đón");
  if (
    JSON.stringify(current.includedServices) !== JSON.stringify(previous.includedServices) ||
    JSON.stringify(current.excludedServices) !== JSON.stringify(previous.excludedServices)
  ) {
    changes.push("Dịch vụ");
  }
  if (current.title !== previous.title) changes.push("Tên chuyến đi");
  return changes;
}

export const placeName = (l: LocationResponse) => l.province ?? l.country;

export const destinationsText = (destinations: LocationResponse[]) => destinations.map(placeName).join(", ");

/** "15/11/2026 – 30/11/2026" hoặc một ngày nếu trùng. */
export const startWindowText = (earliest: string, latest: string) =>
  earliest === latest ? formatDay(earliest) : `${formatDay(earliest)} – ${formatDay(latest)}`;

const vnd = (value: number) => new Intl.NumberFormat("vi-VN").format(value);

/** "2.500.000 – 4.000.000 VNĐ / người" */
export function budgetText(min: number | null, max: number | null): string {
  if (min === null && max === null) return "Không giới hạn";
  if (min !== null && max !== null) {
    return min === max ? `${formatPrice(min)} / người` : `${vnd(min)} – ${formatPrice(max)} / người`;
  }
  return min !== null ? `Từ ${formatPrice(min)} / người` : `Tối đa ${formatPrice(max ?? 0)} / người`;
}

/** "còn 1 ngày 5 giờ" / "còn 3 giờ" / "đã quá hạn" (tính tại lúc hiển thị). */
export function timeLeftText(deadline: string, now: number = Date.now()): string {
  const minutes = Math.floor((new Date(deadline).getTime() - now) / 60_000);
  if (minutes <= 0) return "đã quá hạn";
  const days = Math.floor(minutes / 1440);
  const hours = Math.floor((minutes % 1440) / 60);
  if (days > 0) return `còn ${days} ngày${hours > 0 ? ` ${hours} giờ` : ""}`;
  return hours > 0 ? `còn ${hours} giờ` : `còn ${minutes} phút`;
}
