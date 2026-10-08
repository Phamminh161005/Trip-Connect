"use client";

import { Controller, useFieldArray, useForm, useWatch } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Plus, X } from "lucide-react";
import { toast } from "sonner";
import { PriceInput } from "@/components/form/PriceInput";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Field, FieldError, FieldLabel } from "@/components/ui/field";
import { Input } from "@/components/ui/input";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Spinner } from "@/components/ui/spinner";
import { Textarea } from "@/components/ui/textarea";
import { ApiError, errorMessage } from "@/lib/api/errors";
import { resolveSettlementDispute } from "@/lib/api/settlements";
import { formatPrice } from "@/lib/tour/labels";
import { whenValid } from "@/lib/validation/whenValid";
import { cn } from "@/lib/utils";
import type { SettlementDetail } from "@/types/settlement";

const adjustmentRow = z.object({
  sign: z.enum(["PLUS", "MINUS"]),
  amount: z.number({ error: "Nhập số tiền" }).int().min(1, "Số tiền phải lớn hơn 0").max(1_000_000_000, "Số tiền quá lớn"),
  reason: z.string().trim().min(1, "Ghi lý do điều chỉnh").max(500, "Tối đa 500 ký tự"),
});

const base = z.object({
  decision: z.enum(["ADJUST", "REJECT"]),
  resolution: z.string().trim().min(1, "Ghi nội dung trả lời đơn vị").max(1000, "Tối đa 1000 ký tự"),
  adjustments: z.array(adjustmentRow),
});

const schema = base.refine((v) => v.decision === "REJECT" || v.adjustments.length > 0, {
  path: ["adjustments"],
  message: "Thêm ít nhất một khoản điều chỉnh",
  when: whenValid(base, "decision"),
});

type Values = z.infer<typeof schema>;

const signed = (row: { sign: "PLUS" | "MINUS"; amount: number }) => (row.sign === "MINUS" ? -row.amount : row.amount);

