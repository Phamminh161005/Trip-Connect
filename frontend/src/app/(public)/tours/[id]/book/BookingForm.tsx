"use client";

import { useState } from "react";
import Image from "next/image";
import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { useQuery } from "@tanstack/react-query";
import { Controller, useFieldArray, useForm, useWatch, type Control, type Path, type Resolver } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { ArrowLeft, CalendarDays, Clock, Info, Lock, Minus, Plus, ShieldCheck, Users } from "lucide-react";
import { toast } from "sonner";
import { PassengerSlotsFields, type PassengerSlotsValues } from "@/components/booking/PassengerSlotsFields";
import { TextField } from "@/components/form/TextField";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Checkbox } from "@/components/ui/checkbox";
import { Field, FieldError } from "@/components/ui/field";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Skeleton } from "@/components/ui/skeleton";
import { Spinner } from "@/components/ui/spinner";
import { Textarea } from "@/components/ui/textarea";
import { ApiError, errorMessage } from "@/lib/api/errors";
import { createBooking } from "@/lib/api/bookings";
import { getPublicTour } from "@/lib/api/tours";
import { useAuth } from "@/lib/auth/AuthProvider";
import { BOOKING_RULES } from "@/lib/booking/labels";
import {
  buildSlots,
  checkSlots,
  passengerListDeadline,
  passengerSlotSchema,
  slotsToRequest,
  type SlotRules,
  type TravellerCounts,
} from "@/lib/booking/passengerSlots";
import { focusNextOnEnter } from "@/lib/form/focusNextOnEnter";
import { formatDay, formatDayWithWeekday, formatDuration, formatPrice } from "@/lib/tour/labels";
import type { TourDeparture, TourDetail } from "@/types/tour";

const baseSchema = z.object({
  departureId: z.string().min(1, "Chọn lịch khởi hành"),
  contactName: z.string().trim().min(1, "Nhập họ tên người liên hệ").max(100, "Tối đa 100 ký tự"),
  contactPhone: z.string().trim().regex(/^\+?\d{9,15}$/, "Số điện thoại không hợp lệ"),
  contactEmail: z.string().trim().pipe(z.email("Email không đúng định dạng")),
  note: z.string().trim().max(1000, "Tối đa 1000 ký tự"),
  adults: z.number().int(),
  children: z.number().int(),
  infants: z.number().int(),
  passengers: z.array(passengerSlotSchema),
  agree: z.literal(true, { error: "Vui lòng đồng ý với chính sách hủy tour" }),
});

type BookingValues = z.infer<typeof baseSchema>;

/** Quy tắc danh sách phụ thuộc lịch khởi hành đang chọn -> truyền qua context của form. */
const bookingResolver: Resolver<BookingValues, SlotRules> = (values, context, options) =>
  zodResolver(
    baseSchema.superRefine((v, ctx) => {
      if (v.adults < 1) ctx.addIssue({ code: "custom", path: ["adults"], message: "Cần ít nhất 1 người lớn" });
      if (v.infants > v.adults) {
        ctx.addIssue({ code: "custom", path: ["infants"], message: "Mỗi trẻ sơ sinh cần đi cùng một người lớn" });
      }
      if (v.adults + v.children + v.infants > BOOKING_RULES.maxTravellers) {
        ctx.addIssue({ code: "custom", path: ["adults"], message: `Mỗi đơn tối đa ${BOOKING_RULES.maxTravellers} khách` });
      }
      if (context) checkSlots(v.passengers, context, ctx, ["passengers"]);
    }),
  )(values, context, options);

/** Lỗi Backend "passengers[0].fullName" -> ô "passengers.0.fullName" trong form. */
const toFormPath = (field: string) => field.replace(/\[(\d+)]/g, ".$1") as Path<BookingValues>;

