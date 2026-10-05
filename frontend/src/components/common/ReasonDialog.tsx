"use client";

import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Field, FieldDescription, FieldError, FieldLabel } from "@/components/ui/field";
import { Spinner } from "@/components/ui/spinner";
import { Textarea } from "@/components/ui/textarea";

const MAX_LENGTH = 1000; // khớp @Size(max = 1000) ở Backend

/** Hộp thoại bắt buộc nhập lý do (từ chối hồ sơ, từ chối yêu cầu cập nhật, vô hiệu hóa tài khoản). */
export function ReasonDialog({
  open,
  onOpenChange,
  title,
  description,
  label,
  placeholder,
  confirmLabel,
  hint = "Nội dung này được gửi kèm trong email tới người dùng.",
  emptyMessage = "Vui lòng nhập lý do",
  onConfirm,
}: {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  title: string;
  description: string;
  label: string;
  placeholder?: string;
  confirmLabel: string;
  /** Dòng gợi ý dưới ô nhập. */
  hint?: string;
  emptyMessage?: string;
  /** Lỗi thì ném ra để hộp thoại giữ nguyên, người dùng sửa và thử lại. */
  onConfirm: (reason: string) => Promise<void>;
}) {
  const [reason, setReason] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [pending, setPending] = useState(false);

  const close = (next: boolean) => {
    if (pending) return;
    onOpenChange(next);
    if (!next) {
      setReason("");
      setError(null);
    }
  };

  const submit = async () => {
    const trimmed = reason.trim();
    if (!trimmed) return setError(emptyMessage);
    if (trimmed.length > MAX_LENGTH) return setError(`Tối đa ${MAX_LENGTH} ký tự`);
    setPending(true);
    try {
      await onConfirm(trimmed);
      setPending(false);
      close(false);
    } catch {
      setPending(false);
    }
  };

  return (
    <Dialog open={open} onOpenChange={close}>
      <DialogContent className="rounded-2xl sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>{title}</DialogTitle>
          <DialogDescription>{description}</DialogDescription>
        </DialogHeader>
        <Field data-invalid={Boolean(error)}>
          <FieldLabel htmlFor="reason">{label}</FieldLabel>
          <Textarea
            id="reason"
            rows={4}
            value={reason}
            placeholder={placeholder}
            aria-invalid={Boolean(error)}
            onChange={(e) => {
              setReason(e.target.value);
              setError(null);
            }}
            className="rounded-xl text-base"
          />
          <FieldDescription>{hint}</FieldDescription>
          {error && <FieldError>{error}</FieldError>}
        </Field>
        <DialogFooter>
          <Button variant="outline" className="rounded-xl" disabled={pending} onClick={() => close(false)}>
            Hủy
          </Button>
          <Button variant="destructive" className="rounded-xl" disabled={pending} onClick={submit}>
            {pending && <Spinner />}
            {confirmLabel}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
