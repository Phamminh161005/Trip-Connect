"use client";

import { useState, type ReactNode } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { Controller, useFieldArray, useForm, useWatch, type Path } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { ArrowLeft, Hourglass, MessageSquareQuote, Send } from "lucide-react";
import { toast } from "sonner";
import { PriceInput } from "@/components/form/PriceInput";
import { TextField } from "@/components/form/TextField";
import { ChipMultiSelect, LinesField } from "@/components/tour/form/formParts";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Checkbox } from "@/components/ui/checkbox";
import { Field, FieldDescription, FieldError, FieldGroup, FieldLabel } from "@/components/ui/field";
import { Input } from "@/components/ui/input";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Skeleton } from "@/components/ui/skeleton";
import { Spinner } from "@/components/ui/spinner";
import { Textarea } from "@/components/ui/textarea";
import { travellersText } from "@/lib/booking/labels";
import { ApiError, errorMessage } from "@/lib/api/errors";
import { getAgentCustomRequest, submitProposal } from "@/lib/api/customRequests";
import { CUSTOM_REQUEST_RULES, budgetText, destinationsText, placeName, startWindowText, timeLeftText } from "@/lib/customRequest/labels";
import { focusNextOnEnter } from "@/lib/form/focusNextOnEnter";
import { revalidateTouched } from "@/lib/form/revalidateTouched";
import { formatDateTime } from "@/lib/format";
import { ACCOMMODATION_LABELS, TRANSPORT_LABELS, formatDuration, formatPrice, localDateString } from "@/lib/tour/labels";
import { backendFieldToFormPath, emptyItineraryDay } from "@/lib/validation/tour";
import {
  depositFor,
  proposalFormValues,
  proposalSchema,
  proposalValuesToRequest,
  type ProposalValues,
} from "@/lib/validation/proposal";
import type { CustomRequestDetail } from "@/types/customRequest";
import type { TransportMode } from "@/types/tour";
import { AGENT_REQUESTS_KEY } from "../../AgentRequestList";

export function ProposalFormView({ id }: { id: number }) {
  const query = useQuery({ queryKey: [...AGENT_REQUESTS_KEY, id], queryFn: () => getAgentCustomRequest(id) });

  if (query.isPending) return <Skeleton className="h-96 rounded-2xl" />;
  if (query.isError) return <p className="text-destructive">{errorMessage(query.error)}</p>;
  const r = query.data;

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-col gap-3">
        <Link href={`/agent/requests/${r.id}`} className="flex w-fit items-center gap-1.5 text-sm text-muted-foreground hover:text-foreground">
          <ArrowLeft className="size-4" /> Yêu cầu {r.code}
        </Link>
        <h1 className="text-2xl font-bold tracking-tight">
          {r.stage === "REVISING" ? `Gửi bản chỉnh sửa (bản ${r.proposals.length + 1})` : "Gửi đề xuất lịch trình & báo giá"}
        </h1>
      </div>
      {r.canPropose ? (
        <ProposalForm request={r} />
      ) : (
        <Alert className="rounded-2xl">
          <AlertTitle>Yêu cầu không ở bước chờ bạn gửi đề xuất</AlertTitle>
          <AlertDescription>Có thể khách đang xem đề xuất, yêu cầu đã chốt / đóng hoặc đã quá hạn gửi.</AlertDescription>
        </Alert>
      )}
    </div>
  );
}

const ACCOMMODATION_OPTIONS = Object.entries(ACCOMMODATION_LABELS).map(([value, label]) => ({ value, label }));
const TRANSPORT_OPTIONS = (Object.keys(TRANSPORT_LABELS) as TransportMode[]).map((value) => ({ value, label: TRANSPORT_LABELS[value] }));

