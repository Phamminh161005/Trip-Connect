"use client";

import { useRouter } from "next/navigation";
import { useQuery } from "@tanstack/react-query";
import { Controller, useForm, useWatch } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Clock, MessageSquareText, Send, Sparkles, UserCheck } from "lucide-react";
import { toast } from "sonner";
import { useProvinceOptions } from "@/components/agent/useCatalogOptions";
import { LocationMultiSelect } from "@/components/auth/register/LocationMultiSelect";
import { PriceInput } from "@/components/form/PriceInput";
import { SearchableSelect } from "@/components/form/SearchableSelect";
import { ChipMultiSelect } from "@/components/tour/form/formParts";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Field, FieldDescription, FieldError, FieldLabel } from "@/components/ui/field";
import { Input } from "@/components/ui/input";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Skeleton } from "@/components/ui/skeleton";
import { Spinner } from "@/components/ui/spinner";
import { Textarea } from "@/components/ui/textarea";
import { getLocations, getTourCategories } from "@/lib/api/catalog";
import { createCustomRequest } from "@/lib/api/customRequests";
import { applyApiError } from "@/lib/form/applyApiError";
import { focusNextOnEnter } from "@/lib/form/focusNextOnEnter";
import { revalidateTouched } from "@/lib/form/revalidateTouched";
import { CUSTOM_REQUEST_RULES as RULES } from "@/lib/customRequest/labels";
import { ACCOMMODATION_LABELS, TRANSPORT_LABELS, localDateString } from "@/lib/tour/labels";
import { whenValid } from "@/lib/validation/whenValid";
import type { AccommodationType, TransportMode } from "@/types/tour";

const optionalMoney = z.number().int().min(0, "Ngân sách không hợp lệ").max(1_000_000_000, "Ngân sách quá lớn").optional();

const isoDate = z.string().regex(/^\d{4}-\d{2}-\d{2}$/, "Chọn ngày");
const toDisplayDate = (iso: string) => iso.split("-").reverse().join("/");

const baseSchema = z.object({
  departureLocationId: z.number({ error: "Vui lòng chọn nơi khởi hành" }),
  destinationIds: z
    .array(z.number())
    .min(1, "Chọn ít nhất 1 điểm đến")
    .max(RULES.maxDestinations, `Chọn tối đa ${RULES.maxDestinations} điểm đến`),
  // Mốc "hôm nay + 14 ngày" tính lúc kiểm tra (trang mở qua đêm vẫn đúng)
  earliestStart: isoDate.superRefine((value, ctx) => {
    const minStart = localDateString(RULES.minLeadDays);
    if (value < minStart) ctx.addIssue({ code: "custom", message: `Sớm nhất từ ngày ${toDisplayDate(minStart)}` });
  }),
  latestStart: isoDate,
  durationDays: z
    .number({ error: "Nhập số ngày" })
    .int()
    .min(1, "Ít nhất 1 ngày")
    .max(RULES.maxDurationDays, `Tối đa ${RULES.maxDurationDays} ngày`),
  adults: z.number({ error: "Nhập số người lớn" }).int().min(1, "Cần ít nhất 1 người lớn"),
  children: z.number({ error: "Nhập số trẻ em" }).int().min(0),
  infants: z.number({ error: "Nhập số trẻ sơ sinh" }).int().min(0),
  budgetMin: optionalMoney,
  budgetMax: optionalMoney,
  categoryIds: z.array(z.number()).max(RULES.maxCategories, `Chọn tối đa ${RULES.maxCategories} loại hình`),
  transportModes: z.array(z.string()),
  accommodationType: z.string(),
  notes: z.string().trim().max(2000, "Tối đa 2000 ký tự"),
});

// Luật nhiều ô chạy ngay khi các ô liên quan hợp lệ, không đợi điền xong cả form
const schema = baseSchema
  .superRefine(
    (v, ctx) => {
      if (v.latestStart < v.earliestStart) {
        ctx.addIssue({ code: "custom", path: ["latestStart"], message: "Phải sau ngày sớm nhất" });
        return;
      }
      const window = (new Date(v.latestStart).getTime() - new Date(v.earliestStart).getTime()) / 86_400_000;
      if (window > RULES.maxStartWindowDays) {
        ctx.addIssue({ code: "custom", path: ["latestStart"], message: `Khoảng tối đa ${RULES.maxStartWindowDays} ngày` });
      }
    },
    { when: whenValid(baseSchema, "earliestStart", "latestStart") },
  )
  .refine((v) => v.infants <= v.adults, {
    path: ["infants"],
    message: "Không nhiều hơn số người lớn",
    when: whenValid(baseSchema, "adults", "infants"),
  })
  .refine((v) => v.adults + v.children + v.infants <= RULES.maxTravellers, {
    path: ["adults"],
    message: `Tối đa ${RULES.maxTravellers} khách`,
    when: whenValid(baseSchema, "adults", "children", "infants"),
  })
  .refine((v) => v.budgetMin === undefined || v.budgetMax === undefined || v.budgetMin <= v.budgetMax, {
    path: ["budgetMax"],
    message: "Phải lớn hơn hoặc bằng mức tối thiểu",
    when: whenValid(baseSchema, "budgetMin", "budgetMax"),
  });

