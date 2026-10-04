"use client";

import { useMemo, useState, type ReactNode } from "react";
import Link from "next/link";
import {
  Controller,
  useFieldArray,
  useForm,
  useWatch,
  type FieldErrors,
  type Path,
  type UseFieldArrayReturn,
} from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useQuery } from "@tanstack/react-query";
import { Check, ChevronLeft, ChevronRight } from "lucide-react";
import { toast } from "sonner";
import { LocationMultiSelect } from "@/components/auth/register/LocationMultiSelect";
import { SearchableSelect } from "@/components/form/SearchableSelect";
import { TextField } from "@/components/form/TextField";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Checkbox } from "@/components/ui/checkbox";
import { Field, FieldDescription, FieldError, FieldGroup, FieldLabel } from "@/components/ui/field";
import { Input } from "@/components/ui/input";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Skeleton } from "@/components/ui/skeleton";
import { Spinner } from "@/components/ui/spinner";
import { Textarea } from "@/components/ui/textarea";
import { useProvinceOptions } from "@/components/agent/useCatalogOptions";
import { ApiError, errorMessage } from "@/lib/api/errors";
import { focusNextOnEnter } from "@/lib/form/focusNextOnEnter";
import { getLocations, getTourCategories } from "@/lib/api/catalog";
import { ACCOMMODATION_LABELS, TOUR_RULES, TRANSPORT_LABELS, formatDuration } from "@/lib/tour/labels";
import { cn } from "@/lib/utils";
import {
  backendFieldToFormPath,
  emptyItineraryDay,
  formValuesToRequest,
  tourContentSchema,
  tourToFormValues,
  type TourContentValues,
} from "@/lib/validation/tour";
import type { TourContentRequest, TourDetail, TransportMode } from "@/types/tour";
import { ChipMultiSelect, LinesField } from "./formParts";

const STEPS: { title: string; description: string; fields: Path<TourContentValues>[] }[] = [
  {
    title: "Thông tin chung",
    description: "Tên tour, loại hình, nơi đi – điểm đến, thời lượng và điểm nổi bật.",
    fields: ["title", "categoryIds", "departureLocationId", "destinationIds", "durationDays", "durationNights", "highlights"],
  },
  {
    title: "Lịch trình",
    description: "Nội dung từng ngày, bữa ăn và nơi nghỉ đêm.",
    fields: ["itinerary"],
  },
  {
    title: "Dịch vụ",
    description: "Phương tiện, lưu trú, điểm đón, dịch vụ bao gồm / không bao gồm.",
    fields: ["transportModes", "accommodationType", "meetingPoint", "meetingTime", "includedServices", "excludedServices", "notes"],
  },
];

/** Bước chứa trường bị lỗi đầu tiên (để chuyển tới đúng bước khi gửi form bị lỗi). */
function firstStepWithError(paths: string[]): number {
  const index = STEPS.findIndex((step) => paths.some((p) => step.fields.some((f) => p === f || p.startsWith(`${f}.`))));
  return index === -1 ? 0 : index;
}

interface TourContentFormProps {
  tour?: TourDetail;
  submitLabel: string;
  cancelHref: string;
  /** Cảnh báo hiện trên đầu form (vd sửa tour đang bán sẽ phải duyệt lại). */
  notice?: ReactNode;
  onSubmit: (data: TourContentRequest) => Promise<void>;
}

