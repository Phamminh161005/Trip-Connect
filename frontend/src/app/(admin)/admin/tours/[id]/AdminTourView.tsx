"use client";

import { useState } from "react";
import Link from "next/link";
import { ArrowRight, Ban, Check, Globe, RotateCcw, Undo2 } from "lucide-react";
import { toast } from "sonner";
import { ConfirmDialog } from "@/components/common/ConfirmDialog";
import { PageHeader } from "@/components/common/PageHeader";
import { ReasonDialog } from "@/components/common/ReasonDialog";
import { TourDeparturesTable } from "@/components/tour/TourDeparturesTable";
import { TourDetailContent } from "@/components/tour/TourDetailContent";
import { TourStatusBadge } from "@/components/tour/TourStatusBadge";
import { TourManageView } from "@/components/tour/manage/TourManageView";
import { useManagedTour, useTourRefresh } from "@/components/tour/manage/tourQueries";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { errorMessage } from "@/lib/api/errors";
import { approveTour, getManagedItineraryFileUrl, requestTourRevision, suspendTour, unsuspendTour } from "@/lib/api/tours";
import { formatDateTime } from "@/lib/format";
import type { TourDetail } from "@/types/tour";

const BACK = { href: "/admin/tours", label: "Danh sách tour" };

/** Tour của TripConnect -> trang quản lý (sửa, ảnh, lịch); tour của đối tác -> trang duyệt. */
export function AdminTourView({ id }: { id: number }) {
  const { data: tour, isPending, error } = useManagedTour("admin", id);

  if (isPending) {
    return (
      <div className="flex flex-col gap-4">
        <Skeleton className="h-10 w-72" />
        <Skeleton className="h-96 rounded-2xl" />
      </div>
    );
  }
  if (error) {
    return (
      <>
        <PageHeader title="Tour" back={BACK} />
        <p className="text-sm text-destructive">{errorMessage(error)}</p>
      </>
    );
  }
  if (tour.platformTour) return <TourManageView scope="admin" id={id} basePath="/admin/tours" />;
  return <TourReview tour={tour} />;
}

type Dialog = "approve" | "revision" | "suspend" | "unsuspend" | null;

