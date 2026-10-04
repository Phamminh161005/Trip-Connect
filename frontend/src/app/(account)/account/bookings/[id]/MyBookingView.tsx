"use client";

import { useState } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { useFieldArray, useForm, useWatch, type Path } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { CreditCard, UserPen, XCircle } from "lucide-react";
import { toast } from "sonner";
import { BookingDetailView } from "@/components/booking/BookingDetailView";
import { PassengerSlotsFields, type PassengerSlotsValues } from "@/components/booking/PassengerSlotsFields";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Skeleton } from "@/components/ui/skeleton";
import { Spinner } from "@/components/ui/spinner";
import { Textarea } from "@/components/ui/textarea";
import { ApiError, errorMessage } from "@/lib/api/errors";
import { cancelMyBooking, getCancellationQuote, getMyBooking, payBooking, updateBookingPassengers } from "@/lib/api/bookings";
import { checkSlots, passengerSlotSchema, slotsFromBooking, slotsToRequest } from "@/lib/booking/passengerSlots";
import { focusNextOnEnter } from "@/lib/form/focusNextOnEnter";
import { formatDay, formatPrice } from "@/lib/tour/labels";
import type { BookingDetail } from "@/types/booking";
import { MY_BOOKINGS_KEY } from "../MyBookingList";

export function MyBookingView({ id }: { id: number }) {
  const key = [...MY_BOOKINGS_KEY, id];
  const query = useQuery({ queryKey: key, queryFn: () => getMyBooking(id) });
  const queryClient = useQueryClient();
  const [paying, setPaying] = useState(false);
  const [cancelOpen, setCancelOpen] = useState(false);
  const [passengersOpen, setPassengersOpen] = useState(false);

  if (query.isPending) return <Skeleton className="h-96 rounded-2xl" />;
  if (query.isError) return <p className="text-destructive">{errorMessage(query.error)}</p>;
  const booking = query.data;

  const pay = async () => {
    setPaying(true);
    try {
      const { paymentUrl } = await payBooking(booking.id);
      window.location.assign(paymentUrl);
    } catch (error) {
      toast.error(errorMessage(error));
      setPaying(false);
      await query.refetch();
    }
  };

  return (
    <>
      <BookingDetailView
        booking={booking}
        backHref="/account/bookings"
        backLabel="Đơn đặt của tôi"
        onHoldExpire={() => queryClient.invalidateQueries({ queryKey: MY_BOOKINGS_KEY })}
        passengerAction={
          booking.canEditPassengers && (
            <Button variant="outline" className="rounded-xl" onClick={() => setPassengersOpen(true)}>
              <UserPen /> Sửa danh sách
            </Button>
          )
        }
        actions={
          <>
            {booking.canPay && (
              <Button className="rounded-xl" disabled={paying} onClick={pay}>
                {paying ? <Spinner /> : <CreditCard />} Thanh toán {formatPrice(booking.totalAmount)}
              </Button>
            )}
            {booking.canCancel && (
              <Button variant="outline" className="rounded-xl text-destructive hover:text-destructive" onClick={() => setCancelOpen(true)}>
                <XCircle /> Hủy đơn
              </Button>
            )}
          </>
        }
      />
      {passengersOpen && (
        <PassengerListDialog
          booking={booking}
          onClose={() => setPassengersOpen(false)}
          onDone={(updated) => {
            queryClient.setQueryData(key, updated);
            setPassengersOpen(false);
          }}
        />
      )}
      {cancelOpen && (
        <CancelDialog
          booking={booking}
          onClose={() => setCancelOpen(false)}
          onDone={(updated) => {
            queryClient.setQueryData(key, updated);
            void queryClient.invalidateQueries({ queryKey: MY_BOOKINGS_KEY });
            setCancelOpen(false);
          }}
        />
      )}
    </>
  );
}

const passengerListSchema = z.object({ passengers: z.array(passengerSlotSchema) });