/** Form nội dung tour 3 bước — dùng chung cho tạo mới / chỉnh sửa, Agent / Admin. */
export function TourContentForm({ tour, submitLabel, cancelHref, notice, onSubmit }: TourContentFormProps) {
  const [step, setStep] = useState(0);
  // Bước xa nhất đã tới; sửa tour có sẵn thì dữ liệu đã đủ, cho đi thẳng tới bước nào cũng được
  const [reached, setReached] = useState(tour ? STEPS.length - 1 : 0);
  const form = useForm<TourContentValues>({
    resolver: zodResolver(tourContentSchema),
    defaultValues: tourToFormValues(tour),
    mode: "onTouched",
  });
  // Một nơi duy nhất quản lý danh sách ngày: bước 1 thêm/bớt theo số ngày, bước 2 hiển thị
  const itinerary = useFieldArray({ control: form.control, name: "itinerary" });

  /** Lùi thì đi luôn; tiến thì kiểm tra lần lượt các bước ở giữa, dừng ở bước đầu tiên còn lỗi. */
  const goTo = async (target: number) => {
    if (target > step) {
      for (let i = step; i < target; i++) {
        if (!(await form.trigger(STEPS[i].fields, { shouldFocus: i === step }))) {
          setStep(i);
          return;
        }
      }
      setReached((r) => Math.max(r, target));
    }
    setStep(target);
    window.scrollTo({ top: 0, behavior: "smooth" });
  };

  const onInvalid = (errors: FieldErrors<TourContentValues>) => setStep(firstStepWithError(Object.keys(errors)));

  const submit = async (values: TourContentValues) => {
    try {
      await onSubmit(formValuesToRequest(values));
    } catch (error) {
      if (error instanceof ApiError && error.hasFieldErrors) {
        const paths = Object.entries(error.fieldErrors).map(([field, message]) => {
          const path = backendFieldToFormPath(field);
          form.setError(path as Path<TourContentValues>, { type: "server", message });
          return path;
        });
        setStep(firstStepWithError(paths));
        toast.error("Vui lòng kiểm tra lại các ô được đánh dấu");
      } else {
        toast.error(errorMessage(error));
      }
    }
  };

  const submitting = form.formState.isSubmitting;
  const last = step === STEPS.length - 1;

  return (
    <form
      noValidate
      onSubmit={form.handleSubmit(submit, onInvalid)}
      onKeyDown={focusNextOnEnter}
      className="flex flex-col gap-6"
    >
      {notice}

      <ol className="grid grid-cols-3 gap-2" aria-label="Các bước">
        {STEPS.map((s, index) => (
          <li key={s.title}>
            <button
              type="button"
              disabled={index > reached}
              onClick={() => goTo(index)}
              aria-current={index === step ? "step" : undefined}
              className={cn(
                "flex w-full items-center gap-2 rounded-xl border px-3 py-2.5 text-left text-sm transition-colors disabled:cursor-default",
                index === step ? "border-primary bg-primary/5 font-semibold" : "bg-card",
                index !== step && index <= reached && "hover:bg-accent",
              )}
            >
              <span
                className={cn(
                  "flex size-6 shrink-0 items-center justify-center rounded-full text-xs font-semibold",
                  index < step ? "bg-primary text-primary-foreground" : index === step ? "bg-primary/15 text-primary" : "bg-muted",
                )}
              >
                {index < step ? <Check className="size-3.5" /> : index + 1}
              </span>
              <span className="truncate">{s.title}</span>
            </button>
          </li>
        ))}
      </ol>

      <Card className="rounded-2xl">
        <CardHeader>
          <CardTitle className="text-lg">
            Bước {step + 1}: {STEPS[step].title}
          </CardTitle>
          <CardDescription>{STEPS[step].description}</CardDescription>
        </CardHeader>
        <CardContent>
          {/* Giữ cả 3 bước trong DOM (ẩn bước khác) để dữ liệu đã nhập không bị mất khi chuyển bước */}
          <div hidden={step !== 0}>
            <GeneralStep form={form} itinerary={itinerary} />
          </div>
          <div hidden={step !== 1}>
            <ItineraryStep form={form} itinerary={itinerary} />
          </div>
          <div hidden={step !== 2}>
            <ServicesStep form={form} />
          </div>
        </CardContent>
      </Card>

      <div className="flex flex-wrap items-center justify-between gap-3">
        {step === 0 ? (
          <Button asChild type="button" variant="ghost" className="h-11 rounded-xl">
            <Link href={cancelHref}>Hủy</Link>
          </Button>
        ) : (
          <Button type="button" variant="outline" className="h-11 rounded-xl" onClick={() => setStep((s) => s - 1)}>
            <ChevronLeft /> Quay lại
          </Button>
        )}
        {last ? (
          // key khác nhau: React tạo nút mới, cú bấm "Tiếp tục" không bị hiểu nhầm thành gửi form
          <Button key="submit" type="submit" className="h-11 rounded-xl px-6 font-semibold" disabled={submitting}>
            {submitting && <Spinner />}
            {submitLabel}
          </Button>
        ) : (
          <Button key="next" type="button" className="h-11 rounded-xl px-6 font-semibold" onClick={() => goTo(step + 1)}>
            Tiếp tục <ChevronRight />
          </Button>
        )}
      </div>
    </form>
  );
}

type FormApi = ReturnType<typeof useForm<TourContentValues>>;
type ItineraryArray = UseFieldArrayReturn<TourContentValues, "itinerary">;

// ===================== Bước 1: Thông tin chung =====================

