"use client";

import Link from "next/link";
import { useQueries } from "@tanstack/react-query";
import { ArrowRight, CircleAlert, Clock, FilePen, Map, Plus } from "lucide-react";
import { PageHeader } from "@/components/common/PageHeader";
import { tourKeys } from "@/components/tour/manage/tourQueries";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { listMyTours } from "@/lib/api/tours";
import { useAuth } from "@/lib/auth/AuthProvider";
import { cn } from "@/lib/utils";
import type { TourStatus } from "@/types/tour";
import { useAgentProfile } from "./profile/useAgentProfile";

// Số tour theo trạng thái: gọi API danh sách với size=1 và đọc totalElements
const COUNTS: { status: TourStatus; label: string; hint: string; icon: typeof Map; attention?: boolean }[] = [
  { status: "PUBLISHED", label: "Tour đang bán", hint: "Khách đang tìm thấy", icon: Map },
  { status: "PENDING_APPROVAL", label: "Chờ duyệt", hint: "Quản trị viên đang xem xét", icon: Clock },
  { status: "NEEDS_REVISION", label: "Cần chỉnh sửa", hint: "Sửa theo góp ý rồi gửi lại", icon: CircleAlert, attention: true },
  { status: "DRAFT", label: "Bản nháp", hint: "Chưa gửi duyệt", icon: FilePen },
];

/** Tổng quan khu vực Quản lý kinh doanh. TODO: doanh thu, yêu cầu tư vấn. */
export function AgentDashboard() {
  const { user } = useAuth();
  const { data: profile, isPending } = useAgentProfile();
  const approved = profile?.status === "APPROVED";

  const counts = useQueries({
    queries: COUNTS.map(({ status }) => {
      const params = { status, size: 1 };
      return {
        queryKey: tourKeys.list("agent", params),
        queryFn: () => listMyTours(params),
        enabled: approved,
      };
    }),
  });

  return (
    <>
      <PageHeader
        title={`Xin chào${user?.fullName ? `, ${user.fullName}` : ""}`}
        description={profile?.companyName ?? "Tổng quan hoạt động kinh doanh trên TripConnect."}
        actions={
          approved && (
            <Button asChild className="rounded-xl">
              <Link href="/agent/tours/new">
                <Plus /> Tạo tour
              </Link>
            </Button>
          )
        }
      />

      {isPending ? (
        <Skeleton className="h-40 rounded-2xl" />
      ) : !approved ? (
        <Alert className="rounded-2xl border-amber-200 bg-amber-50">
          <CircleAlert />
          <AlertTitle>Hồ sơ kinh doanh chưa được duyệt</AlertTitle>
          <AlertDescription>
            <p>Hoàn thiện hồ sơ và chờ quản trị viên duyệt để bắt đầu đăng bán tour.</p>
            <Link href="/agent/profile" className="mt-1 flex items-center gap-1 font-medium underline">
              Đến Hồ sơ kinh doanh <ArrowRight className="size-3.5" />
            </Link>
          </AlertDescription>
        </Alert>
      ) : (
        <div className="grid gap-4 sm:grid-cols-2">
          {COUNTS.map(({ status, label, hint, icon: Icon, attention }, index) => {
            const result = counts[index];
            const value = result.data?.totalElements ?? 0;
            const highlight = attention && value > 0;
            return (
              <Link key={status} href={`/agent/tours?status=${status}`} className="group rounded-2xl">
                <Card className={cn("h-full rounded-2xl transition-shadow group-hover:shadow-md", highlight && "border-red-200 bg-red-50/60")}>
                  <CardContent className="flex items-start justify-between gap-4">
                    <div className="flex flex-col gap-1">
                      <p className="text-sm text-muted-foreground">{label}</p>
                      {result.isPending ? (
                        <Skeleton className="h-9 w-12" />
                      ) : (
                        <p className="text-3xl font-semibold tabular-nums">{value}</p>
                      )}
                      <p className="flex items-center gap-1 text-sm text-muted-foreground group-hover:text-foreground">
                        {hint} <ArrowRight className="size-3.5" />
                      </p>
                    </div>
                    <span className="flex size-11 shrink-0 items-center justify-center rounded-xl bg-primary/10 text-primary">
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
