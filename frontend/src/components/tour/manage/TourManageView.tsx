"use client";

import { useState, type ReactNode } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import {
  ArrowLeft,
  CircleAlert,
  Eye,
  EyeOff,
  ExternalLink,
  Globe,
  Pencil,
  RefreshCw,
  Send,
  Trash2,
  Undo2,
} from "lucide-react";
import { toast } from "sonner";
import { ConfirmDialog } from "@/components/common/ConfirmDialog";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { Card, CardAction, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { locationLabel } from "@/components/auth/register/LocationMultiSelect";
import { errorMessage } from "@/lib/api/errors";
import { deleteTour, hideTour, publishTour, submitTour, unhideTour, withdrawTour, type TourScope } from "@/lib/api/tours";
import { formatDateTime } from "@/lib/format";
import { formatDuration } from "@/lib/tour/labels";
import type { TourDetail } from "@/types/tour";
import { TourStatusBadge } from "../TourStatusBadge";
import { TourDeparturesSection } from "./TourDeparturesSection";
import { TourImagesSection } from "./TourImagesSection";
import { TourItineraryFileSection } from "./TourItineraryFileSection";
import { contentLocked, useManagedTour, useTourRefresh } from "./tourQueries";

type Confirm = "submit" | "withdraw" | "hide" | "unhide" | "publish" | "delete" | null;

/**
 * Trang quản lý một tour: trạng thái + việc cần làm, ảnh, file PDF, lịch khởi hành.
 * Dùng cho Agent (/agent/tours/[id]) và Admin với tour của TripConnect (/admin/tours/[id]).
 */
export function TourManageView({ scope, id, basePath }: { scope: TourScope; id: number; basePath: string }) {
  const { data: tour, isPending, isError, error, refetch } = useManagedTour(scope, id);

  if (isPending) {
    return (
      <div className="flex flex-col gap-6" aria-busy="true">
        <Skeleton className="h-10 w-2/3" />
        <Skeleton className="h-40 rounded-2xl" />
        <Skeleton className="h-72 rounded-2xl" />
      </div>
    );
  }
  if (isError) {
    return (
      <div className="flex flex-col items-center gap-4 rounded-2xl border bg-card p-10 text-center">
        <p className="text-muted-foreground">{errorMessage(error)}</p>
        <div className="flex gap-2">
          <Button asChild variant="outline" className="rounded-xl">
            <Link href={basePath}>
              <ArrowLeft /> Danh sách tour
            </Link>
          </Button>
          <Button variant="outline" className="rounded-xl" onClick={() => refetch()}>
            <RefreshCw /> Thử lại
          </Button>
        </div>
      </div>
    );
  }
  return <TourManage scope={scope} tour={tour} basePath={basePath} />;
}

function TourManage({ scope, tour, basePath }: { scope: TourScope; tour: TourDetail; basePath: string }) {
  const router = useRouter();
  const refresh = useTourRefresh(scope, tour.id);
  const [confirm, setConfirm] = useState<Confirm>(null);
  const isAgent = scope === "agent";
  const locked = contentLocked(tour);
  const ready = tour.missingItems.length === 0;

  const run = async (task: () => Promise<TourDetail | void>, success: string) => {
    try {
      const updated = await task();
      toast.success(success);
      await refresh(updated ?? undefined);
    } catch (error) {
      toast.error(errorMessage(error));
      throw error;
    }
  };

  const confirmConfig: Record<Exclude<Confirm, null>, { title: string; description: string; label: string; action: () => Promise<void>; destructive?: boolean }> = {
    submit: {
      title: "Gửi tour cho quản trị viên duyệt?",
      description: "Trong lúc chờ duyệt bạn không sửa được nội dung (có thể rút lại). Lịch khởi hành vẫn thêm / sửa được.",
      label: "Gửi duyệt",
      action: () => run(() => submitTour(tour.id), "Đã gửi duyệt. Bạn sẽ nhận email khi có kết quả."),
    },
    withdraw: {
      title: "Rút lại yêu cầu duyệt?",
      description: "Tour quay về Nháp để bạn chỉnh sửa tiếp, sau đó gửi duyệt lại.",
      label: "Rút lại",
      action: () => run(() => withdrawTour(tour.id), "Đã rút lại yêu cầu duyệt"),
    },
    hide: {
      title: "Tạm ẩn tour?",
      description: "Khách không tìm thấy và không đặt thêm được. Các lịch đã có khách đặt vẫn khởi hành bình thường.",
      label: "Tạm ẩn",
      action: () => run(() => hideTour(scope, tour.id), "Đã tạm ẩn tour"),
    },
    unhide: {
      title: "Hiện lại tour?",
      description: "Tour hiển thị lại với khách ngay (nội dung không đổi nên không cần duyệt lại).",
      label: "Hiện lại",
      action: () => run(() => unhideTour(scope, tour.id), "Tour đã hiển thị lại"),
    },
    publish: {
      title: "Công khai tour?",
      description: "Tour của TripConnect hiển thị với khách ngay, không qua bước duyệt.",
      label: "Công khai",
      action: () => run(() => publishTour(tour.id), "Tour đã được công khai"),
    },
    delete: {
      title: "Xóa tour vĩnh viễn?",
      description: "Toàn bộ nội dung, ảnh và lịch khởi hành bị xóa. Chỉ xóa được tour chưa có khách đặt.",
      label: "Xóa tour",
      destructive: true,
      action: async () => {
        try {
          await deleteTour(scope, tour.id);
          toast.success("Đã xóa tour");
          await refresh();
          router.replace(basePath);
        } catch (error) {
          toast.error(errorMessage(error));
          throw error;
        }
      },
    },
  };

  const canSubmit = isAgent && (tour.status === "DRAFT" || tour.status === "NEEDS_REVISION");
  const canPublish = !isAgent && (tour.status === "DRAFT" || tour.status === "HIDDEN");
  const current = confirm ? confirmConfig[confirm] : null;

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-col gap-3">
        <Link href={basePath} className="flex w-fit items-center gap-1.5 text-sm text-muted-foreground hover:text-foreground">
          <ArrowLeft className="size-4" /> Danh sách tour
        </Link>
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div className="min-w-0">
            <h1 className="flex flex-wrap items-center gap-3 text-2xl font-bold tracking-tight">
              {tour.title}
              <TourStatusBadge status={tour.status} />
            </h1>
            <p className="mt-1 text-sm text-muted-foreground">
              {formatDuration(tour.durationDays, tour.durationNights)} · Khởi hành từ {locationLabel(tour.departureLocation)} ·{" "}
              {tour.destinations.map(locationLabel).join(", ")}
            </p>
          </div>
          <div className="flex flex-wrap gap-2">
            {tour.status === "PUBLISHED" && (
              <Button asChild variant="outline" className="rounded-xl">
                <Link href={`/tours/${tour.id}`} target="_blank">
                  <ExternalLink /> Xem trang khách
                </Link>
              </Button>
            )}
            {tour.status !== "PUBLISHED" && (
              <Button asChild variant="outline" className="rounded-xl">
                <Link href={`${basePath}/${tour.id}/preview`}>
                  <Eye /> Xem trước
                </Link>
              </Button>
            )}
            {tour.status === "PUBLISHED" && (
              <Button variant="outline" className="rounded-xl" onClick={() => setConfirm("hide")}>
                <EyeOff /> Tạm ẩn
              </Button>
            )}
            {tour.status === "HIDDEN" && isAgent && (
              <Button variant="outline" className="rounded-xl" onClick={() => setConfirm("unhide")}>
                <Eye /> Hiện lại
              </Button>
            )}
            {isAgent && tour.status === "PENDING_APPROVAL" && (
              <Button variant="outline" className="rounded-xl" onClick={() => setConfirm("withdraw")}>
                <Undo2 /> Rút lại
              </Button>
            )}
            {canSubmit && (
              <Button className="rounded-xl" disabled={!ready} onClick={() => setConfirm("submit")}>
                <Send /> Gửi duyệt
              </Button>
            )}
            {canPublish && (
              <Button className="rounded-xl" disabled={!ready} onClick={() => setConfirm("publish")}>
                <Globe /> Công khai
              </Button>
            )}
          </div>
        </div>
      </div>

      <StatusAlert tour={tour} isAgent={isAgent} />

      {tour.missingItems.length > 0 && (
        <Alert className="rounded-2xl">
          <CircleAlert />
          <AlertTitle>Cần hoàn thiện trước khi {isAgent ? "gửi duyệt" : "công khai"}</AlertTitle>
          <AlertDescription>
            <ul className="list-disc pl-4">
              {tour.missingItems.map((item) => (
                <li key={item}>{item}</li>
              ))}
            </ul>
          </AlertDescription>
        </Alert>
      )}

      <Card className="rounded-2xl">
        <CardHeader>
          <CardTitle className="text-lg">Nội dung tour</CardTitle>
          <CardDescription>
            Thông tin chung, lịch trình {tour.itinerary.length} ngày, {tour.highlights.length} điểm nổi bật,{" "}
            {tour.includedServices.length} dịch vụ bao gồm.
            {tour.updatedAt && ` Cập nhật lần cuối ${formatDateTime(tour.updatedAt)}.`}
          </CardDescription>
          {!locked && (
            <CardAction>
              <Button asChild variant="outline" size="sm" className="rounded-lg">
                <Link href={`${basePath}/${tour.id}/edit`}>
                  <Pencil /> Sửa nội dung
                </Link>
              </Button>
            </CardAction>
          )}
        </CardHeader>
      </Card>

      <TourImagesSection scope={scope} tour={tour} />
      <TourDeparturesSection scope={scope} tour={tour} />
      <TourItineraryFileSection scope={scope} tour={tour} />

      {tour.status !== "SUSPENDED" && (
        <Card className="rounded-2xl border-destructive/30">
          <CardHeader>
            <CardTitle className="text-lg">Xóa tour</CardTitle>
            <CardDescription>Chỉ xóa được tour chưa có khách đặt. Tour đã có khách hãy dùng Tạm ẩn để ngừng bán.</CardDescription>
            <CardAction>
              <Button variant="outline" size="sm" className="rounded-lg text-destructive hover:text-destructive" onClick={() => setConfirm("delete")}>
                <Trash2 /> Xóa tour
              </Button>
            </CardAction>
          </CardHeader>
        </Card>
      )}

      {current && (
        <ConfirmDialog
          open
          onOpenChange={(open) => !open && setConfirm(null)}
          title={current.title}
          description={current.description}
          confirmLabel={current.label}
          destructive={current.destructive}
          onConfirm={current.action}
        />
      )}
    </div>
  );
}