function ProposalForm({ request: r }: { request: CustomRequestDetail }) {
  const router = useRouter();
  const queryClient = useQueryClient();
  const previous = r.proposals.at(-1);
  // Khoảng ngày của yêu cầu không đổi trong lúc soạn
  const [schema] = useState(() => proposalSchema(r));
  const form = useForm<ProposalValues>({
    resolver: zodResolver(schema),
    defaultValues: proposalFormValues(r, previous),
    mode: "onTouched",
  });
  const { control } = form;
  const itinerary = useFieldArray({ control, name: "itinerary" });
  const [days, adultPrice, childPrice] = useWatch({ control, name: ["durationDays", "adultPrice", "childPrice"] });
  const validDays = Number.isInteger(days) && days >= 1 ? days : null;
  const total = (adultPrice || 0) * r.adults + (childPrice || 0) * r.children;
  const minStart = localDateString(CUSTOM_REQUEST_RULES.minProposalLeadDays);

  const changeDays = (value: number) => {
    form.setValue("durationDays", value, { shouldValidate: true });
    if (!Number.isInteger(value) || value < 1 || value > CUSTOM_REQUEST_RULES.maxDurationDays) return;
    form.setValue("durationNights", Math.max(value - 1, 0), { shouldValidate: true });
    const current = form.getValues("itinerary").length;
    if (value > current) itinerary.append(Array.from({ length: value - current }, emptyItineraryDay), { shouldFocus: false });
    else if (value < current) itinerary.remove(Array.from({ length: current - value }, (_, i) => value + i));
  };

  const submit = async (values: ProposalValues) => {
    try {
      const updated = await submitProposal(r.id, proposalValuesToRequest(values));
      queryClient.setQueryData([...AGENT_REQUESTS_KEY, r.id], updated);
      void queryClient.invalidateQueries({ queryKey: AGENT_REQUESTS_KEY });
      toast.success("Đã gửi đề xuất cho khách");
      router.push(`/agent/requests/${r.id}`);
    } catch (error) {
      if (error instanceof ApiError && error.hasFieldErrors) {
        Object.entries(error.fieldErrors).forEach(([field, message]) =>
          form.setError(backendFieldToFormPath(field) as Path<ProposalValues>, { type: "server", message }),
        );
        toast.error("Vui lòng kiểm tra lại các ô được đánh dấu");
      } else {
        toast.error(errorMessage(error));
      }
    }
  };

  const itineraryError = form.formState.errors.itinerary?.root?.message ?? form.formState.errors.itinerary?.message;

  return (
    <form noValidate onSubmit={form.handleSubmit(submit)} onKeyDown={focusNextOnEnter} className="flex flex-col gap-6">
      {r.proposalDeadline && (
        <Alert className="rounded-2xl border-amber-200 bg-amber-50 dark:border-amber-900 dark:bg-amber-950/40">
          <Hourglass />
          <AlertTitle>
            Hạn gửi: {formatDateTime(r.proposalDeadline)} ({timeLeftText(r.proposalDeadline)})
          </AlertTitle>
          <AlertDescription>
            Quá hạn, yêu cầu sẽ được chuyển cho đơn vị khác. Khách có {CUSTOM_REQUEST_RULES.responseDays} ngày để đồng ý hoặc yêu cầu chỉnh
            sửa; gửi rồi thì không sửa được bản này.
          </AlertDescription>
        </Alert>
      )}
      {previous?.customerFeedback && (
        <div className="flex gap-3 rounded-2xl border-l-4 border-sky-400 bg-sky-50 p-4 text-sm dark:bg-sky-950/40">
          <MessageSquareQuote className="mt-0.5 size-4 shrink-0 text-muted-foreground" />
          <div>
            <p className="font-medium">Khách yêu cầu chỉnh sửa bản {previous.versionNo}:</p>
            <p className="leading-relaxed whitespace-pre-line">{previous.customerFeedback}</p>
            <p className="mt-1 text-xs text-muted-foreground">Form đã điền sẵn nội dung bản {previous.versionNo}, bạn chỉ cần sửa những chỗ cần thiết.</p>
          </div>
        </div>
      )}

      <Card className="rounded-2xl bg-muted/40">
        <CardContent className="grid gap-x-6 gap-y-2 py-4 text-sm sm:grid-cols-2 lg:grid-cols-3">
          <Info label="Hành trình" value={`${placeName(r.departureLocation)} → ${destinationsText(r.destinations)}`} />
          <Info label="Có thể khởi hành" value={startWindowText(r.earliestStart, r.latestStart)} />
          <Info label="Khách mong muốn" value={`${r.durationDays} ngày · ${travellersText(r.adults, r.children, r.infants)}`} />
          <Info label="Ngân sách" value={budgetText(r.budgetMin, r.budgetMax)} />
          {r.accommodationType && <Info label="Lưu trú mong muốn" value={ACCOMMODATION_LABELS[r.accommodationType]} />}
          {r.notes && <Info label="Ghi chú của khách" value={r.notes} />}
        </CardContent>
      </Card>

      <Section title="Thông tin chung">
        <TextField control={control} name="title" label="Tên chuyến đi" placeholder="Ví dụ: Hà Giang – Đồng Văn 4N3Đ cho gia đình" />
        <div className="grid gap-6 sm:grid-cols-3">
          <Controller
            control={control}
            name="startDate"
            render={({ field, fieldState }) => (
              <Field data-invalid={fieldState.invalid}>
                <FieldLabel htmlFor="startDate">Ngày khởi hành</FieldLabel>
                <Input
                  id="startDate"
                  type="date"
                  {...field}
                  min={r.earliestStart > minStart ? r.earliestStart : minStart}
                  max={r.latestStart}
                  aria-invalid={fieldState.invalid}
                  className="h-12 rounded-xl text-base"
                />
                <FieldDescription>Trong khoảng {startWindowText(r.earliestStart, r.latestStart)}</FieldDescription>
                {fieldState.invalid && <FieldError errors={[fieldState.error]} />}
              </Field>
            )}
          />
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
                  max={CUSTOM_REQUEST_RULES.maxDurationDays}
                  value={Number.isNaN(field.value) || field.value === undefined ? "" : field.value}
                  onChange={(e) => changeDays(e.target.value === "" ? Number.NaN : Number(e.target.value))}
                  onBlur={field.onBlur}
                  aria-invalid={fieldState.invalid}
                  className="h-12 rounded-xl text-base"
                />
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
                <Select value={String(field.value)} onValueChange={(v) => field.onChange(Number(v))} disabled={validDays === null}>
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
      </Section>

      <Section title="Lịch trình" description="Nội dung từng ngày, bữa ăn và nơi nghỉ đêm. Số ngày tự thêm / bớt theo ô Số ngày.">
        {itineraryError && <p className="text-sm text-destructive">{itineraryError}</p>}
        {itinerary.fields.map((item, index) => (
          <fieldset key={item.id} className="flex flex-col gap-4 rounded-2xl border p-4">
            <legend className="px-2 text-sm font-semibold">Ngày {index + 1}</legend>
            <TextField control={control} name={`itinerary.${index}.title`} label="Tiêu đề" placeholder="Ví dụ: Hà Nội – Hà Giang – Quản Bạ" />
            <Controller
              control={control}
              name={`itinerary.${index}.description`}
              render={({ field, fieldState }) => (
                <Field data-invalid={fieldState.invalid}>
                  <FieldLabel htmlFor={`itinerary-${index}-description`}>Nội dung</FieldLabel>
                  <Textarea
                    id={`itinerary-${index}-description`}
                    {...field}
                    rows={4}
                    placeholder={"07:00 Xe đón đoàn tại điểm hẹn...\n12:00 Ăn trưa..."}
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
              <TextField control={control} name={`itinerary.${index}.accommodation`} label="Nơi nghỉ đêm (không bắt buộc)" placeholder="Ví dụ: Homestay Lô Lô Chải" />
            </div>
          </fieldset>
        ))}
      </Section>

      <Section title="Dịch vụ">
        <Controller
          control={control}
          name="transportModes"
          render={({ field, fieldState }) => (
            <Field data-invalid={fieldState.invalid}>
              <FieldLabel>Phương tiện di chuyển</FieldLabel>
              <ChipMultiSelect
                label="Phương tiện di chuyển"
                options={TRANSPORT_OPTIONS}
                value={field.value as TransportMode[]}
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
              <Select value={field.value || undefined} onValueChange={field.onChange}>
                <SelectTrigger id="accommodationType" className="h-12! w-full rounded-xl text-base sm:w-80" aria-invalid={fieldState.invalid}>
                  <SelectValue placeholder="Chọn tiêu chuẩn lưu trú" />
                </SelectTrigger>
                <SelectContent>
                  {ACCOMMODATION_OPTIONS.map((o) => (
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
          label="Giá bao gồm"
          placeholder="Ví dụ: Xe 16 chỗ đưa đón suốt hành trình"
          addLabel="Thêm dịch vụ"
          min={1}
          max={30}
        />
        <LinesField
          control={control}
          name="excludedServices"
          label="Không bao gồm (không bắt buộc)"
          placeholder="Ví dụ: Chi tiêu cá nhân, đồ uống"
          addLabel="Thêm dòng"
          max={30}
        />
        <Controller
          control={control}
          name="notes"
          render={({ field, fieldState }) => (
            <Field data-invalid={fieldState.invalid}>
              <FieldLabel htmlFor="notes">Lưu ý (không bắt buộc)</FieldLabel>
              <Textarea id="notes" {...field} rows={3} placeholder="Giấy tờ, trang phục, sức khỏe..." aria-invalid={fieldState.invalid} className="rounded-xl text-base" />
              {fieldState.invalid && <FieldError errors={[fieldState.error]} />}
            </Field>
          )}
        />
      </Section>

      <Section title="Báo giá" description="Giá trọn gói mỗi khách (VNĐ). Trẻ sơ sinh miễn phí.">
        <div className="grid gap-6 sm:grid-cols-2">
          <Controller
            control={control}
            name="adultPrice"
            render={({ field, fieldState }) => (
              <Field data-invalid={fieldState.invalid}>
                <FieldLabel htmlFor="adultPrice">Giá người lớn</FieldLabel>
                <PriceInput
                  id="adultPrice"
                  value={field.value}
                  onChange={(v) => {
                    field.onChange(v);
                    revalidateTouched(form, "childPrice");
                  }}
                  onBlur={field.onBlur}
                  aria-invalid={fieldState.invalid}
                />
                {fieldState.invalid && <FieldError errors={[fieldState.error]} />}
              </Field>
            )}
          />
          <Controller
            control={control}
            name="childPrice"
            render={({ field, fieldState }) => (
              <Field data-invalid={fieldState.invalid}>
                <FieldLabel htmlFor="childPrice">Giá trẻ em</FieldLabel>
                <PriceInput id="childPrice" value={field.value} onChange={field.onChange} onBlur={field.onBlur} aria-invalid={fieldState.invalid} />
                {r.children === 0 && <FieldDescription>Đoàn không có trẻ em — nhập 0 nếu không áp dụng.</FieldDescription>}
                {fieldState.invalid && <FieldError errors={[fieldState.error]} />}
              </Field>
            )}
          />
        </div>
        <div className="flex flex-col gap-1.5 rounded-xl bg-muted/60 p-4 text-sm">
          <p className="flex justify-between">
            <span className="text-muted-foreground">Số khách</span>
            <span>{travellersText(r.adults, r.children, r.infants)}</span>
          </p>
          <p className="flex items-baseline justify-between border-t pt-2">
            <span className="font-medium">Tổng khách thanh toán</span>
            <span className="text-lg font-bold text-primary">{formatPrice(total)}</span>
          </p>
          <p className="flex justify-between">
            <span className="text-muted-foreground">Đặt cọc {CUSTOM_REQUEST_RULES.depositPercent}% khi khách xác nhận</span>
            <span className="font-medium">{formatPrice(depositFor(total))}</span>
          </p>
        </div>
      </Section>

      <Section title="Lời nhắn cho khách (không bắt buộc)">
        <Controller
          control={control}
          name="message"
          render={({ field, fieldState }) => (
            <Field data-invalid={fieldState.invalid}>
              <Textarea
                id="message"
                aria-label="Lời nhắn cho khách"
                {...field}
                rows={3}
                placeholder={previous ? "Ví dụ: Đã đổi sang khách sạn 4 sao và bỏ ngày tham quan bảo tàng theo ý anh chị." : "Giới thiệu ngắn về chuyến đi bạn thiết kế"}
                aria-invalid={fieldState.invalid}
                className="rounded-xl text-base"
              />
              {fieldState.invalid && <FieldError errors={[fieldState.error]} />}
            </Field>
          )}
        />
      </Section>

      <div className="flex flex-wrap items-center justify-between gap-3">
        <Button asChild type="button" variant="ghost" className="h-11 rounded-xl">
          <Link href={`/agent/requests/${r.id}`}>Hủy</Link>
        </Button>
        <Button type="submit" className="h-11 rounded-xl px-6 font-semibold" disabled={form.formState.isSubmitting}>
          {form.formState.isSubmitting ? <Spinner /> : <Send />}
          Gửi đề xuất cho khách
        </Button>
      </div>
    </form>
  );
}

function Section({ title, description, children }: { title: string; description?: string; children: ReactNode }) {
  return (
    <Card className="rounded-2xl">
      <CardHeader>
        <CardTitle className="text-lg">{title}</CardTitle>
        {description && <CardDescription>{description}</CardDescription>}
      </CardHeader>
      <CardContent>
        <FieldGroup className="gap-6">{children}</FieldGroup>
      </CardContent>
    </Card>
  );
}

function Info({ label, value }: { label: string; value: string }) {
  return (
    <div className="min-w-0">
      <p className="text-xs text-muted-foreground">{label}</p>
      <p className="line-clamp-3 font-medium">{value}</p>
    </div>
  );
}