export function BookingForm({ tourId }: { tourId: number }) {
  const query = useQuery({ queryKey: ["public-tour", tourId], queryFn: () => getPublicTour(tourId) });

  if (query.isPending) {
    return (
      <div className="mx-auto grid max-w-6xl gap-6 px-4 py-8 lg:grid-cols-[1fr_360px]">
        <Skeleton className="h-[600px] rounded-2xl" />
        <Skeleton className="h-80 rounded-2xl" />
      </div>
    );
  }
  if (query.isError) {
    return (
      <div className="mx-auto max-w-xl px-4 py-20 text-center">
        <p className="text-lg font-semibold">Không thể đặt tour này</p>
        <p className="text-muted-foreground">{errorMessage(query.error)}</p>
        <Button asChild className="mt-4 rounded-xl">
          <Link href="/tours">Xem tour khác</Link>
        </Button>
      </div>
    );
  }
  const bookable = query.data.departures.filter((d) => d.bookable);
  if (bookable.length === 0) {
    return (
      <div className="mx-auto max-w-xl px-4 py-20 text-center">
        <p className="text-lg font-semibold">Tour hiện không còn lịch khởi hành mở bán</p>
        <Button asChild className="mt-4 rounded-xl">
          <Link href={`/tours/${tourId}`}>Quay lại trang tour</Link>
        </Button>
      </div>
    );
  }
  return <Form tour={query.data} departures={bookable} />;
}