function StatusAlert({ tour, isAgent }: { tour: TourDetail; isAgent: boolean }) {
  const box = (className: string, title: string, body?: ReactNode) => (
    <Alert className={`rounded-2xl ${className}`}>
      <AlertTitle>{title}</AlertTitle>
      {body && <AlertDescription className="whitespace-pre-line">{body}</AlertDescription>}
    </Alert>
  );

  switch (tour.status) {
    case "PENDING_APPROVAL":
      return box(
        "border-amber-200 bg-amber-50",
        "Đang chờ quản trị viên duyệt",
        `Gửi lúc ${formatDateTime(tour.submittedAt)}. Nội dung tạm khóa; lịch khởi hành vẫn thêm / sửa được.`,
      );
    case "NEEDS_REVISION":
      return box("border-red-200 bg-red-50", "Quản trị viên yêu cầu chỉnh sửa", tour.statusReason);
    case "SUSPENDED":
      return box(
        "border-red-300 bg-red-50",
        "Tour đang bị đình chỉ",
        `${tour.statusReason ?? ""}\nKhông nhận đặt chỗ mới; lịch đã có khách vẫn giữ nguyên. Liên hệ bộ phận hỗ trợ nếu cần.`,
      );
    case "HIDDEN":
      return box("", "Tour đang tạm ẩn", "Khách không tìm thấy tour; các lịch đã có khách vẫn khởi hành.");
    case "DRAFT":
      return tour.publishedAt && isAgent
        ? box(
            "border-amber-200 bg-amber-50",
            "Bạn đã chỉnh sửa tour đang bán",
            "Tour tạm ẩn khỏi trang khách cho tới khi được duyệt lại. Chỉnh sửa xong hãy bấm Gửi duyệt.",
          )
        : null;
    default:
      return null;
  }
}