/** Sửa danh sách hành khách — vẫn đủ đúng số khách từng loại đã đặt. */
function PassengerListDialog({ booking, onClose, onDone }: { booking: BookingDetail; onClose: () => void; onDone: (b: BookingDetail) => void }) {
  const form = useForm<PassengerSlotsValues>({
    resolver: zodResolver(
      passengerListSchema.superRefine((v, ctx) =>
        checkSlots(v.passengers, { startDate: booking.startDate, international: booking.international }, ctx, ["passengers"]),
      ),
    ),
    defaultValues: { passengers: slotsFromBooking(booking, booking.passengers) },
  });
  const { fields } = useFieldArray({ control: form.control, name: "passengers" });
  const slots = useWatch({ control: form.control, name: "passengers" });
  const submitting = form.formState.isSubmitting;

  const submit = async (values: PassengerSlotsValues) => {
    try {
      const updated = await updateBookingPassengers(booking.id, slotsToRequest(values.passengers));
      toast.success("Đã lưu danh sách hành khách");
      onDone(updated);
    } catch (error) {
      if (error instanceof ApiError && error.hasFieldErrors) {
        Object.entries(error.fieldErrors).forEach(([field, message]) =>
          form.setError(field.replace(/\[(\d+)]/g, ".$1") as Path<PassengerSlotsValues>, { type: "server", message }),
        );
      }
      toast.error(errorMessage(error));
    }
  };

  return (
    <Dialog open onOpenChange={(open) => !open && !submitting && onClose()}>
      <DialogContent className="max-h-[90vh] overflow-y-auto rounded-2xl sm:max-w-2xl">
        <DialogHeader>
          <DialogTitle>Danh sách hành khách</DialogTitle>
          <DialogDescription>
            Nhập đúng họ tên, ngày sinh như giấy tờ tùy thân
            {booking.international ? " và số hộ chiếu" : ""}. Loại khách tính theo tuổi vào ngày đi. Sửa được tới hết ngày{" "}
            {formatDay(booking.passengerListDeadline)}.
          </DialogDescription>
        </DialogHeader>
        <form id="passenger-list-form" noValidate onKeyDown={focusNextOnEnter} onSubmit={form.handleSubmit(submit)}>
          <PassengerSlotsFields
            control={form.control}
            fields={fields}
            slots={slots}
            startDate={booking.startDate}
            international={booking.international}
          />
        </form>
        <DialogFooter>
          <Button variant="outline" className="rounded-xl" disabled={submitting} onClick={onClose}>
            Đóng
          </Button>
          <Button type="submit" form="passenger-list-form" className="rounded-xl" disabled={submitting}>
            {submitting && <Spinner />} Lưu danh sách
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

/** Hộp thoại hủy: hỏi Backend số tiền được hoàn trước, khách đồng ý mới hủy. */
function CancelDialog({ booking, onClose, onDone }: { booking: BookingDetail; onClose: () => void; onDone: (b: BookingDetail) => void }) {
  const quote = useQuery({ queryKey: ["cancellation-quote", booking.id], queryFn: () => getCancellationQuote(booking.id), staleTime: 0 });
  const [reason, setReason] = useState("");
  const [submitting, setSubmitting] = useState(false);

  const confirm = async () => {
    setSubmitting(true);
    try {
      const updated = await cancelMyBooking(booking.id, reason.trim() || null);
      toast.success(updated.refundAmount > 0 ? `Đã hủy đơn. TripConnect sẽ hoàn ${formatPrice(updated.refundAmount)}.` : "Đã hủy đơn");
      onDone(updated);
    } catch (error) {
      toast.error(errorMessage(error));
      setSubmitting(false);
    }
  };

  return (
    <Dialog open onOpenChange={(open) => !open && !submitting && onClose()}>
      <DialogContent className="rounded-2xl sm:max-w-md">
        <DialogHeader>
          <DialogTitle>Hủy đơn {booking.code}?</DialogTitle>
          <DialogDescription>Đơn đã hủy không khôi phục được.</DialogDescription>
        </DialogHeader>
        {quote.isPending ? (
          <Skeleton className="h-20 rounded-xl" />
        ) : quote.isError ? (
          <p className="text-sm text-destructive">{errorMessage(quote.error)}</p>
        ) : (
          <div className="flex flex-col gap-3">
            <div className="rounded-xl bg-muted/60 p-3 text-sm">
              <p>{quote.data.explanation}</p>
              {quote.data.paidAmount > 0 && (
                <p className="mt-1 font-semibold">
                  Đã thanh toán {formatPrice(quote.data.paidAmount)} · được hoàn{" "}
                  <span className="text-primary">{formatPrice(quote.data.refundAmount)}</span>
                </p>
              )}
            </div>
            {quote.data.cancellable && (
              <Textarea
                value={reason}
                onChange={(e) => setReason(e.target.value)}
                rows={3}
                maxLength={500}
                placeholder="Lý do hủy (không bắt buộc)"
                aria-label="Lý do hủy"
                className="rounded-xl"
              />
            )}
          </div>
        )}
        <DialogFooter>
          <Button variant="outline" className="rounded-xl" disabled={submitting} onClick={onClose}>
            Giữ đơn
          </Button>
          <Button
            variant="destructive"
            className="rounded-xl"
            disabled={submitting || !quote.data?.cancellable}
            onClick={confirm}
          >
            {submitting && <Spinner />} Xác nhận hủy
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
