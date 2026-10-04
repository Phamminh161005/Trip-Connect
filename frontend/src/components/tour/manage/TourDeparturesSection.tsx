"use client";

import { useState } from "react";
import Link from "next/link";
import { Controller, useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Ban, CalendarPlus, MoreHorizontal, Pencil, PlayCircle, StopCircle, Trash2, Users } from "lucide-react";
import { toast } from "sonner";
import { ConfirmDialog } from "@/components/common/ConfirmDialog";
import { ReasonDialog } from "@/components/common/ReasonDialog";
import { PriceInput } from "@/components/form/PriceInput";
import { Button } from "@/components/ui/button";
import { Card, CardAction, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Field, FieldDescription, FieldError, FieldLabel } from "@/components/ui/field";
import { Input } from "@/components/ui/input";
import { Spinner } from "@/components/ui/spinner";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { ApiError, errorMessage } from "@/lib/api/errors";
import {
  addDeparture,
  cancelDeparture,
  closeDeparture,
  deleteDeparture,
  reopenDeparture,
  updateDeparture,
  type TourScope,
} from "@/lib/api/tours";
import { TOUR_RULES, formatDay, formatDayWithWeekday, formatPrice, localDateString } from "@/lib/tour/labels";
import { departureSchema, type DepartureValues } from "@/lib/validation/tour";
import type { TourDeparture, TourDetail } from "@/types/tour";
import { DepartureStatusBadge } from "../TourStatusBadge";
import { useTourRefresh } from "./tourQueries";
import { focusNextOnEnter } from "@/lib/form/focusNextOnEnter";

type Action = { kind: "delete" | "cancel"; departure: TourDeparture } | null;

/**
 * Lịch khởi hành: thêm / sửa / ngừng bán / mở bán lại / hủy chuyến / xóa.
 * Không cần Admin duyệt lại — Backend tự kiểm tra ngày đi, giá, lịch đã có khách.
 */