function GeneralStep({ form, itinerary }: { form: FormApi; itinerary: ItineraryArray }) {
  const { control } = form;
  const locationsQuery = useQuery({ queryKey: ["locations"], queryFn: getLocations, staleTime: Infinity });
  const categoriesQuery = useQuery({ queryKey: ["tour-categories"], queryFn: getTourCategories, staleTime: Infinity });
  const provinces = useProvinceOptions();
  const days = useWatch({ control, name: "durationDays" });

  /** Đổi số ngày -> tự thêm / bớt ngày trong lịch trình và gợi ý số đêm = số ngày - 1. */
  const changeDays = (value: number) => {
    form.setValue("durationDays", value, { shouldValidate: true });
    if (!Number.isInteger(value) || value < 1 || value > TOUR_RULES.maxDurationDays) return;
    form.setValue("durationNights", Math.max(value - 1, 0), { shouldValidate: true });
    const current = form.getValues("itinerary").length;
    if (value > current) itinerary.append(Array.from({ length: value - current }, emptyItineraryDay), { shouldFocus: false });
    else if (value < current) itinerary.remove(Array.from({ length: current - value }, (_, i) => value + i));
  };

  const validDays = Number.isInteger(days) && days >= 1 ? days : null;

  return (
    <FieldGroup className="gap-6">
      <TextField control={control} name="title" label="Tên tour" placeholder="Ví dụ: Hạ Long – Lan Hạ 2N1Đ trên du thuyền 5 sao" />

      <Controller
        control={control}
        name="categoryIds"
        render={({ field, fieldState }) => (
          <Field data-invalid={fieldState.invalid}>
            <FieldLabel>Loại hình tour</FieldLabel>
            <FieldDescription>Chọn 1–{TOUR_RULES.maxCategories} loại hình phù hợp nhất.</FieldDescription>
            {categoriesQuery.isPending ? (
              <Skeleton className="h-20 rounded-xl" />
            ) : (
              <ChipMultiSelect
                label="Loại hình tour"
                options={(categoriesQuery.data ?? []).map((c) => ({ value: c.id, label: c.name }))}
                value={field.value}
                onChange={(v) => {
  field.onChange(v);
  field.onBlur();
}}
                max={TOUR_RULES.maxCategories}
              />
            )}
            {fieldState.invalid && <FieldError errors={[fieldState.error]} />}
          </Field>
        )}
      />

      <div className="grid gap-6 md:grid-cols-2">
        <Controller
          control={control}
          name="departureLocationId"
          render={({ field, fieldState }) => (
            <Field data-invalid={fieldState.invalid}>
              <FieldLabel htmlFor="departureLocationId">Nơi khởi hành</FieldLabel>
              <SearchableSelect
                id="departureLocationId"
                options={provinces.options}
                value={field.value}
                onChange={field.onChange}
                onBlur={field.onBlur}
                placeholder={provinces.isPending ? "Đang tải..." : "Chọn tỉnh/thành"}
                searchPlaceholder="Gõ để tìm tỉnh/thành..."
                emptyText="Không tìm thấy tỉnh/thành"
                invalid={fieldState.invalid}
              />
              {fieldState.invalid && <FieldError errors={[fieldState.error]} />}
            </Field>
          )}
        />
        <Controller
          control={control}
          name="destinationIds"
          render={({ field, fieldState }) => (
            <Field data-invalid={fieldState.invalid}>
              <FieldLabel htmlFor="destinationIds">Điểm đến</FieldLabel>
              {locationsQuery.isPending ? (
                <Skeleton className="h-12 rounded-xl" />
              ) : (
                <LocationMultiSelect
                  id="destinationIds"
                  locations={locationsQuery.data ?? []}
                  value={field.value}
                  onChange={(v) => {
                    field.onChange(v);
                    field.onBlur();
                  }}
                  invalid={fieldState.invalid}
                />
              )}
              <FieldDescription>Nước ngoài chọn tới cấp quốc gia; thành phố ghi trong tên tour và lịch trình.</FieldDescription>
              {fieldState.invalid && <FieldError errors={[fieldState.error]} />}
            </Field>
          )}
        />
      </div>

      <div className="grid gap-6 sm:grid-cols-2">
        <Controller
          control={control}
          name="durationDays"
          render={({ field, fieldState }) => (
            <Field data-invalid={fieldState.invalid}>
              <FieldLabel htmlFor="durationDays">Số ngày</FieldLabel>
              <Input
                id="durationDays"
                type="number"
                inputMode="numeric"
                min={1}
                max={TOUR_RULES.maxDurationDays}
                value={Number.isNaN(field.value) || field.value === undefined ? "" : field.value}
                onChange={(e) => changeDays(e.target.value === "" ? Number.NaN : Number(e.target.value))}
                onBlur={field.onBlur}
                aria-invalid={fieldState.invalid}
                className="h-12 rounded-xl text-base"
              />
              <FieldDescription>Lịch trình tự thêm / bớt ngày theo số này.</FieldDescription>
              {fieldState.invalid && <FieldError errors={[fieldState.error]} />}
            </Field>
          )}
        />
        <Controller
          control={control}
          name="durationNights"
          render={({ field, fieldState }) => (
            <Field data-invalid={fieldState.invalid}>
              <FieldLabel htmlFor="durationNights">Thời lượng</FieldLabel>
              <Select
                value={String(field.value)}
                onValueChange={(v) => {
                  field.onChange(Number(v));
                  field.onBlur();
                }}
                disabled={validDays === null}
              >
                <SelectTrigger id="durationNights" className="h-12! w-full rounded-xl text-base" aria-invalid={fieldState.invalid}>
                  <SelectValue placeholder="Nhập số ngày trước" />
                </SelectTrigger>
                <SelectContent>
                  {validDays !== null &&
                    [validDays - 1, validDays]
                      .filter((n) => n >= 0)
                      .map((nights) => (
                        <SelectItem key={nights} value={String(nights)}>
                          {formatDuration(validDays, nights)}
                        </SelectItem>
                      ))}
                </SelectContent>
              </Select>
              {fieldState.invalid && <FieldError errors={[fieldState.error]} />}
            </Field>
          )}
        />
      </div>

      <LinesField
        control={control}
        name="highlights"
        label="Điểm nổi bật"
        description={`${TOUR_RULES.minHighlights}–${TOUR_RULES.maxHighlights} dòng ngắn. 2 dòng đầu hiện trên thẻ tour ở trang tìm kiếm.`}
        placeholder="Ví dụ: Chèo kayak khám phá hang Luồn"
        addLabel="Thêm điểm nổi bật"
        min={1}
        max={TOUR_RULES.maxHighlights}
      />
    </FieldGroup>
  );
}

