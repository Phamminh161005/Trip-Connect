"use client";

import { useState, type ReactNode } from "react";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Spinner } from "@/components/ui/spinner";

interface ConfirmDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  title: string;
  description: ReactNode;
  confirmLabel: string;
  destructive?: boolean;
  /** Hộp thoại tự đóng khi xong; lỗi thì giữ nguyên để người dùng thử lại. */
  onConfirm: () => Promise<void>;
}

/** Hộp thoại xác nhận trước các thao tác quan trọng (nộp hồ sơ, xóa giấy tờ...). */
export function ConfirmDialog({ open, onOpenChange, title, description, confirmLabel, destructive, onConfirm }: ConfirmDialogProps) {
  const [pending, setPending] = useState(false);

  const handleConfirm = async () => {
    setPending(true);
    try {
      await onConfirm();
      onOpenChange(false);
    } catch {
      // Lỗi đã được thông báo ở nơi gọi
    } finally {
      setPending(false);
    }
  };

  return (
    <Dialog open={open} onOpenChange={(next) => !pending && onOpenChange(next)}>
      <DialogContent className="rounded-2xl sm:max-w-md">
        <DialogHeader>
          <DialogTitle>{title}</DialogTitle>
          <DialogDescription>{description}</DialogDescription>
        </DialogHeader>
        <DialogFooter>
          <Button variant="outline" className="rounded-xl" disabled={pending} onClick={() => onOpenChange(false)}>
            Hủy
          </Button>
          <Button variant={destructive ? "destructive" : "default"} className="rounded-xl" disabled={pending} onClick={handleConfirm}>
            {pending && <Spinner />}
            {confirmLabel}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