function Form({ tour, departures }: { tour: TourDetail; departures: TourDeparture[] }) {
  const { user } = useAuth();
  const searchParams = useSearchParams();
  const requested = departures.find((d) => String(d.id) === searchParams.get("departure"));
  const [redirecting, setRedirecting] = useState(false);
  const [departureId, setDepartureId] = useState(String((requested ?? departures[0]).id));
  const departure = departures.find((d) => String(d.id) === departureId) ?? departures[0];

  const deadline = passengerListDeadline(departure.startDate);

  const form = useForm<BookingValues, SlotRules>({
    resolver: bookingResolver,
    context: { startDate: departure.startDate, international: tour.international },
    defaultValues: {
      departureId,
      contactName: user?.fullName ?? "",
      contactPhone: user?.phone ?? "",
      contactEmail: user?.email ?? "",
      note: "",
      adults: 1,
      children: 0,
      infants: 0,
      // Người đặt thường đi cùng đoàn -> điền sẵn tên vào ô người lớn đầu tiên
      passengers: buildSlots({ adults: 1, children: 0, infants: 0 }, [
        { type: "ADULT", fullName: user?.fullName ?? "", dateOfBirth: "", passportNumber: "" },
      ]),
      agree: false as unknown as true,
    },
  });
  const passengers = useFieldArray({ control: form.control, name: "passengers" });
  const [adults, children, infants, slots] = useWatch({
    control: form.control,
    name: ["adults", "children", "infants", "passengers"],
  });
  const travellers = adults + children + infants;
  const seats = adults + children;
  const total = adults * departure.adultPrice + children * departure.childPrice;

  const changeCount = (key: keyof TravellerCounts, value: number) => {
    const counts: TravellerCounts = { adults, children, infants, [key]: value };
    // Không để trẻ sơ sinh nhiều hơn người lớn
    if (counts.infants > counts.adults) counts.infants = counts.adults;
    form.setValue("adults", counts.adults);
    form.setValue("children", counts.children);
    form.setValue("infants", counts.infants);
    passengers.replace(buildSlots(counts, form.getValues("passengers")));
    if (form.formState.isSubmitted) void form.trigger(["adults", "infants"]);
  };

  const submit = async (values: BookingValues) => {
    try {
      const result = await createBooking({
        departureId: Number(values.departureId),
        contactName: values.contactName,
        contactPhone: values.contactPhone,
        contactEmail: values.contactEmail,
        note: values.note || null,
        adults: values.adults,
        children: values.children,
        infants: values.infants,
        passengers: slotsToRequest(values.passengers),
      });
      setRedirecting(true);
      // Chuyển sang cổng VNPay (rời khỏi web) — thanh toán xong VNPay đưa khách về /payment/vnpay-return
      window.location.assign(result.paymentUrl);
    } catch (error) {
      if (error instanceof ApiError && error.hasFieldErrors) {
        Object.entries(error.fieldErrors).forEach(([field, message]) =>
          form.setError(toFormPath(field), { type: "server", message }),
        );
        toast.error("Vui lòng kiểm tra lại thông tin");
      } else {
        toast.error(errorMessage(error));
      }
    }
  };

  const busy = form.formState.isSubmitting || redirecting;
  const countError = form.formState.errors.adults?.message ?? form.formState.errors.infants?.message;

  return (
    <div className="mx-auto w-full max-w-6xl px-4 py-8 sm:px-6">
      <Link
        href={`/tours/${tour.id}`}
        className="mb-4 flex w-fit items-center gap-1.5 text-sm text-muted-foreground hover:text-foreground"
      >
        <ArrowLeft className="size-4" /> Quay lại trang tour
      </Link>
      <h1 className="mb-6 text-2xl font-bold tracking-tight">Đặt tour</h1>

      <form
        noValidate
        onKeyDown={focusNextOnEnter}
        onSubmit={form.handleSubmit(submit)}
        className="grid items-start gap-6 lg:grid-cols-[1fr_360px]"
      >
        <div className="flex min-w-0 flex-col gap-6">
          {/* Lịch khởi hành */}
          <Card className="rounded-2xl">
            <CardHeader>
              <CardTitle className="flex items-center gap-2 text-lg">
                <CalendarDays className="size-5 text-primary" /> Lịch khởi hành
              </CardTitle>
            </CardHeader>
            <CardContent>
              <Controller
                control={form.control}
                name="departureId"
                render={({ field }) => (
                  <Select
                    value={field.value}
                    onValueChange={(value) => {
                      field.onChange(value);
                      setDepartureId(value);
                    }}
                  >
                    <SelectTrigger className="h-12! w-full rounded-xl text-base" aria-label="Lịch khởi hành">
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      {departures.map((d) => (
                        <SelectItem key={d.id} value={String(d.id)}>
                          {formatDayWithWeekday(d.startDate)} · {formatPrice(d.adultPrice)} · còn {d.seatsAvailable} chỗ
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                )}
              />
            </CardContent>
          </Card>

          {/* Người đại diện */}
          <Card className="rounded-2xl">
            <CardHeader>
              <CardTitle className="text-lg">Người đại diện đặt tour</CardTitle>
              <CardDescription>Xác nhận đặt tour và thông tin tập trung được gửi về email này.</CardDescription>
            </CardHeader>
            <CardContent className="grid gap-5 sm:grid-cols-2">
              <div className="sm:col-span-2">
                <TextField control={form.control} name="contactName" label="Họ và tên" autoComplete="name" />
              </div>
              <TextField control={form.control} name="contactPhone" label="Số điện thoại" type="tel" autoComplete="tel" />
              <TextField control={form.control} name="contactEmail" label="Email" type="email" autoComplete="email" />
            </CardContent>
          </Card>

          {/* Số khách + danh sách */}
          <Card className="rounded-2xl">
            <CardHeader>
              <CardTitle className="flex items-center gap-2 text-lg">
                <Users className="size-5 text-primary" /> Số khách
              </CardTitle>
              <CardDescription>Loại khách tính theo tuổi vào ngày khởi hành.</CardDescription>
            </CardHeader>
            <CardContent className="flex flex-col gap-5">
              <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
                <div className="flex flex-col justify-center rounded-xl border border-primary/40 bg-primary/5 p-3">
                  <span className="text-xs text-muted-foreground">Tổng</span>
                  <span className="text-xl font-bold tabular-nums">{travellers} khách</span>
                  <span className="text-xs text-muted-foreground">chiếm {seats} chỗ</span>
                </div>
                <Stepper
                  label="Người lớn"
                  hint="từ 12 tuổi"
                  value={adults}
                  min={1}
                  max={adults + BOOKING_RULES.maxTravellers - travellers}
                  onChange={(v) => changeCount("adults", v)}
                />
                <Stepper
                  label="Trẻ em"
                  hint="2–11 tuổi"
                  value={children}
                  min={0}
                  max={children + BOOKING_RULES.maxTravellers - travellers}
                  onChange={(v) => changeCount("children", v)}
                />
                <Stepper
                  label="Trẻ sơ sinh"
                  hint="dưới 2 tuổi, miễn phí, không chiếm chỗ"
                  value={infants}
                  min={0}
                  max={Math.min(adults, infants + BOOKING_RULES.maxTravellers - travellers)}
                  onChange={(v) => changeCount("infants", v)}
                />
              </div>
              {countError && <p className="text-sm text-destructive">{countError}</p>}
              {seats > departure.seatsAvailable && (
                <p className="text-sm text-destructive">Lịch này chỉ còn {departure.seatsAvailable} chỗ.</p>
              )}

              <div className="flex flex-col gap-4 border-t pt-5">
                <div>
                  <p className="font-semibold">Danh sách hành khách</p>
                  <p className="text-sm text-muted-foreground">
                    Nhập đúng họ tên, ngày sinh như giấy tờ tùy thân
                    {tour.international ? " và số hộ chiếu" : ""} — dùng để mua bảo hiểm, vé và làm thủ tục. Nhập sai có thể sửa trong
                    &quot;Đơn đặt của tôi&quot; tới hết ngày {formatDay(deadline)}.
                  </p>
                </div>
                <PassengerSlotsFields
                  control={form.control as unknown as Control<PassengerSlotsValues>}
                  fields={passengers.fields}
                  slots={slots}
                  startDate={departure.startDate}
                  international={tour.international}
                />
              </div>
            </CardContent>
          </Card>

          <Card className="rounded-2xl">
            <CardHeader>
              <CardTitle className="text-lg">Ghi chú cho đơn vị tổ chức (không bắt buộc)</CardTitle>
            </CardHeader>
            <CardContent>
              <Controller
                control={form.control}
                name="note"
                render={({ field, fieldState }) => (
                  <Field data-invalid={fieldState.invalid}>
                    <Textarea
                      {...field}
                      rows={3}
                      placeholder="Ví dụ: ăn chay, cần ghế trẻ em, đón tại khách sạn..."
                      aria-label="Ghi chú"
                      className="rounded-xl text-base"
                    />
                    {fieldState.invalid && <FieldError errors={[fieldState.error]} />}
                  </Field>
                )}
              />
            </CardContent>
          </Card>
        </div>

        {/* Tóm tắt + thanh toán */}
        <aside className="flex flex-col gap-4 lg:sticky lg:top-24">
          <Card className="overflow-hidden rounded-2xl pt-0">
            {tour.images[0] && (
              <div className="relative aspect-video">
                <Image src={tour.images[0].url} alt={tour.title} fill sizes="360px" className="object-cover" />
              </div>
            )}
            <CardContent className="flex flex-col gap-4">
              <div>
                <p className="line-clamp-2 font-semibold">{tour.title}</p>
                <p className="mt-1 flex items-center gap-1.5 text-sm text-muted-foreground">
                  <Clock className="size-4" /> {formatDuration(tour.durationDays, tour.durationNights)} ·{" "}
                  {formatDay(departure.startDate)} – {formatDay(departure.endDate)}
                </p>
              </div>

              <dl className="flex flex-col gap-1.5 border-t pt-4 text-sm">
                <Row label={`Người lớn × ${adults}`} value={formatPrice(adults * departure.adultPrice)} />
                {children > 0 && <Row label={`Trẻ em × ${children}`} value={formatPrice(children * departure.childPrice)} />}
                {infants > 0 && <Row label={`Trẻ sơ sinh × ${infants}`} value="Miễn phí" />}
                <div className="mt-2 flex items-end justify-between border-t pt-3">
                  <dt className="font-semibold">Tổng cộng</dt>
                  <dd className="text-2xl font-bold text-primary">{formatPrice(total)}</dd>
                </div>
              </dl>

              <div className="rounded-xl bg-muted/60 p-3 text-xs leading-relaxed text-muted-foreground">
                <p className="mb-1 flex items-center gap-1.5 font-semibold text-foreground">
                  <Info className="size-3.5" /> Chính sách hủy tour
                </p>
                Hủy trước ngày đi từ 7 ngày: hoàn 100% · từ 3 đến 6 ngày: hoàn 50% · dưới 3 ngày: không hoàn. Đơn vị tổ chức hủy chuyến:
                hoàn 100%.
              </div>

              <Controller
                control={form.control}
                name="agree"
                render={({ field, fieldState }) => (
                  <Field data-invalid={fieldState.invalid}>
                    <label className="flex items-start gap-2 text-sm">
                      <Checkbox
                        checked={field.value === true}
                        onCheckedChange={(checked) => field.onChange(checked === true)}
                        aria-invalid={fieldState.invalid}
                        className="mt-0.5"
                      />
                      Tôi đã kiểm tra thông tin và đồng ý với chính sách hủy tour.
                    </label>
                    {fieldState.invalid && <FieldError errors={[fieldState.error]} />}
                  </Field>
                )}
              />

              <Button
                type="submit"
                size="lg"
                className="h-12 rounded-xl text-base font-semibold"
                disabled={busy || seats > departure.seatsAvailable}
              >
                {busy ? <Spinner /> : <Lock />}
                {redirecting ? "Đang chuyển sang VNPay..." : "Thanh toán qua VNPay"}
              </Button>
              <p className="flex items-start gap-1.5 text-xs text-muted-foreground">
                <ShieldCheck className="mt-0.5 size-3.5 shrink-0 text-emerald-600" />
                Chỗ được giữ {BOOKING_RULES.holdMinutes} phút để bạn thanh toán. Thông tin thẻ nhập trực tiếp tại VNPay, TripConnect không
                lưu.
              </p>
            </CardContent>
          </Card>
        </aside>
      </form>
    </div>
  );
}

/** Ô chọn số lượng: [−] số [+]. */
function Stepper({
  label,
  hint,
  value,
  min,
  max,
  onChange,
}: {
  label: string;
  hint: string;
  value: number;
  min: number;
  max: number;
  onChange: (value: number) => void;
}) {
  return (
    <div className="flex flex-col rounded-xl border bg-muted/30 p-3">
      <span className="text-xs text-muted-foreground">{label}</span>
      <div className="my-1 flex items-center justify-between gap-2">
        <Button
          type="button"
          variant="outline"
          size="icon"
          className="size-8 rounded-lg"
          disabled={value <= min}
          onClick={() => onChange(value - 1)}
          aria-label={`Bớt ${label.toLowerCase()}`}
        >
          <Minus />
        </Button>
        <span className="text-xl font-bold tabular-nums" aria-live="polite">
          {value}
        </span>
        <Button
          type="button"
          variant="outline"
          size="icon"
          className="size-8 rounded-lg"
          disabled={value >= max}
          onClick={() => onChange(value + 1)}
          aria-label={`Thêm ${label.toLowerCase()}`}
        >
          <Plus />
        </Button>
      </div>
      <span className="text-xs leading-snug text-muted-foreground">{hint}</span>
    </div>
  );
}

function Row({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex justify-between gap-2">
      <dt className="text-muted-foreground">{label}</dt>
      <dd>{value}</dd>
    </div>
  );
}