// ===================== Bước 2: Lịch trình =====================

function ItineraryStep({ form, itinerary }: { form: FormApi; itinerary: ItineraryArray }) {
  const { control } = form;
  const { fields } = itinerary;
  const listError = form.formState.errors.itinerary?.root?.message ?? form.formState.errors.itinerary?.message;

  return (
    <div className="flex flex-col gap-5">
      {listError && <p className="text-sm text-destructive">{listError}</p>}
      <p className="text-sm text-muted-foreground">
        Mẹo: nếu đã có chương trình tour trong Word, hãy sao chép – dán từng ngày. Bạn có thể tải thêm file PDF chương trình tour sau
        khi lưu.
      </p>
      {fields.map((item, index) => (
        <fieldset key={item.id} className="flex flex-col gap-4 rounded-2xl border p-4">
          <legend className="px-2 text-sm font-semibold">Ngày {index + 1}</legend>
          <TextField control={control} name={`itinerary.${index}.title`} label="Tiêu đề" placeholder="Ví dụ: Hà Nội – Vịnh Lan Hạ – Chèo kayak" />
          <Controller
            control={control}
            name={`itinerary.${index}.description`}
            render={({ field, fieldState }) => (
              <Field data-invalid={fieldState.invalid}>
                <FieldLabel htmlFor={`itinerary-${index}-description`}>Nội dung</FieldLabel>
                <Textarea
                  id={`itinerary-${index}-description`}
                  {...field}
                  rows={5}
                  placeholder={"08:00 Xe đón quý khách tại điểm hẹn...\n12:00 Ăn trưa..."}
                  aria-invalid={fieldState.invalid}
                  className="rounded-xl text-base"
                />
                {fieldState.invalid && <FieldError errors={[fieldState.error]} />}
              </Field>
            )}
          />
          <div className="grid gap-4 sm:grid-cols-2">
            <Field>
              <FieldLabel>Bữa ăn được bao gồm</FieldLabel>
              <div className="flex h-12 items-center gap-5">
                {(
                  [
                    ["breakfast", "Sáng"],
                    ["lunch", "Trưa"],
                    ["dinner", "Tối"],
                  ] as const
                ).map(([meal, label]) => (
                  <Controller
                    key={meal}
                    control={control}
                    name={`itinerary.${index}.${meal}`}
                    render={({ field }) => (
                      <label className="flex items-center gap-2 text-sm">
                        <Checkbox checked={field.value} onCheckedChange={(checked) => field.onChange(checked === true)} />
                        {label}
                      </label>
                    )}
                  />
                ))}
              </div>
            </Field>
            <TextField
              control={control}
              name={`itinerary.${index}.accommodation`}
              label="Nơi nghỉ đêm (không bắt buộc)"
              placeholder="Ví dụ: Du thuyền Stellar"
            />
          </div>
        </fieldset>
      ))}
    </div>
  );
}

