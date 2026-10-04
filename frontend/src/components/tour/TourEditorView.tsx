"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useQueryClient } from "@tanstack/react-query";
import { ArrowLeft } from "lucide-react";
import { toast } from "sonner";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Skeleton } from "@/components/ui/skeleton";
import { errorMessage } from "@/lib/api/errors";
import { createTour, updateTour, type TourScope } from "@/lib/api/tours";
import type { TourDetail } from "@/types/tour";
import { TourContentForm } from "./form/TourContentForm";
import { changeNeedsReview, contentLocked, tourKeys, useManagedTour } from "./manage/tourQueries";

/** Trang tạo tour mới (id không truyền) hoặc sửa nội dung tour. */
export function TourEditorView({ scope, id, basePath }: { scope: TourScope; id?: number; basePath: string }) {
  if (id === undefined) return <Editor scope={scope} basePath={basePath} />;
  return <EditExisting scope={scope} id={id} basePath={basePath} />;
}

function EditExisting({ scope, id, basePath }: { scope: TourScope; id: number; basePath: string }) {
  const { data: tour, isPending, error } = useManagedTour(scope, id);
  if (isPending) return <Skeleton className="h-96 rounded-2xl" />;
  if (error) return <p className="text-destructive">{errorMessage(error)}</p>;
  if (contentLocked(tour)) {
    return (
      <Alert className="rounded-2xl">
        <AlertTitle>Không thể sửa nội dung lúc này</AlertTitle>
        <AlertDescription>
          {tour.status === "PENDING_APPROVAL"
            ? "Tour đang chờ duyệt — hãy rút lại yêu cầu duyệt trước khi chỉnh sửa."
            : "Tour đang bị đình chỉ."}{" "}
          <Link href={`${basePath}/${id}`} className="font-medium underline">
            Quay lại trang tour
          </Link>
        </AlertDescription>
      </Alert>
    );
  }
  return <Editor scope={scope} basePath={basePath} tour={tour} />;
}

function Editor({ scope, basePath, tour }: { scope: TourScope; basePath: string; tour?: TourDetail }) {
  const router = useRouter();
  const queryClient = useQueryClient();
  const backHref = tour ? `${basePath}/${tour.id}` : basePath;

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-col gap-3">
        <Link href={backHref} className="flex w-fit items-center gap-1.5 text-sm text-muted-foreground hover:text-foreground">
          <ArrowLeft className="size-4" /> {tour ? "Trang tour" : "Danh sách tour"}
        </Link>
        <div>
          <h1 className="text-2xl font-bold tracking-tight">{tour ? "Sửa nội dung tour" : "Tạo tour mới"}</h1>
          <p className="text-muted-foreground">
            {tour
              ? tour.title
              : "Nhập nội dung tour. Sau khi lưu, bạn thêm ảnh và lịch khởi hành rồi " +
                (scope === "agent" ? "gửi duyệt." : "công khai.")}
          </p>
        </div>
      </div>

      <TourContentForm
        tour={tour}
        cancelHref={backHref}
        submitLabel={tour ? "Lưu thay đổi" : "Lưu và tiếp tục"}
        notice={
          tour && changeNeedsReview(scope, tour) ? (
            <Alert className="rounded-2xl border-amber-200 bg-amber-50">
              <AlertTitle>Tour đang {tour.status === "PUBLISHED" ? "bán" : "tạm ẩn"}</AlertTitle>
              <AlertDescription>
                Lưu thay đổi nội dung sẽ chuyển tour về Nháp (tạm ẩn khỏi trang khách) và bạn cần gửi duyệt lại. Lịch khởi hành và khách
                đã đặt vẫn giữ nguyên.
              </AlertDescription>
            </Alert>
          ) : undefined
        }
        onSubmit={async (data) => {
          const saved = tour ? await updateTour(scope, tour.id, data) : await createTour(scope, data);
          queryClient.setQueryData(tourKeys.detail(scope, saved.id), saved);
          await queryClient.invalidateQueries({ queryKey: tourKeys.scope(scope) });
          toast.success(tour ? "Đã lưu nội dung tour" : "Đã tạo tour. Tiếp theo: thêm ảnh và lịch khởi hành.");
          router.push(`${basePath}/${saved.id}`);
        }}
      />
    </div>
  );
}