/** Admin xử lý khiếu nại: điều chỉnh (gửi đơn vị xác nhận lại) hoặc bác (chuyển sang chờ thanh toán). */
export function ResolveDialog({
  settlement,
  onClose,
  onDone,
}: {
  settlement: SettlementDetail;
  onClose: () => void;
  onDone: (updated: SettlementDetail) => void;
}) {
  const s = settlement.summary;
  const form = useForm<Values>({
    resolver: zodResolver(schema),
    mode: "onTouched",
    defaultValues: { decision: "ADJUST", resolution: "", adjustments: [{ sign: "PLUS", amount: undefined as unknown as number, reason: "" }] },
  });
  const rows = useFieldArray({ control: form.control, name: "adjustments" });
  const [decision, adjustments] = useWatch({ control: form.control, name: ["decision", "adjustments"] });
  const delta = decision === "ADJUST" ? adjustments.reduce((sum, a) => sum + (Number.isFinite(a.amount) ? signed(a) : 0), 0) : 0;
  const newPayout = s.payoutAmount + delta;
  const submitting = form.formState.isSubmitting;

  const submit = async (values: Values) => {
    try {
      const updated = await resolveSettlementDispute(s.id, {
        decision: values.decision,
        resolution: values.resolution.trim(),
        adjustments:
          values.decision === "ADJUST" ? values.adjustments.map((a) => ({ amount: signed(a), reason: a.reason.trim() })) : null,
      });
      onDone(updated);
      toast.success(values.decision === "ADJUST" ? "Đã điều chỉnh, gửi đơn vị xác nhận lại" : "Đã bác khiếu nại");
      onClose();
    } catch (error) {
      toast.error(error instanceof ApiError ? errorMessage(error) : "Không xử lý được khiếu nại");
    }
  };

  const listError = form.formState.errors.adjustments?.root?.message ?? form.formState.errors.adjustments?.message;

  return (
    <Dialog open onOpenChange={(open) => !open && !submitting && onClose()}>
      <DialogContent className="max-h-[90vh] overflow-y-auto rounded-2xl sm:max-w-xl">
        <DialogHeader>
          <DialogTitle>Xử lý khiếu nại</DialogTitle>
          <DialogDescription>
            Điều chỉnh: thêm khoản cộng / trừ, đơn vị xác nhận lại trong 5 ngày. Bác khiếu nại: bảng đối soát chuyển sang chờ thanh toán.
          </DialogDescription>
        </DialogHeader>
        <form id="resolve-settlement-form" noValidate onSubmit={form.handleSubmit(submit)} className="flex flex-col gap-5">
          <Controller
            control={form.control}
            name="decision"
            render={({ field }) => (
              <div className="grid grid-cols-2 gap-2" role="radiogroup" aria-label="Cách xử lý">
                {(
                  [
                    ["ADJUST", "Điều chỉnh"],
                    ["REJECT", "Bác khiếu nại"],
                  ] as const
                ).map(([value, label]) => (
                  <button
                    key={value}
                    type="button"
                    role="radio"
                    aria-checked={field.value === value}
                    onClick={() => {
                      field.onChange(value);
                      // Bác khiếu nại thì không có khoản điều chỉnh; quay lại điều chỉnh thì mở sẵn một dòng
                      if (value === "REJECT") rows.replace([]);
                      else if (rows.fields.length === 0) rows.append({ sign: "PLUS", amount: undefined as unknown as number, reason: "" });
                    }}
                    className={cn(
                      "rounded-xl border px-3 py-2.5 text-sm font-medium transition-colors",
                      field.value === value ? "border-primary bg-primary/5 text-primary" : "hover:bg-accent",
                    )}
                  >
                    {label}
                  </button>
                ))}
              </div>
            )}
          />

          {decision === "ADJUST" && (
            <div className="flex flex-col gap-3">
              <FieldLabel>Khoản điều chỉnh</FieldLabel>
              {rows.fields.map((row, index) => (
                <div key={row.id} className="grid gap-2 rounded-xl border p-3 sm:grid-cols-[110px_1fr_auto]">
                  <Controller
                    control={form.control}
                    name={`adjustments.${index}.sign`}
                    render={({ field }) => (
                      <Select value={field.value} onValueChange={field.onChange}>
                        <SelectTrigger className="h-11! rounded-xl" aria-label="Cộng hay trừ">
                          <SelectValue />
                        </SelectTrigger>
                        <SelectContent>
                          <SelectItem value="PLUS">Cộng thêm</SelectItem>
                          <SelectItem value="MINUS">Trừ bớt</SelectItem>
                        </SelectContent>
                      </Select>
                    )}
                  />
                  <Controller
                    control={form.control}
                    name={`adjustments.${index}.amount`}
                    render={({ field, fieldState }) => (
                      <Field data-invalid={fieldState.invalid}>
                        <PriceInput value={field.value} onChange={field.onChange} onBlur={field.onBlur} aria-label="Số tiền" aria-invalid={fieldState.invalid} />
                        {fieldState.invalid && <FieldError errors={[fieldState.error]} />}
                      </Field>
                    )}
                  />
                  <Button
                    type="button"
                    variant="ghost"
                    size="icon"
                    className="rounded-lg"
                    disabled={rows.fields.length <= 1}
                    onClick={() => rows.remove(index)}
                    aria-label="Bỏ khoản này"
                  >
                    <X />
                  </Button>
                  <Controller
                    control={form.control}
                    name={`adjustments.${index}.reason`}
                    render={({ field, fieldState }) => (
                      <Field data-invalid={fieldState.invalid} className="sm:col-span-3">
                        <Input {...field} placeholder="Lý do, ví dụ: bù đơn TC261005123456 bị sót" aria-label="Lý do" aria-invalid={fieldState.invalid} className="h-11 rounded-xl" />
                        {fieldState.invalid && <FieldError errors={[fieldState.error]} />}
                      </Field>
                    )}
                  />
                </div>
              ))}
              {listError && <p className="text-sm text-destructive">{listError}</p>}
              {rows.fields.length < 20 && (
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  className="w-fit rounded-lg"
                  onClick={() => rows.append({ sign: "PLUS", amount: undefined as unknown as number, reason: "" })}
                >
                  <Plus /> Thêm khoản
                </Button>
              )}
              <p className="rounded-xl bg-muted/60 p-3 text-sm">
                Đơn vị nhận: {formatPrice(s.payoutAmount)} → <strong className={cn(newPayout < 0 && "text-destructive")}>{formatPrice(newPayout)}</strong>
                {newPayout < 0 && " (không được âm)"}
              </p>
            </div>
          )}

          <Controller
            control={form.control}
            name="resolution"
            render={({ field, fieldState }) => (
              <Field data-invalid={fieldState.invalid}>
                <FieldLabel htmlFor="resolution">Trả lời đơn vị</FieldLabel>
                <Textarea
                  id="resolution"
                  {...field}
                  rows={3}
                  maxLength={1000}
                  placeholder={decision === "ADJUST" ? "Ví dụ: Đã bổ sung đơn bị sót, mời anh chị xác nhận lại" : "Ví dụ: Đơn TC... khách được hoàn 100% nên không tính vào kỳ này"}
                  aria-invalid={fieldState.invalid}
                  className="rounded-xl"
                />
                {fieldState.invalid && <FieldError errors={[fieldState.error]} />}
              </Field>
            )}
          />
        </form>
        <DialogFooter>
          <Button variant="outline" className="rounded-xl" disabled={submitting} onClick={onClose}>
            Hủy
          </Button>
          <Button type="submit" form="resolve-settlement-form" className="rounded-xl" disabled={submitting || newPayout < 0}>
            {submitting && <Spinner />} {decision === "ADJUST" ? "Điều chỉnh và gửi lại" : "Bác khiếu nại"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