// ===================== Bước 3: Dịch vụ =====================

const TRANSPORT_OPTIONS = (Object.keys(TRANSPORT_LABELS) as TransportMode[]).map((value) => ({
  value,
  label: TRANSPORT_LABELS[value],
}));

function ServicesStep({ form }: { form: FormApi }) {
  const { control } = form;
  const accommodationOptions = useMemo(
    () => Object.entries(ACCOMMODATION_LABELS).map(([value, label]) => ({ value, label })),
    [],
  );

  return (
    <FieldGroup className="gap-6">
      <Controller
        control={control}
        name="transportModes"
        render={({ field, fieldState }) => (
          <Field data-invalid={fieldState.invalid}>
            <FieldLabel>Phương tiện di chuyển</FieldLabel>
            <ChipMultiSelect
              label="Phương tiện di chuyển"
              options={TRANSPORT_OPTIONS}
              value={field.value}
              onChange={(v) => {
                field.onChange(v);
                field.onBlur();
              }}
            />
            {fieldState.invalid && <FieldError errors={[fieldState.error]} />}
          </Field>
        )}
      />

      <Controller
        control={control}
        name="accommodationType"
        render={({ field, fieldState }) => (
          <Field data-invalid={fieldState.invalid}>
            <FieldLabel htmlFor="accommodationType">Tiêu chuẩn lưu trú</FieldLabel>
            <Select
              value={field.value || undefined}
              onValueChange={(v) => {
                field.onChange(v);
                field.onBlur();
              }}
            >
              <SelectTrigger id="accommodationType" className="h-12! w-full rounded-xl text-base sm:w-80" aria-invalid={fieldState.invalid}>
                <SelectValue placeholder="Chọn tiêu chuẩn lưu trú" />
              </SelectTrigger>
              <SelectContent>
                {accommodationOptions.map((o) => (
                  <SelectItem key={o.value} value={o.value}>
                    {o.label}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
            {fieldState.invalid && <FieldError errors={[fieldState.error]} />}
          </Field>
        )}
      />

      <div className="grid gap-6 sm:grid-cols-[1fr_180px]">
        <TextField control={control} name="meetingPoint" label="Điểm đón" placeholder="Ví dụ: Nhà hát Lớn Hà Nội" />
        <Controller
          control={control}
          name="meetingTime"
          render={({ field, fieldState }) => (
            <Field data-invalid={fieldState.invalid}>
              <FieldLabel htmlFor="meetingTime">Giờ tập trung</FieldLabel>
              <Input id="meetingTime" type="time" {...field} aria-invalid={fieldState.invalid} className="h-12 rounded-xl text-base" />
              {fieldState.invalid && <FieldError errors={[fieldState.error]} />}
            </Field>
          )}
        />
      </div>

      <LinesField
        control={control}
        name="includedServices"
        label="Giá tour bao gồm"
        placeholder="Ví dụ: Xe đưa đón Hà Nội – Hạ Long"
        addLabel="Thêm dịch vụ"
        min={1}
        max={TOUR_RULES.maxServiceItems}
      />
      <LinesField
        control={control}
        name="excludedServices"
        label="Không bao gồm (không bắt buộc)"
        description="Những khoản khách tự chi trả — ghi rõ để tránh hiểu lầm."
        placeholder="Ví dụ: Thuế VAT, chi tiêu cá nhân"
        addLabel="Thêm dòng"
        max={TOUR_RULES.maxServiceItems}
      />

      <Controller
        control={control}
        name="notes"
        render={({ field, fieldState }) => (
          <Field data-invalid={fieldState.invalid}>
            <FieldLabel htmlFor="notes">Lưu ý (không bắt buộc)</FieldLabel>
            <Textarea
              id="notes"
              {...field}
              rows={4}
              placeholder="Giấy tờ cần mang, sức khỏe, trang phục..."
              aria-invalid={fieldState.invalid}
              className="rounded-xl text-base"
            />
            {fieldState.invalid && <FieldError errors={[fieldState.error]} />}
          </Field>
        )}
      />
    </FieldGroup>
  );
}
