"use client";

import { useEffect, useRef, useState } from "react";
import Image from "next/image";
import { ImagePlus, X } from "lucide-react";
import { toast } from "sonner";
import { StarInput } from "@/components/review/Stars";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Field, FieldDescription, FieldError, FieldLabel } from "@/components/ui/field";
import { Spinner } from "@/components/ui/spinner";
import { Textarea } from "@/components/ui/textarea";
import { errorMessage } from "@/lib/api/errors";
import { createReview, deleteReviewImage, updateReview, uploadReviewImage } from "@/lib/api/reviews";
import type { MyReview } from "@/types/review";

// Khớp ReviewRules / ReviewRequests của Backend
const MAX_IMAGES = 5;
const MAX_IMAGE_BYTES = 5 * 1024 * 1024;
const MIN_COMMENT = 20;
const MAX_COMMENT = 2000;

interface PendingImage {
  file: File;
  preview: string;
}

/** Viết mới hoặc sửa đánh giá. Ảnh mới được tải lên sau khi lưu nội dung; ảnh cũ xóa ngay khi bấm X. */
export function ReviewDialog({
  bookingId,
  tourTitle,
  review,
  onClose,
  onSaved,
}: {
  bookingId: number;
  tourTitle: string;
  review: MyReview | null;
  onClose: () => void;
  onSaved: () => void;
}) {
  const [rating, setRating] = useState(review?.rating ?? 0);
  const [comment, setComment] = useState(review?.comment ?? "");
  const [existing, setExisting] = useState(review?.images ?? []);
  const [pending, setPending] = useState<PendingImage[]>([]);
  const [errors, setErrors] = useState<{ rating?: string; comment?: string }>({});
  const [saving, setSaving] = useState(false);
  const fileInput = useRef<HTMLInputElement>(null);
  // Link xem trước (blob:) đang dùng — giải phóng khi đóng hộp thoại
  const previews = useRef(new Set<string>());
  useEffect(() => {
    const urls = previews.current;
    return () => urls.forEach((url) => URL.revokeObjectURL(url));
  }, []);

  const slotsLeft = MAX_IMAGES - existing.length - pending.length;

  const addFiles = (files: FileList | null) => {
    if (!files) return;
    const accepted: PendingImage[] = [];
    for (const file of Array.from(files)) {
      if (accepted.length >= slotsLeft) {
        toast.error(`Mỗi đánh giá có tối đa ${MAX_IMAGES} ảnh`);
        break;
      }
      if (!file.type.startsWith("image/")) toast.error(`${file.name} không phải ảnh`);
      else if (file.size > MAX_IMAGE_BYTES) toast.error(`${file.name} lớn hơn 5MB`);
      else {
        const preview = URL.createObjectURL(file);
        previews.current.add(preview);
        accepted.push({ file, preview });
      }
    }
    setPending((list) => [...list, ...accepted]);
  };

  const removePending = (index: number) => {
    URL.revokeObjectURL(pending[index].preview);
    previews.current.delete(pending[index].preview);
    setPending((list) => list.filter((_, i) => i !== index));
  };

  const removeExisting = async (imageId: number) => {
    if (!review) return;
    try {
      await deleteReviewImage(review.id, imageId);
      setExisting((list) => list.filter((i) => i.id !== imageId));
    } catch (error) {
      toast.error(errorMessage(error));
    }
  };

  const save = async () => {
    const text = comment.trim();
    const nextErrors = {
      rating: rating === 0 ? "Vui lòng chọn số sao" : undefined,
      comment: text.length < MIN_COMMENT ? `Nhận xét cần ít nhất ${MIN_COMMENT} ký tự` : undefined,
    };
    setErrors(nextErrors);
    if (nextErrors.rating || nextErrors.comment) return;

    setSaving(true);
    try {
      const saved = review ? await updateReview(review.id, { rating, comment: text }) : await createReview(bookingId, { rating, comment: text });
      let failed = 0;
      for (const image of pending) {
        try {
          await uploadReviewImage(saved.id, image.file);
        } catch {
          failed++;
        }
      }
      if (failed > 0) toast.error(`Đã lưu đánh giá nhưng ${failed} ảnh tải lên không thành công`);
      else toast.success(review ? "Đã cập nhật đánh giá" : "Cảm ơn bạn đã đánh giá!");
      onSaved();
    } catch (error) {
      toast.error(errorMessage(error));
      setSaving(false);
    }
  };

  return (
    <Dialog open onOpenChange={(open) => !open && !saving && onClose()}>
      <DialogContent className="max-h-[90vh] overflow-y-auto rounded-2xl sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>{review ? "Sửa đánh giá" : "Đánh giá chuyến đi"}</DialogTitle>
          <DialogDescription className="line-clamp-2">{tourTitle}</DialogDescription>
        </DialogHeader>

        <div className="flex flex-col gap-5">
          <Field data-invalid={!!errors.rating}>
            <FieldLabel>Bạn chấm chuyến đi mấy sao?</FieldLabel>
            <StarInput value={rating} onChange={setRating} invalid={!!errors.rating} />
            {errors.rating && <FieldError>{errors.rating}</FieldError>}
          </Field>

          <Field data-invalid={!!errors.comment}>
            <FieldLabel htmlFor="review-comment">Nhận xét</FieldLabel>
            <Textarea
              id="review-comment"
              value={comment}
              onChange={(e) => setComment(e.target.value)}
              rows={5}
              maxLength={MAX_COMMENT}
              placeholder="Lịch trình, hướng dẫn viên, khách sạn, ăn uống... có gì đáng khen hoặc cần cải thiện?"
              aria-invalid={!!errors.comment}
              className="rounded-xl text-base"
            />
            <FieldDescription className="flex justify-between">
              <span>Ít nhất {MIN_COMMENT} ký tự</span>
              <span className="tabular-nums">
                {comment.trim().length}/{MAX_COMMENT}
              </span>
            </FieldDescription>
            {errors.comment && <FieldError>{errors.comment}</FieldError>}
          </Field>

          <Field>
            <FieldLabel>Ảnh (không bắt buộc, tối đa {MAX_IMAGES} ảnh)</FieldLabel>
            <div className="flex flex-wrap gap-2">
              {existing.map((image) => (
                <Thumb key={image.id} src={image.url} onRemove={() => removeExisting(image.id)} disabled={saving} />
              ))}
              {pending.map((image, index) => (
                <Thumb key={image.preview} src={image.preview} onRemove={() => removePending(index)} disabled={saving} />
              ))}
              {slotsLeft > 0 && (
                <button
                  type="button"
                  onClick={() => fileInput.current?.click()}
                  disabled={saving}
                  className="flex size-20 flex-col items-center justify-center gap-1 rounded-lg border border-dashed text-xs text-muted-foreground hover:bg-accent"
                >
                  <ImagePlus className="size-5" /> Thêm ảnh
                </button>
              )}
            </div>
            <input
              ref={fileInput}
              type="file"
              accept="image/jpeg,image/png,image/webp"
              multiple
              hidden
              onChange={(e) => {
                addFiles(e.target.files);
                e.target.value = "";
              }}
            />
          </Field>
          {review && <p className="text-xs text-muted-foreground">Bạn sửa được đánh giá trong 7 ngày sau khi gửi.</p>}
        </div>

        <DialogFooter>
          <Button variant="outline" className="rounded-xl" disabled={saving} onClick={onClose}>
            Hủy
          </Button>
          <Button className="rounded-xl" disabled={saving} onClick={save}>
            {saving && <Spinner />} {review ? "Lưu thay đổi" : "Gửi đánh giá"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

function Thumb({ src, onRemove, disabled }: { src: string; onRemove: () => void; disabled: boolean }) {
  return (
    <div className="relative size-20 overflow-hidden rounded-lg bg-muted">
      <Image src={src} alt="" fill sizes="80px" className="object-cover" unoptimized={src.startsWith("blob:")} />
      <button
        type="button"
        onClick={onRemove}
        disabled={disabled}
        aria-label="Bỏ ảnh"
        className="absolute top-1 right-1 flex size-6 items-center justify-center rounded-full bg-black/60 text-white hover:bg-black/80"
      >
        <X className="size-3.5" />
      </button>
    </div>
  );
}
