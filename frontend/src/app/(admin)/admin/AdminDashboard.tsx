"use client";

import Link from "next/link";
import { ArrowRight, FilePen, Map, Store, Ticket, UserCheck, Users } from "lucide-react";
import { PageHeader } from "@/components/common/PageHeader";
import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { useAuth } from "@/lib/auth/AuthProvider";
import { errorMessage } from "@/lib/api/errors";
import { cn } from "@/lib/utils";
import type { AdminSummaryResponse } from "@/types/admin";
import { useAdminSummary } from "./adminQueries";

const CARDS: {
  key: keyof AdminSummaryResponse;
  label: string;
  hint: string;
  href: string;
  icon: typeof Users;
  /** Thẻ "việc cần làm": nổi bật khi > 0 */
  todo?: boolean;
}[] = [
  {
    key: "pendingAgentProfiles",
    label: "Hồ sơ đối tác chờ duyệt",
    hint: "Xét duyệt hồ sơ mới",
    href: "/admin/agents",
    icon: Store,
    todo: true,
  },
  {
    key: "pendingChangeRequests",
    label: "Yêu cầu cập nhật chờ duyệt",
    hint: "Đối tác xin đổi thông tin pháp lý",
    href: "/admin/change-requests",
    icon: FilePen,
    todo: true,
  },
  {
    key: "pendingTours",
    label: "Tour chờ duyệt",
    hint: "Tour mới hoặc tour vừa được chỉnh sửa",
    href: "/admin/tours",
    icon: Map,
    todo: true,
  },
  {
    key: "manualRefunds",
    label: "Đơn cần hoàn tiền thủ công",
    hint: "VNPay hoàn tự động không thành công",
    href: "/admin/bookings?status=MANUAL_REFUND",
    icon: Ticket,
    todo: true,
  },
  { key: "approvedAgents", label: "Đối tác đang hoạt động", hint: "Hồ sơ đã được duyệt", href: "/admin/agents?status=APPROVED", icon: UserCheck },
  { key: "totalUsers", label: "Tổng số tài khoản", hint: "Khách hàng, đối tác, quản trị", href: "/admin/users", icon: Users },
];

export function AdminDashboard() {
  const { user } = useAuth();
  const { data, isPending, error } = useAdminSummary();

  return (
    <>
      <PageHeader
        title={`Xin chào${user?.fullName ? `, ${user.fullName}` : ""}`}
        description="Tổng quan các việc cần xử lý trên TripConnect."
      />
      {error ? (
        <p className="text-sm text-destructive">{errorMessage(error)}</p>
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
          {CARDS.map(({ key, label, hint, href, icon: Icon, todo }) => {
            const value = data?.[key] ?? 0;
            const highlight = todo && value > 0;
            return (
              <Link key={key} href={href} className="group rounded-2xl focus-visible:ring-2 focus-visible:ring-ring focus-visible:outline-none">
                <Card className={cn("h-full rounded-2xl transition-shadow group-hover:shadow-md", highlight && "border-amber-300 bg-amber-50/60")}>
                  <CardContent className="flex items-start justify-between gap-4">
                    <div className="flex flex-col gap-1">
                      <p className="text-sm text-muted-foreground">{label}</p>
                      {isPending ? (
                        <Skeleton className="h-9 w-16" />
                      ) : (
                        <p className={cn("text-3xl font-semibold tabular-nums", highlight && "text-amber-900")}>{value}</p>
                      )}
                      <p className="flex items-center gap-1 text-sm text-muted-foreground group-hover:text-foreground">
                        {hint} <ArrowRight className="size-3.5" />
                      </p>
                    </div>
                    <span
                      className={cn(
                        "flex size-11 shrink-0 items-center justify-center rounded-xl bg-primary/10 text-primary",
                        highlight && "bg-amber-100 text-amber-900",
                      )}
                    >
                      <Icon className="size-5" />
                    </span>
                  </CardContent>
                </Card>
              </Link>
            );
          })}
        </div>
      )}
    </>
  );
}