type Values = z.infer<typeof schema>;

const STEPS = [
  { icon: Send, title: "Gửi yêu cầu", text: "Mô tả chuyến đi bạn mong muốn" },
  { icon: UserCheck, title: "Chọn đơn vị phù hợp", text: "TripConnect giao cho đối tác am hiểu điểm đến" },
  { icon: MessageSquareText, title: "Nhận đề xuất", text: "Lịch trình và báo giá riêng cho bạn, sửa đến khi ưng ý" },
];

/** Form gửi yêu cầu thiết kế tour riêng. */
export function CustomRequestForm() {
  const router = useRouter();
  const provinces = useProvinceOptions();
  const locations = useQuery({ queryKey: ["locations"], queryFn: getLocations, staleTime: Infinity });
  const categories = useQuery({ queryKey: ["tour-categories"], queryFn: getTourCategories, staleTime: Infinity });

  const form = useForm<Values>({
    resolver: zodResolver(schema),
    mode: "onTouched",
    defaultValues: {
      departureLocationId: undefined as unknown as number,
      destinationIds: [],
      earliestStart: "",
      latestStart: "",
      durationDays: 3,
      adults: 2,
      children: 0,
      infants: 0,
      budgetMin: undefined,
      budgetMax: undefined,
      categoryIds: [],
      transportModes: [],
      accommodationType: "",
      notes: "",
    },
  });
  const earliestStart = useWatch({ control: form.control, name: "earliestStart" });

  const submit = async (v: Values) => {
    try {
      const created = await createCustomRequest({
        departureLocationId: v.departureLocationId,
        destinationIds: v.destinationIds,
        earliestStart: v.earliestStart,
        latestStart: v.latestStart,
        durationDays: v.durationDays,
        adults: v.adults,
        children: v.children,
        infants: v.infants,
        budgetMin: v.budgetMin ?? null,
        budgetMax: v.budgetMax ?? null,
        categoryIds: v.categoryIds,
        transportModes: v.transportModes as TransportMode[],
        accommodationType: (v.accommodationType || null) as AccommodationType | null,
        notes: v.notes || null,
      });
      toast.success(`Đã gửi yêu cầu ${created.code}. TripConnect sẽ tìm đơn vị phù hợp cho bạn.`);
      router.push(`/account/requests/${created.id}`);
    } catch (error) {
      applyApiError(error, form.setError);
    }
  };

  // Lỗi của luật nhiều ô nằm ở một ô (vd lỗi trẻ sơ sinh nằm ở ô Trẻ sơ sinh) -> sửa ô kia cũng phải kiểm lại
  const RELATED: Partial<Record<keyof Values, (keyof Values)[]>> = {
    earliestStart: ["latestStart"],
    adults: ["infants"],
    children: ["adults"],
    infants: ["adults"],
    budgetMin: ["budgetMax"],
  };
  const revalidateRelated = (name: keyof Values) => revalidateTouched(form, ...(RELATED[name] ?? []));

  const numberField = (name: "durationDays" | "adults" | "children" | "infants", label: string, hint?: string) => (
    <Controller
      control={form.control}
      name={name}
      render={({ field, fieldState }) => (
        <Field data-invalid={fieldState.invalid}>
          <FieldLabel htmlFor={name}>{label}</FieldLabel>
          <Input
            id={name}
            type="number"
            inputMode="numeric"
            min={name === "adults" || name === "durationDays" ? 1 : 0}
            value={Number.isNaN(field.value) ? "" : field.value}
            onChange={(e) => {
              field.onChange(e.target.value === "" ? Number.NaN : Number(e.target.value));
              revalidateRelated(name);
            }}
            onBlur={field.onBlur}
            aria-invalid={fieldState.invalid}
            className="h-12 rounded-xl text-base"
          />
          {hint && <FieldDescription>{hint}</FieldDescription>}
          {fieldState.invalid && <FieldError errors={[fieldState.error]} />}
        </Field>
      )}
    />
  );

  return (
    <div className="mx-auto w-full max-w-4xl px-4 py-8 sm:px-6">
      <div className="mb-6">
        <h1 className="flex items-center gap-2 text-2xl font-bold tracking-tight sm:text-3xl">
          <Sparkles className="size-7 text-primary" /> Thiết kế tour riêng
        </h1>
        <p className="mt-2 text-muted-foreground">
          Chưa tìm thấy tour ưng ý? Cho chúng tôi biết chuyến đi bạn mong muốn — đối tác lữ hành sẽ thiết kế lịch trình và báo giá
          riêng cho bạn.
        </p>
      </div>
      <ol className="mb-8 grid gap-3 sm:grid-cols-3">
        {STEPS.map(({ icon: Icon, title, text }, i) => (
          <li key={title} className="flex gap-3 rounded-2xl border bg-card p-4">
            <span className="flex size-9 shrink-0 items-center justify-center rounded-full bg-primary/10 text-primary">
              <Icon className="size-4" />
            </span>
            <div>
              <p className="text-sm font-semibold">
                {i + 1}. {title}
              </p>
              <p className="text-xs text-muted-foreground">{text}</p>
            </div>
          </li>
        ))}
      </ol>

      <form noValidate onKeyDown={focusNextOnEnter} onSubmit={form.handleSubmit(submit)} className="flex flex-col gap-6">
        <Card className="rounded-2xl">
          <CardHeader>
            <CardTitle className="text-lg">Hành trình</CardTitle>
          </CardHeader>
          <CardContent className="grid gap-5 md:grid-cols-2">
            <Controller
              control={form.control}
              name="departureLocationId"
              render={({ field, fieldState }) => (
                <Field data-invalid={fieldState.invalid}>
                  <FieldLabel htmlFor="departureLocationId">Khởi hành từ</FieldLabel>
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
              control={form.control}
              name="destinationIds"
              render={({ field, fieldState }) => (
                <Field data-invalid={fieldState.invalid}>
                  <FieldLabel htmlFor="destinationIds">Muốn đến</FieldLabel>
                  {locations.isPending ? (
                    <Skeleton className="h-12 rounded-xl" />
                  ) : (
                    <LocationMultiSelect
                      id="destinationIds"
                      locations={locations.data ?? []}
                      value={field.value}
                      onChange={(v) => {
                        field.onChange(v);
                        field.onBlur();
                      }}
                      invalid={fieldState.invalid}
                    />
                  )}
                  <FieldDescription>Tỉnh/thành trong nước hoặc quốc gia; có thể chọn nhiều nơi.</FieldDescription>
                  {fieldState.invalid && <FieldError errors={[fieldState.error]} />}
                </Field>
              )}
            />
          </CardContent>
        </Card>

        <Card className="rounded-2xl">
          <CardHeader>
            <CardTitle className="flex items-center gap-2 text-lg">
              <Clock className="size-5 text-primary" /> Thời gian và số khách
            </CardTitle>
            <CardDescription>
              Chọn khoảng ngày bạn có thể khởi hành để đơn vị tổ chức linh hoạt sắp xếp (sớm nhất sau {RULES.minLeadDays} ngày).
            </CardDescription>
          </CardHeader>
          <CardContent className="grid gap-5 sm:grid-cols-3">
            {(["earliestStart", "latestStart"] as const).map((name) => (
              <Controller
                key={name}
                control={form.control}
                name={name}
                render={({ field, fieldState }) => (
                  <Field data-invalid={fieldState.invalid}>
                    <FieldLabel htmlFor={name}>{name === "earliestStart" ? "Có thể đi từ ngày" : "Đến ngày"}</FieldLabel>
                    <Input
                      id={name}
                      type="date"
                      min={name === "latestStart" && earliestStart ? earliestStart : localDateString(RULES.minLeadDays)}
                      {...field}
                      onChange={(e) => {
                        field.onChange(e);
                        revalidateRelated(name);
                      }}
                      aria-invalid={fieldState.invalid}
                      className="h-12 rounded-xl text-base"
                    />
                    {fieldState.invalid && <FieldError errors={[fieldState.error]} />}
                  </Field>
                )}
              />
            ))}
            {numberField("durationDays", "Số ngày của chuyến")}
            {numberField("adults", "Người lớn", "Từ 12 tuổi")}
            {numberField("children", "Trẻ em", "2–11 tuổi")}
            {numberField("infants", "Trẻ sơ sinh", "Dưới 2 tuổi")}
          </CardContent>
        </Card>

        <Card className="rounded-2xl">
          <CardHeader>
            <CardTitle className="text-lg">Mong muốn của bạn (không bắt buộc)</CardTitle>
          </CardHeader>
          <CardContent className="flex flex-col gap-5">
            <div className="grid gap-5 sm:grid-cols-2">
              {(["budgetMin", "budgetMax"] as const).map((name) => (
                <Controller
                  key={name}
                  control={form.control}
                  name={name}
                  render={({ field, fieldState }) => (
                    <Field data-invalid={fieldState.invalid}>
                      <FieldLabel htmlFor={name}>{name === "budgetMin" ? "Ngân sách mỗi người từ" : "Đến"}</FieldLabel>
                      <PriceInput
                        id={name}
                        value={field.value}
                        onChange={(v) => {
                          field.onChange(v);
                          revalidateRelated(name);
                        }}
                        onBlur={field.onBlur}
                        aria-invalid={fieldState.invalid}
                      />
                      {fieldState.invalid && <FieldError errors={[fieldState.error]} />}
                    </Field>
                  )}
                />
              ))}
            </div>
            <Controller
              control={form.control}
              name="categoryIds"
              render={({ field, fieldState }) => (
                <Field data-invalid={fieldState.invalid}>
                  <FieldLabel>Loại hình yêu thích</FieldLabel>
                  {categories.isPending ? (
                    <Skeleton className="h-20 rounded-xl" />
                  ) : (
                    <ChipMultiSelect
                      label="Loại hình yêu thích"
                      options={(categories.data ?? []).map((c) => ({ value: c.id, label: c.name }))}
                      value={field.value}
                      onChange={field.onChange}
                      max={RULES.maxCategories}
                    />
                  )}
                  {fieldState.invalid && <FieldError errors={[fieldState.error]} />}
                </Field>
              )}
            />
            <Controller
              control={form.control}
              name="transportModes"
              render={({ field }) => (
                <Field>
                  <FieldLabel>Phương tiện mong muốn</FieldLabel>
                  <ChipMultiSelect
                    label="Phương tiện mong muốn"
                    options={(Object.keys(TRANSPORT_LABELS) as TransportMode[]).map((m) => ({ value: m, label: TRANSPORT_LABELS[m] }))}
                    value={field.value}
                    onChange={field.onChange}
                  />
                </Field>
              )}
            />
            <Controller
              control={form.control}
              name="accommodationType"
              render={({ field }) => (
                <Field>
                  <FieldLabel htmlFor="accommodationType">Tiêu chuẩn lưu trú</FieldLabel>
                  <Select value={field.value || "ANY"} onValueChange={(v) => field.onChange(v === "ANY" ? "" : v)}>
                    <SelectTrigger id="accommodationType" className="h-12! w-full rounded-xl text-base sm:w-80">
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="ANY">Không yêu cầu</SelectItem>
                      {Object.entries(ACCOMMODATION_LABELS).map(([value, label]) => (
                        <SelectItem key={value} value={value}>
                          {label}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </Field>
              )}
            />
            <Controller
              control={form.control}
              name="notes"
              render={({ field, fieldState }) => (
                <Field data-invalid={fieldState.invalid}>
                  <FieldLabel htmlFor="notes">Ghi chú thêm</FieldLabel>
                  <Textarea
                    id="notes"
                    {...field}
                    rows={4}
                    maxLength={2000}
                    placeholder="Ví dụ: có người lớn tuổi đi cùng, muốn nghỉ ở resort sát biển, ăn chay, kỷ niệm ngày cưới..."
                    className="rounded-xl text-base"
                  />
                  {fieldState.invalid && <FieldError errors={[fieldState.error]} />}
                </Field>
              )}
            />
          </CardContent>
        </Card>

        <div className="flex flex-wrap items-center justify-between gap-3">
          <p className="text-sm text-muted-foreground">Gửi yêu cầu hoàn toàn miễn phí; bạn chỉ trả tiền khi đồng ý với đề xuất.</p>
          <Button type="submit" size="lg" className="h-12 rounded-xl px-6 text-base font-semibold" disabled={form.formState.isSubmitting}>
            {form.formState.isSubmitting ? <Spinner /> : <Send />} Gửi yêu cầu
          </Button>
        </div>
      </form>
    </div>
  );
}
