import { Badge } from "@/components/ui/badge";
import { BOOKING_STATUS, REFUND_STATUS } from "@/lib/booking/labels";
import { cn } from "@/lib/utils";
import type { BookingStatus, RefundStatus } from "@/types/booking";

export function BookingStatusBadge({ status }: { status: BookingStatus }) {
  return <Badge className={cn("border-0", BOOKING_STATUS[status].className)}>{BOOKING_STATUS[status].label}</Badge>;
}

/** Chỉ hiện khi có chuyện hoàn tiền (bỏ qua "Không hoàn tiền"). */
export function RefundStatusBadge({ status }: { status: RefundStatus }) {
  if (status === "NONE") return null;
  return <Badge className={cn("border-0", REFUND_STATUS[status].className)}>{REFUND_STATUS[status].label}</Badge>;
}
