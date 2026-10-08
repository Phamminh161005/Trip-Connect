"use client";

import {
  Ban,
  BadgeCheck,
  CalendarClock,
  CircleCheck,
  CircleX,
  Eye,
  EyeOff,
  FilePen,
  Hourglass,
  Lock,
  PenTool,
  Sparkles,
  UserX,
  Banknote,
  MessageCircle,
  MessageSquareWarning,
  WalletCards,
  MessageSquareReply,
  Star,
  Map as MapIcon,
  RotateCcw,
  Store,
  Ticket,
  TrendingDown,
  TriangleAlert,
  Wallet,
  type LucideIcon,
} from "lucide-react";
import { formatRelative } from "@/lib/format";
import { cn } from "@/lib/utils";
import type { NotificationItem, NotificationType } from "@/types/notification";

type Tone = "success" | "danger" | "warning" | "info";

const TYPE_STYLE: Record<NotificationType, { icon: LucideIcon; tone: Tone }> = {
  BOOKING_PAID: { icon: CircleCheck, tone: "success" },
  BOOKING_CANCELLED: { icon: CircleX, tone: "danger" },
  REFUND_COMPLETED: { icon: Wallet, tone: "success" },
  TRIP_REMINDER: { icon: CalendarClock, tone: "info" },
  REVIEW_INVITE: { icon: Star, tone: "warning" },
  REVIEW_REPLIED: { icon: MessageSquareReply, tone: "info" },
  REVIEW_HIDDEN: { icon: EyeOff, tone: "danger" },
  REVIEW_UNHIDDEN: { icon: Eye, tone: "success" },
  NEW_REVIEW: { icon: Star, tone: "warning" },
  CUSTOM_REQUEST_ACCEPTED: { icon: PenTool, tone: "success" },
  CUSTOM_REQUEST_CLOSED: { icon: Lock, tone: "info" },
  CUSTOM_REQUEST_ASSIGNED: { icon: Sparkles, tone: "warning" },
  CUSTOM_REQUEST_EXPIRED: { icon: Hourglass, tone: "danger" },
  CUSTOM_REQUEST_CANCELLED: { icon: CircleX, tone: "info" },
  CUSTOM_REQUEST_NEW: { icon: Sparkles, tone: "info" },
  CUSTOM_REQUEST_DECLINED: { icon: UserX, tone: "warning" },
  CUSTOM_REQUEST_REASSIGNING: { icon: RotateCcw, tone: "warning" },
  CUSTOM_PROPOSAL_RECEIVED: { icon: MapIcon, tone: "success" },
  CUSTOM_PROPOSAL_EXPIRING: { icon: Hourglass, tone: "warning" },
  CUSTOM_PROPOSAL_EXPIRED: { icon: Hourglass, tone: "info" },
  CUSTOM_PROPOSAL_REVISION_REQUESTED: { icon: FilePen, tone: "warning" },
  CUSTOM_PROPOSAL_ACCEPTED: { icon: CircleCheck, tone: "success" },
  CUSTOM_PROPOSAL_DUE_SOON: { icon: CalendarClock, tone: "warning" },
  CUSTOM_PROPOSAL_OVERDUE: { icon: CircleX, tone: "danger" },
  CHAT_MESSAGE: { icon: MessageCircle, tone: "info" },
  BOOKING_DEPOSIT_PAID: { icon: CircleCheck, tone: "success" },
  BOOKING_PAYMENT_DUE: { icon: CalendarClock, tone: "warning" },
  BOOKING_BALANCE_EXTENDED: { icon: CalendarClock, tone: "info" },
  SETTLEMENT_CREATED: { icon: WalletCards, tone: "warning" },
  SETTLEMENT_RESOLVED: { icon: WalletCards, tone: "info" },
  SETTLEMENT_PAID: { icon: Banknote, tone: "success" },
  SETTLEMENT_DISPUTED: { icon: MessageSquareWarning, tone: "warning" },
  SETTLEMENT_READY: { icon: Banknote, tone: "info" },
  TOUR_REVIEW_HIDDEN: { icon: EyeOff, tone: "info" },
  TOUR_REVIEW_UNHIDDEN: { icon: Eye, tone: "info" },
  NEW_BOOKING: { icon: Ticket, tone: "success" },
  TOUR_APPROVED: { icon: BadgeCheck, tone: "success" },
  TOUR_NEEDS_REVISION: { icon: FilePen, tone: "warning" },
  TOUR_SUSPENDED: { icon: Ban, tone: "danger" },
  TOUR_UNSUSPENDED: { icon: RotateCcw, tone: "success" },
  AGENT_PROFILE_APPROVED: { icon: BadgeCheck, tone: "success" },
  AGENT_PROFILE_NEEDS_REVISION: { icon: FilePen, tone: "warning" },
  CHANGE_REQUEST_APPROVED: { icon: BadgeCheck, tone: "success" },
  CHANGE_REQUEST_REJECTED: { icon: CircleX, tone: "danger" },
  DEPARTURE_REMINDER: { icon: CalendarClock, tone: "info" },
  LOW_BOOKINGS: { icon: TrendingDown, tone: "warning" },
  AGENT_PROFILE_SUBMITTED: { icon: Store, tone: "info" },
  CHANGE_REQUEST_SUBMITTED: { icon: FilePen, tone: "info" },
  TOUR_SUBMITTED: { icon: MapIcon, tone: "info" },
  MANUAL_REFUND_NEEDED: { icon: TriangleAlert, tone: "danger" },
};

const TONE_CLASS: Record<Tone, string> = {
  success: "bg-emerald-100 text-emerald-700 dark:bg-emerald-950 dark:text-emerald-300",
  danger: "bg-red-100 text-red-700 dark:bg-red-950 dark:text-red-300",
  warning: "bg-amber-100 text-amber-700 dark:bg-amber-950 dark:text-amber-300",
  info: "bg-sky-100 text-sky-700 dark:bg-sky-950 dark:text-sky-300",
};

/** Một dòng thông báo (dùng ở bảng chuông và trang Thông báo). Chưa đọc: nền xanh nhạt + chấm. */
export function NotificationRow({ item, onOpen }: { item: NotificationItem; onOpen: (item: NotificationItem) => void }) {
  const style = TYPE_STYLE[item.type] ?? { icon: CalendarClock, tone: "info" as const };
  const Icon = style.icon;
  return (
    <button
      type="button"
      onClick={() => onOpen(item)}
      className={cn(
        "flex w-full items-start gap-3 rounded-xl px-3 py-2.5 text-left transition-colors outline-none hover:bg-accent focus-visible:ring-3 focus-visible:ring-ring/50",
        !item.read && "bg-primary/5",
      )}
    >
      <span className={cn("mt-0.5 flex size-9 shrink-0 items-center justify-center rounded-full", TONE_CLASS[style.tone])}>
        <Icon className="size-4" />
      </span>
      <span className="flex min-w-0 flex-1 flex-col gap-0.5">
        <span className={cn("text-sm leading-snug", item.read ? "text-foreground/90" : "font-semibold")}>{item.title}</span>
        {item.body && <span className="line-clamp-2 text-xs text-muted-foreground">{item.body}</span>}
        <span className="text-xs text-muted-foreground">{formatRelative(item.createdAt)}</span>
      </span>
      {!item.read && <span className="mt-2 size-2 shrink-0 rounded-full bg-primary" aria-label="Chưa đọc" />}
    </button>
  );
}