export function TourDeparturesSection({ scope, tour }: { scope: TourScope; tour: TourDetail }) {
  const refresh = useTourRefresh(scope, tour.id);
  const [editing, setEditing] = useState<TourDeparture | "new" | null>(null);
  const [action, setAction] = useState<Action>(null);
  const suspended = tour.status === "SUSPENDED";

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

  const upcoming = tour.departures.filter((d) => !d.departed);
  const past = tour.departures.filter((d) => d.departed);

  return (
    <Card className="rounded-2xl">
      <CardHeader>
        <CardTitle className="text-lg">Lịch khởi hành & giá</CardTitle>
        <CardDescription>
          Thêm lịch không cần duyệt lại. Ngày đi phải cách hôm nay ít nhất {TOUR_RULES.minDepartureLeadDays} ngày; lịch đã có khách
          đặt thì không đổi được ngày và giá.
        </CardDescription>
        {!suspended && (
          <CardAction>
            <Button size="sm" className="rounded-lg" onClick={() => setEditing("new")}>
              <CalendarPlus /> Thêm lịch
            </Button>
          </CardAction>
        )}
      </CardHeader>
      <CardContent>
        {tour.departures.length === 0 ? (
          <p className="rounded-xl border border-dashed p-6 text-center text-sm text-muted-foreground">
            Chưa có lịch khởi hành. Khách chỉ đặt được tour khi có ít nhất 1 lịch đang mở bán.
          </p>
        ) : (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Ngày đi – về</TableHead>
                <TableHead>Giá người lớn / trẻ em</TableHead>
                <TableHead className="hidden sm:table-cell">Chỗ</TableHead>
                <TableHead>Trạng thái</TableHead>
                <TableHead className="w-10" />
              </TableRow>
            </TableHeader>
            <TableBody>
              {[...upcoming, ...past].map((d) => (
                <TableRow key={d.id} className={d.departed || d.status === "CANCELLED" ? "text-muted-foreground" : undefined}>
                  <TableCell>
                    <p className="font-medium">{formatDayWithWeekday(d.startDate)}</p>
                    <p className="text-xs text-muted-foreground">về {formatDay(d.endDate)}</p>
                  </TableCell>
                  <TableCell>
                    <p className="font-medium">{formatPrice(d.adultPrice)}</p>
                    <p className="text-xs text-muted-foreground">Trẻ em: {formatPrice(d.childPrice)}</p>
                  </TableCell>
                  <TableCell className="hidden sm:table-cell">
                    {d.seatsBooked}/{d.capacity} đã đặt
                  </TableCell>
                  <TableCell>
                    <DepartureStatusBadge departure={d} />
                    {d.cancelReason && (
                      <p className="mt-1 max-w-48 truncate text-xs" title={d.cancelReason}>
                        {d.cancelReason}
                      </p>
                    )}
                  </TableCell>
                  <TableCell>
                    <DepartureActions
                      departure={d}
                      manifestHref={`/${scope}/tours/${tour.id}/departures/${d.id}`}
                      suspended={suspended}
                      onEdit={() => setEditing(d)}
                      onClose={() => run(() => closeDeparture(scope, tour.id, d.id), "Đã ngừng bán lịch này").catch(() => {})}
                      onReopen={() => run(() => reopenDeparture(scope, tour.id, d.id), "Đã mở bán lại").catch(() => {})}
                      onCancel={() => setAction({ kind: "cancel", departure: d })}
                      onDelete={() => setAction({ kind: "delete", departure: d })}
                    />
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}
      </CardContent>

      {editing !== null && (
        <DepartureDialog
          key={editing === "new" ? "new" : editing.id}
          departure={editing === "new" ? null : editing}
          durationDays={tour.durationDays}
          onClose={() => setEditing(null)}
          onSave={async (values) => {
            if (editing === "new") await addDeparture(scope, tour.id, values);
            else await updateDeparture(scope, tour.id, editing.id, values);
            toast.success(editing === "new" ? "Đã thêm lịch khởi hành" : "Đã lưu lịch khởi hành");
            setEditing(null);
            await refresh();
          }}
        />
      )}

      <ReasonDialog
        open={action?.kind === "cancel"}
        onOpenChange={(open) => !open && setAction(null)}
        title="Hủy chuyến này?"
        description={`Lịch ${action ? formatDay(action.departure.startDate) : ""} sẽ bị hủy. Khách đã đặt (nếu có) được hoàn 100% tiền và nhận email thông báo.`}
        label="Lý do hủy"
        placeholder="Ví dụ: Không đủ số khách tối thiểu / thời tiết xấu"
        confirmLabel="Hủy chuyến"
        hint="Lý do được gửi kèm email tới khách đã đặt."
        onConfirm={(reason) => run(() => cancelDeparture(scope, tour.id, action!.departure.id, reason), "Đã hủy chuyến")}
      />
      <ConfirmDialog
        open={action?.kind === "delete"}
        onOpenChange={(open) => !open && setAction(null)}
        title="Xóa lịch khởi hành?"
        description="Chỉ xóa được lịch chưa có khách đặt. Lịch đã có khách hãy dùng Hủy chuyến."
        confirmLabel="Xóa lịch"
        destructive
        onConfirm={() => run(() => deleteDeparture(scope, tour.id, action!.departure.id), "Đã xóa lịch khởi hành")}
      />
    </Card>
  );
}

function DepartureActions({
  departure,
  manifestHref,
  suspended,
  onEdit,
  onClose,
  onReopen,
  onCancel,
  onDelete,
}: {
  departure: TourDeparture;
  manifestHref: string;
  suspended: boolean;
  onEdit: () => void;
  onClose: () => void;
  onReopen: () => void;
  onCancel: () => void;
  onDelete: () => void;
}) {
  const active = departure.status !== "CANCELLED" && !departure.departed;
  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button size="icon" variant="ghost" className="size-8 rounded-lg" aria-label="Thao tác">
          <MoreHorizontal />
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end">
        <DropdownMenuItem asChild>
          <Link href={manifestHref}>
            <Users /> Danh sách khách ({departure.seatsBooked})
          </Link>
        </DropdownMenuItem>
        <DropdownMenuSeparator />
        {active && !suspended && (
          <DropdownMenuItem onSelect={onEdit}>
            <Pencil /> Sửa
          </DropdownMenuItem>
        )}
        {active && departure.status === "OPEN" && (
          <DropdownMenuItem onSelect={onClose}>
            <StopCircle /> Ngừng bán
          </DropdownMenuItem>
        )}
        {active && departure.status === "CLOSED" && !suspended && (
          <DropdownMenuItem onSelect={onReopen}>
            <PlayCircle /> Mở bán lại
          </DropdownMenuItem>
        )}
        {active && (
          <DropdownMenuItem onSelect={onCancel}>
            <Ban /> Hủy chuyến
          </DropdownMenuItem>
        )}
        {departure.seatsBooked === 0 && (
          <>
            {active && <DropdownMenuSeparator />}
            <DropdownMenuItem variant="destructive" onSelect={onDelete}>
              <Trash2 /> Xóa
            </DropdownMenuItem>
          </>
        )}
      </DropdownMenuContent>
    </DropdownMenu>
  );
}

function DepartureDialog({
  departure,
  durationDays,
  onClose,
  onSave,
}: {
  departure: TourDeparture | null;
  durationDays: number;
  onClose: () => void;
  onSave: (values: DepartureValues) => Promise<void>;
}) {
  const booked = (departure?.seatsBooked ?? 0) > 0;
  const minDate = localDateString(TOUR_RULES.minDepartureLeadDays);
  const form = useForm<DepartureValues>({
    resolver: zodResolver(departureSchema),
    defaultValues: departure
      ? {
          startDate: departure.startDate,
          capacity: departure.capacity,
          adultPrice: departure.adultPrice,
          childPrice: departure.childPrice,
        }
      : { startDate: "", capacity: 20, adultPrice: undefined, childPrice: undefined },
  });

  const submit = async (values: DepartureValues) => {
    try {
      await onSave(values);
    } catch (error) {
      if (error instanceof ApiError && error.hasFieldErrors) {
        Object.entries(error.fieldErrors).forEach(([field, message]) =>
          form.setError(field as keyof DepartureValues, { type: "server", message }),
        );
      } else {
        toast.error(errorMessage(error));
      }
    }
  };

  const submitting = form.formState.isSubmitting;

  return (
    <Dialog open onOpenChange={(open) => !open && !submitting && onClose()}>
      <DialogContent className="rounded-2xl sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>{departure ? "Sửa lịch khởi hành" : "Thêm lịch khởi hành"}</DialogTitle>
          <DialogDescription>
            {booked
              ? "Lịch đã có khách đặt: chỉ được tăng số chỗ, không đổi ngày và giá."
              : `Tour ${durationDays} ngày — ngày về được tính tự động.`}
          </DialogDescription>
        </DialogHeader>
        <form id="departure-form" noValidate onKeyDown={focusNextOnEnter} onSubmit={form.handleSubmit(submit)} className="grid gap-5 sm:grid-cols-2">
          <Controller
            control={form.control}
            name="startDate"
            render={({ field, fieldState }) => (
              <Field data-invalid={fieldState.invalid}>
                <FieldLabel htmlFor="startDate">Ngày khởi hành</FieldLabel>
                <Input
                  id="startDate"
                  type="date"
                  min={minDate}
                  {...field}
                  disabled={booked}
                  aria-invalid={fieldState.invalid}
                  className="h-12 rounded-xl text-base"
                />
                {fieldState.invalid && <FieldError errors={[fieldState.error]} />}
              </Field>
            )}
          />
          <Controller
            control={form.control}
            name="capacity"
            render={({ field, fieldState }) => (
              <Field data-invalid={fieldState.invalid}>
                <FieldLabel htmlFor="capacity">Số chỗ</FieldLabel>
                <Input
                  id="capacity"
                  type="number"
                  inputMode="numeric"
                  min={Math.max(departure?.seatsBooked ?? 1, 1)}
                  max={TOUR_RULES.maxCapacity}
                  value={field.value ?? ""}
                  onChange={(e) => field.onChange(e.target.value === "" ? undefined : Number(e.target.value))}
                  onBlur={field.onBlur}
                  aria-invalid={fieldState.invalid}
                  className="h-12 rounded-xl text-base"
                />
                {fieldState.invalid && <FieldError errors={[fieldState.error]} />}
              </Field>
            )}
          />
          <Controller
            control={form.control}
            name="adultPrice"
            render={({ field, fieldState }) => (
              <Field data-invalid={fieldState.invalid}>
                <FieldLabel htmlFor="adultPrice">Giá người lớn</FieldLabel>
                <PriceInput id="adultPrice" value={field.value} onChange={field.onChange} onBlur={field.onBlur} disabled={booked} aria-invalid={fieldState.invalid} />
                {fieldState.invalid && <FieldError errors={[fieldState.error]} />}
              </Field>
            )}
          />
          <Controller
            control={form.control}
            name="childPrice"
            render={({ field, fieldState }) => (
              <Field data-invalid={fieldState.invalid}>
                <FieldLabel htmlFor="childPrice">Giá trẻ em (2–11 tuổi)</FieldLabel>
                <PriceInput id="childPrice" value={field.value} onChange={field.onChange} onBlur={field.onBlur} disabled={booked} aria-invalid={fieldState.invalid} />
                <FieldDescription>Trẻ sơ sinh dưới 2 tuổi miễn phí.</FieldDescription>
                {fieldState.invalid && <FieldError errors={[fieldState.error]} />}
              </Field>
            )}
          />
        </form>
        <DialogFooter>
          <Button variant="outline" className="rounded-xl" disabled={submitting} onClick={onClose}>
            Hủy
          </Button>
          <Button type="submit" form="departure-form" className="rounded-xl" disabled={submitting}>
            {submitting && <Spinner />}
            {departure ? "Lưu" : "Thêm lịch"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
