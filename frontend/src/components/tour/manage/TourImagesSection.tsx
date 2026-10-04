"use client";

import { useRef, useState } from "react";
import Image from "next/image";
import { ArrowLeft, ArrowRight, ImagePlus, Star, Trash2 } from "lucide-react";
import { toast } from "sonner";
import { ConfirmDialog } from "@/components/common/ConfirmDialog";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardAction, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Spinner } from "@/components/ui/spinner";
import { errorMessage } from "@/lib/api/errors";
import { deleteTourImage, reorderTourImages, uploadTourImage, type TourScope } from "@/lib/api/tours";
import { ACCEPTED_IMAGE_TYPES, TOUR_RULES } from "@/lib/tour/labels";
import type { TourDetail, TourImage } from "@/types/tour";
import { changeNeedsReview, contentLocked, useTourRefresh } from "./tourQueries";

function checkImage(file: File): string | null {
  if (!ACCEPTED_IMAGE_TYPES.includes(file.type)) return `${file.name}: chỉ nhận ảnh JPG, PNG hoặc WebP`;
  if (file.size > TOUR_RULES.maxImageBytes) return `${file.name}: ảnh vượt quá 5 MB`;
  return null;
}

/** Ảnh tour: tải nhiều ảnh một lúc, sắp xếp (ảnh đầu là ảnh bìa), xóa. */
export function TourImagesSection({ scope, tour }: { scope: TourScope; tour: TourDetail }) {
  const refresh = useTourRefresh(scope, tour.id);
  const inputRef = useRef<HTMLInputElement>(null);
  const [uploading, setUploading] = useState<{ done: number; total: number } | null>(null);
  const [pendingFiles, setPendingFiles] = useState<File[] | null>(null);
  const [deleting, setDeleting] = useState<TourImage | null>(null);
  const [reordering, setReordering] = useState(false);

  const images = tour.images;
  const locked = contentLocked(tour);
  const slots = TOUR_RULES.maxImages - images.length;
  const live = tour.status === "PUBLISHED" || tour.status === "HIDDEN";

  const upload = async (files: File[]) => {
    const errors = files.map(checkImage).filter((e): e is string => e !== null);
    const valid = files.filter((f) => checkImage(f) === null).slice(0, slots);
    if (files.length - errors.length > slots) errors.push(`Chỉ tải thêm được ${slots} ảnh (tối đa ${TOUR_RULES.maxImages})`);
    errors.forEach((e) => toast.error(e));
    if (valid.length === 0) return;

    setUploading({ done: 0, total: valid.length });
    let ok = 0;
    // Tải lần lượt từng ảnh: Backend khóa tour khi thêm ảnh, gửi song song cũng phải chờ nhau
    for (const file of valid) {
      try {
        await uploadTourImage(scope, tour.id, file);
        ok++;
      } catch (error) {
        toast.error(`${file.name}: ${errorMessage(error)}`);
      }
      setUploading({ done: ok, total: valid.length });
    }
    setUploading(null);
    if (ok > 0) toast.success(`Đã tải lên ${ok} ảnh`);
    await refresh();
  };

  const onPick = (fileList: FileList | null) => {
    const files = Array.from(fileList ?? []);
    if (inputRef.current) inputRef.current.value = "";
    if (files.length === 0) return;
    // Thêm ảnh mới vào tour đang bán -> phải duyệt lại -> hỏi trước
    if (changeNeedsReview(scope, tour)) setPendingFiles(files);
    else void upload(files);
  };

  const reorder = async (ids: number[]) => {
    setReordering(true);
    try {
      await reorderTourImages(scope, tour.id, ids);
      await refresh();
    } catch (error) {
      toast.error(errorMessage(error));
    } finally {
      setReordering(false);
    }
  };

  const move = (index: number, step: number) => {
    const ids = images.map((i) => i.id);
    [ids[index], ids[index + step]] = [ids[index + step], ids[index]];
    void reorder(ids);
  };

  const makeCover = (index: number) => {
    const ids = images.map((i) => i.id);
    const [picked] = ids.splice(index, 1);
    void reorder([picked, ...ids]);
  };

  const confirmDelete = async () => {
    if (!deleting) return;
    try {
      await deleteTourImage(scope, tour.id, deleting.id);
      toast.success("Đã xóa ảnh");
      await refresh();
    } catch (error) {
      toast.error(errorMessage(error));
      throw error;
    }
  };

  return (
    <Card className="rounded-2xl">
      <CardHeader>
        <CardTitle className="text-lg">
          Hình ảnh{" "}
          <span className="text-sm font-normal text-muted-foreground">
            ({images.length}/{TOUR_RULES.maxImages})
          </span>
        </CardTitle>
        <CardDescription>
          Cần {TOUR_RULES.minImages}–{TOUR_RULES.maxImages} ảnh JPG, PNG hoặc WebP (≤ 5 MB). Ảnh đầu tiên là ảnh bìa.
        </CardDescription>
        {!locked && slots > 0 && (
          <CardAction>
            <input
              ref={inputRef}
              type="file"
              accept={ACCEPTED_IMAGE_TYPES.join(",")}
              multiple
              hidden
              onChange={(e) => onPick(e.target.files)}
            />
            <Button size="sm" className="rounded-lg" disabled={uploading !== null} onClick={() => inputRef.current?.click()}>
              {uploading ? <Spinner /> : <ImagePlus />}
              {uploading ? `Đang tải ${uploading.done}/${uploading.total}` : "Thêm ảnh"}
            </Button>
          </CardAction>
        )}
      </CardHeader>
      <CardContent>
        {images.length === 0 ? (
          <button
            type="button"
            disabled={locked}
            onClick={() => inputRef.current?.click()}
            className="flex w-full flex-col items-center gap-2 rounded-2xl border-2 border-dashed p-10 text-muted-foreground hover:bg-accent/40 disabled:cursor-not-allowed"
          >
            <ImagePlus className="size-8" />
            <span>Chưa có ảnh — bấm để chọn ảnh (chọn được nhiều ảnh cùng lúc)</span>
          </button>
        ) : (
          <ul className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-4">
            {images.map((image, index) => (
              <li key={image.id} className="group relative overflow-hidden rounded-xl border bg-muted">
                <div className="relative aspect-[4/3]">
                  <Image src={image.url} alt={`Ảnh ${index + 1}`} fill sizes="(min-width: 1024px) 20vw, 45vw" className="object-cover" />
                </div>
                {index === 0 && (
                  <Badge className="absolute top-2 left-2 border-0 bg-primary">
                    <Star /> Ảnh bìa
                  </Badge>
                )}
                {!locked && (
                  <div className="flex items-center justify-between gap-1 border-t bg-card p-1.5">
                    <div className="flex gap-1">
                      <Button
                        size="icon"
                        variant="ghost"
                        className="size-8 rounded-lg"
                        disabled={index === 0 || reordering}
                        onClick={() => move(index, -1)}
                        aria-label="Chuyển lên trước"
                      >
                        <ArrowLeft />
                      </Button>
                      <Button
                        size="icon"
                        variant="ghost"
                        className="size-8 rounded-lg"
                        disabled={index === images.length - 1 || reordering}
                        onClick={() => move(index, 1)}
                        aria-label="Chuyển ra sau"
                      >
                        <ArrowRight />
                      </Button>
                      {index > 0 && (
                        <Button
                          size="icon"
                          variant="ghost"
                          className="size-8 rounded-lg"
                          disabled={reordering}
                          onClick={() => makeCover(index)}
                          aria-label="Đặt làm ảnh bìa"
                          title="Đặt làm ảnh bìa"
                        >
                          <Star />
                        </Button>
                      )}
                    </div>
                    <Button
                      size="icon"
                      variant="ghost"
                      className="size-8 rounded-lg text-destructive hover:text-destructive"
                      disabled={live && images.length <= TOUR_RULES.minImages}
                      title={live && images.length <= TOUR_RULES.minImages ? "Tour đang bán phải còn ít nhất 3 ảnh" : "Xóa ảnh"}
                      onClick={() => setDeleting(image)}
                      aria-label="Xóa ảnh"
                    >
                      <Trash2 />
                    </Button>
                  </div>
                )}
              </li>
            ))}
          </ul>
        )}
      </CardContent>

      <ConfirmDialog
        open={pendingFiles !== null}
        onOpenChange={(open) => !open && setPendingFiles(null)}
        title="Thêm ảnh mới cần duyệt lại"
        description="Tour đang bán sẽ tạm ẩn khỏi trang khách và chuyển về Nháp. Bạn cần gửi duyệt lại sau khi chỉnh sửa xong. Các lịch khởi hành và khách đã đặt vẫn giữ nguyên."
        confirmLabel="Đồng ý, tải ảnh lên"
        onConfirm={async () => {
          const files = pendingFiles ?? [];
          setPendingFiles(null);
          void upload(files);
        }}
      />
      <ConfirmDialog
        open={deleting !== null}
        onOpenChange={(open) => !open && setDeleting(null)}
        title="Xóa ảnh này?"
        description="Ảnh sẽ bị xóa vĩnh viễn."
        confirmLabel="Xóa ảnh"
        destructive
        onConfirm={confirmDelete}
      />
    </Card>
  );
}
