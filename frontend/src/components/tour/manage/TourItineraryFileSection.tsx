"use client";

import { useRef, useState } from "react";
import { Eye, FileText, Trash2, Upload } from "lucide-react";
import { toast } from "sonner";
import { ConfirmDialog } from "@/components/common/ConfirmDialog";
import { Button } from "@/components/ui/button";
import { Card, CardAction, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Spinner } from "@/components/ui/spinner";
import { openTemporaryDocument } from "@/lib/agent/documents";
import { errorMessage } from "@/lib/api/errors";
import { deleteItineraryFile, getManagedItineraryFileUrl, uploadItineraryFile, type TourScope } from "@/lib/api/tours";
import { formatFileSize } from "@/lib/format";
import { TOUR_RULES } from "@/lib/tour/labels";
import type { TourDetail } from "@/types/tour";
import { changeNeedsReview, contentLocked, useTourRefresh } from "./tourQueries";

/** File chương trình tour (PDF, không bắt buộc) — khách tải về để in / lưu. */
export function TourItineraryFileSection({ scope, tour }: { scope: TourScope; tour: TourDetail }) {
  const refresh = useTourRefresh(scope, tour.id);
  const inputRef = useRef<HTMLInputElement>(null);
  const [uploading, setUploading] = useState(false);
  const [pendingFile, setPendingFile] = useState<File | null>(null);
  const [confirmDelete, setConfirmDelete] = useState(false);
  const locked = contentLocked(tour);

  const upload = async (file: File) => {
    if (file.type !== "application/pdf") return toast.error("Chỉ nhận file PDF");
    if (file.size > TOUR_RULES.maxPdfBytes) return toast.error("File vượt quá 10 MB");
    setUploading(true);
    try {
      await uploadItineraryFile(scope, tour.id, file);
      toast.success("Đã tải file chương trình tour");
      await refresh();
    } catch (error) {
      toast.error(errorMessage(error));
    } finally {
      setUploading(false);
    }
  };

  const onPick = (file: File | undefined) => {
    if (inputRef.current) inputRef.current.value = "";
    if (!file) return;
    if (changeNeedsReview(scope, tour)) setPendingFile(file);
    else void upload(file);
  };

  return (
    <Card className="rounded-2xl">
      <CardHeader>
        <CardTitle className="text-lg">File chương trình tour (không bắt buộc)</CardTitle>
        <CardDescription>File PDF ≤ 10 MB để khách tải về in hoặc lưu. Không thay thế lịch trình đã nhập.</CardDescription>
        {!locked && (
          <CardAction>
            <input ref={inputRef} type="file" accept="application/pdf" hidden onChange={(e) => onPick(e.target.files?.[0])} />
            <Button size="sm" variant="outline" className="rounded-lg" disabled={uploading} onClick={() => inputRef.current?.click()}>
              {uploading ? <Spinner /> : <Upload />}
              {tour.itineraryFile ? "Thay file" : "Tải lên"}
            </Button>
          </CardAction>
        )}
      </CardHeader>
      <CardContent>
        {tour.itineraryFile ? (
          <div className="flex flex-wrap items-center justify-between gap-2 rounded-xl bg-muted/50 px-3 py-2">
            <p className="flex min-w-0 items-center gap-2 text-sm">
              <FileText className="size-4 shrink-0 text-primary" />
              <span className="truncate">{tour.itineraryFile.fileName}</span>
              <span className="shrink-0 text-muted-foreground">· {formatFileSize(tour.itineraryFile.sizeBytes)}</span>
            </p>
            <div className="flex gap-1">
              <Button
                size="sm"
                variant="ghost"
                className="rounded-lg"
                onClick={() => openTemporaryDocument(() => getManagedItineraryFileUrl(scope, tour.id))}
              >
                <Eye /> Xem
              </Button>
              {!locked && (
                <Button size="sm" variant="ghost" className="rounded-lg text-destructive" onClick={() => setConfirmDelete(true)}>
                  <Trash2 /> Xóa
                </Button>
              )}
            </div>
          </div>
        ) : (
          <p className="text-sm text-muted-foreground">Chưa có file.</p>
        )}
      </CardContent>

      <ConfirmDialog
        open={pendingFile !== null}
        onOpenChange={(open) => !open && setPendingFile(null)}
        title="Thay file chương trình cần duyệt lại"
        description="Tour đang bán sẽ tạm ẩn khỏi trang khách và chuyển về Nháp. Bạn cần gửi duyệt lại sau khi chỉnh sửa xong."
        confirmLabel="Đồng ý, tải lên"
        onConfirm={async () => {
          const file = pendingFile;
          setPendingFile(null);
          if (file) void upload(file);
        }}
      />
      <ConfirmDialog
        open={confirmDelete}
        onOpenChange={setConfirmDelete}
        title="Xóa file chương trình tour?"
        description="Khách sẽ không tải được file này nữa. Lịch trình đã nhập vẫn giữ nguyên."
        confirmLabel="Xóa file"
        destructive
        onConfirm={async () => {
          try {
            await deleteItineraryFile(scope, tour.id);
            toast.success("Đã xóa file");
            await refresh();
          } catch (error) {
            toast.error(errorMessage(error));
            throw error;
          }
        }}
      />
    </Card>
  );
}