function TourReview({ tour }: { tour: TourDetail }) {
  const refresh = useTourRefresh("admin", tour.id);
  const [dialog, setDialog] = useState<Dialog>(null);

  const run = async (task: () => Promise<TourDetail>, success: string) => {
    try {
      await refresh(await task());
      toast.success(success);
    } catch (error) {
      toast.error(errorMessage(error));
      await refresh();
      throw error;
    }
  };

  const pending = tour.status === "PENDING_APPROVAL";
  const reReview = pending && tour.publishedAt !== null;

  return (
    <>
      <PageHeader
        back={BACK}
        title={
          <span className="flex flex-wrap items-center gap-3">
            Duyệt tour <TourStatusBadge status={tour.status} />
          </span>
        }
        description={
          tour.submittedAt
            ? `Gửi duyệt lúc ${formatDateTime(tour.submittedAt)}${tour.reviewedAt ? ` · Xử lý lần cuối ${formatDateTime(tour.reviewedAt)}` : ""}`
            : "Đối tác chưa gửi duyệt tour này."
        }
        actions={
          <>
            {pending && (
              <>
                <Button variant="outline" className="rounded-xl" onClick={() => setDialog("revision")}>
                  <Undo2 /> Yêu cầu chỉnh sửa
                </Button>
                <Button className="rounded-xl" onClick={() => setDialog("approve")}>
                  <Check /> Duyệt tour
                </Button>
              </>
            )}
            {(tour.status === "PUBLISHED" || tour.status === "HIDDEN") && (
              <Button variant="outline" className="rounded-xl text-destructive hover:text-destructive" onClick={() => setDialog("suspend")}>
                <Ban /> Đình chỉ
              </Button>
            )}
            {tour.status === "SUSPENDED" && (
              <Button variant="outline" className="rounded-xl" onClick={() => setDialog("unsuspend")}>
                <RotateCcw /> Bỏ đình chỉ
              </Button>
            )}
          </>
        }
      />

      <div className="mb-6 flex flex-col gap-3">
        {reReview && (
          <Alert className="rounded-2xl border-amber-200 bg-amber-50">
            <AlertTitle>Duyệt lại tour đã từng bán</AlertTitle>
            <AlertDescription>
              Đối tác đã chỉnh sửa nội dung tour đang bán (công khai lần đầu {formatDateTime(tour.publishedAt)}). Tour đang tạm ẩn cho tới
              khi được duyệt.
            </AlertDescription>
          </Alert>
        )}
        {tour.statusReason && (
          <Alert className="rounded-2xl border-red-200 bg-red-50">
            <AlertTitle>{tour.status === "SUSPENDED" ? "Lý do đình chỉ" : "Góp ý lần duyệt trước"}</AlertTitle>
            <AlertDescription className="whitespace-pre-line">{tour.statusReason}</AlertDescription>
          </Alert>
        )}
        {tour.international && pending && (
          <Alert className="rounded-2xl border-violet-200 bg-violet-50">
            <Globe />
            <AlertTitle>Tour quốc tế</AlertTitle>
            <AlertDescription>
              Kiểm tra đối tác có giấy phép lữ hành <strong>quốc tế</strong> trong hồ sơ trước khi duyệt.
            </AlertDescription>
          </Alert>
        )}
        {tour.provider && (
          <Card className="rounded-2xl">
            <CardContent className="flex flex-wrap items-center justify-between gap-2 text-sm">
              <span>
                Đơn vị tổ chức: <strong>{tour.provider.companyName ?? "Đối tác"}</strong>
              </span>
              <Link
                href={`/admin/agents/${tour.provider.agentProfileId}`}
                className="flex items-center gap-1 font-medium text-primary hover:underline"
              >
                Xem hồ sơ đối tác <ArrowRight className="size-3.5" />
              </Link>
            </CardContent>
          </Card>
        )}
      </div>

      <TourDetailContent
        tour={tour}
        getItineraryFileUrl={() => getManagedItineraryFileUrl("admin", tour.id)}
        departures={<TourDeparturesTable departures={tour.departures} mode="review" />}
      />

      <ConfirmDialog
        open={dialog === "approve"}
        onOpenChange={(open) => !open && setDialog(null)}
        title="Duyệt tour này?"
        description="Tour hiển thị với khách ngay và đối tác nhận email thông báo."
        confirmLabel="Duyệt tour"
        onConfirm={() => run(() => approveTour(tour.id), "Đã duyệt tour")}
      />
      <ReasonDialog
        open={dialog === "revision"}
        onOpenChange={(open) => !open && setDialog(null)}
        title="Yêu cầu chỉnh sửa tour"
        description="Tour chuyển về trạng thái “Cần chỉnh sửa” để đối tác sửa và gửi lại."
        label="Cần sửa những gì?"
        placeholder="Ví dụ: Ảnh bìa bị mờ; lịch trình ngày 2 thiếu giờ tập trung."
        confirmLabel="Gửi yêu cầu"
        onConfirm={(reason) => run(() => requestTourRevision(tour.id, reason), "Đã gửi yêu cầu chỉnh sửa")}
      />
      <ReasonDialog
        open={dialog === "suspend"}
        onOpenChange={(open) => !open && setDialog(null)}
        title="Đình chỉ tour?"
        description="Tour bị ẩn và không nhận đặt chỗ mới; đối tác không tự mở lại được. Lịch đã có khách vẫn giữ nguyên."
        label="Lý do đình chỉ"
        placeholder="Ví dụ: Nội dung quảng cáo không đúng thực tế."
        confirmLabel="Đình chỉ"
        onConfirm={(reason) => run(() => suspendTour(tour.id, reason), "Đã đình chỉ tour")}
      />
      <ConfirmDialog
        open={dialog === "unsuspend"}
        onOpenChange={(open) => !open && setDialog(null)}
        title="Bỏ đình chỉ tour?"
        description="Tour hiển thị lại với khách và đối tác nhận email thông báo."
        confirmLabel="Bỏ đình chỉ"
        onConfirm={() => run(() => unsuspendTour(tour.id), "Đã bỏ đình chỉ tour")}
      />
    </>
  );
}
