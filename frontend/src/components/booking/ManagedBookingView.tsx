"use client";

import { useState } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { Ban, CheckCheck, RotateCcw } from "lucide-react";
import { toast } from "sonner";
import { ReasonDialog } from "@/components/common/ReasonDialog";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Skeleton } from "@/components/ui/skeleton";
import { Spinner } from "@/components/ui/spinner";
import { errorMessage } from "@/lib/api/errors";
import { cancelBookingByAdmin, getManagedBooking, markRefundDone, retryRefund, type BookingScope } from "@/lib/api/bookings";
import { formatPrice } from "@/lib/tour/labels";
import { BookingDetailView } from "./BookingDetailView";

/** Chi tiết đơn cho Agent (chỉ xem) và Admin (hủy vì bất khả kháng, xử lý hoàn tiền thủ công). */
export function ManagedBookingView({ scope, id }: { scope: BookingScope; id: number }) {
  const key = [scope, "booking", id];
  const query = useQuery({ queryKey: key, queryFn: () => getManagedBooking(scope, id) });
  const queryClient = useQueryClient();
  const [cancelOpen, setCancelOpen] = useState(false);
  const [retrying, setRetrying] = useState<number | null>(null);
  const [manualFor, setManualFor] = useState<number | null>(null);
  const isAdmin = scope === "admin";

  if (query.isPending) return <Skeleton className="h-96 rounded-2xl" />;
  if (query.isError) return <p className="text-destructive">{errorMessage(query.error)}</p>;
  const booking = query.data;

  // Làm mới chi tiết + danh sách + số việc chờ trên menu Admin
  const refresh = () => queryClient.invalidateQueries({ queryKey: [scope] });

  const run = async (task: () => Promise<unknown>, success: string) => {
    try {
      await task();
      toast.success(success);
      await refresh();
    } catch (error) {
      toast.error(errorMessage(error));
      throw error;
    }
  };

  const cancellable = booking.status === "PAID" || booking.status === "PENDING_PAYMENT";

  return (
    <>
      <BookingDetailView
        booking={booking}
        staff
        backHref={`/${scope}/bookings`}
        backLabel="Đơn đặt tour"
        actions={
          isAdmin &&
          cancellable && (
            <Button variant="outline" className="rounded-xl text-destructive hover:text-destructive" onClick={() => setCancelOpen(true)}>
              <Ban /> Hủy vì bất khả kháng
            </Button>
          )
        }
        refundActions={(refundId) => {
          const refund = booking.refunds.find((r) => r.id === refundId);
          if (!isAdmin || refund?.status !== "MANUAL_REQUIRED") return null;
          return (
            <div className="flex flex-wrap gap-2">
              <Button
                size="sm"
                variant="outline"
                className="rounded-lg"
                disabled={retrying === refundId}
                onClick={async () => {
                  setRetrying(refundId);
                  await run(() => retryRefund(refundId), "Đã gửi lại yêu cầu hoàn tiền qua VNPay").catch(() => {});
                  setRetrying(null);
                }}
              >
                {retrying === refundId ? <Spinner /> : <RotateCcw />} Thử hoàn lại qua VNPay
              </Button>
              <Button size="sm" className="rounded-lg" onClick={() => setManualFor(refundId)}>
                <CheckCheck /> Đã hoàn thủ công
              </Button>
            </div>
          );
        }}
      />

      <ReasonDialog
        open={cancelOpen}
        onOpenChange={setCancelOpen}
        title={`Hủy đơn ${booking.code}?`}
        description={`Dùng khi có sự cố bất khả kháng (thiên tai, dịch bệnh, đơn vị tổ chức vi phạm...). ${
          booking.status === "PAID" ? `Khách được hoàn 100% (${formatPrice(booking.totalAmount)}).` : "Đơn chưa thanh toán nên không phát sinh hoàn tiền."
        }`}
        label="Lý do hủy"
        placeholder="Ví dụ: Bão số 5 đổ bộ Quảng Ninh, cấm tàu ra vịnh."
        confirmLabel="Hủy đơn"
        onConfirm={(reason) => run(() => cancelBookingByAdmin(booking.id, reason), "Đã hủy đơn")}
      />
      {manualFor !== null && (
        <ManualRefundDialog
          onClose={() => setManualFor(null)}
          onConfirm={(note) => run(() => markRefundDone(manualFor, note), "Đã ghi nhận hoàn tiền thủ công")}
        />
      )}
    </>
  );
}

function ManualRefundDialog({ onClose, onConfirm }: { onClose: () => void; onConfirm: (note: string) => Promise<void> }) {
  const [note, setNote] = useState("");
  const [busy, setBusy] = useState(false);
  return (
    <Dialog open onOpenChange={(open) => !open && !busy && onClose()}>
      <DialogContent className="rounded-2xl sm:max-w-md">
        <DialogHeader>
          <DialogTitle>Xác nhận đã hoàn tiền thủ công</DialogTitle>
          <DialogDescription>Chỉ xác nhận sau khi đã chuyển khoản trả khách. Khách sẽ nhận email báo đã hoàn tiền.</DialogDescription>
        </DialogHeader>
        <Input
          value={note}
          onChange={(e) => setNote(e.target.value)}
          maxLength={500}
          placeholder="Ghi chú, vd mã tham chiếu chuyển khoản"
          aria-label="Ghi chú"
          className="h-11 rounded-xl"
        />
        <DialogFooter>
          <Button variant="outline" className="rounded-xl" disabled={busy} onClick={onClose}>
            Hủy
          </Button>
          <Button
            className="rounded-xl"
            disabled={busy}
            onClick={async () => {
              setBusy(true);
              try {
                await onConfirm(note);
                onClose();
              } catch {
                setBusy(false);
              }
            }}
          >
            {busy && <Spinner />} Xác nhận
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
