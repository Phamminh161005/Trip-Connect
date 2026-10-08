"use client";

import { Controller, useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { toast } from "sonner";
import { TextField } from "@/components/form/TextField";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Field, FieldDescription, FieldError, FieldLabel } from "@/components/ui/field";
import { Input } from "@/components/ui/input";
import { Spinner } from "@/components/ui/spinner";
import { errorMessage } from "@/lib/api/errors";
import { paySettlement } from "@/lib/api/settlements";
import { formatPrice } from "@/lib/tour/labels";
import type { SettlementDetail } from "@/types/settlement";

// Khớp FileRule.IMAGE của Backend
const IMAGE_TYPES = ["image/jpeg", "image/png", "image/webp"];
const MAX_BYTES = 5 * 1024 * 1024;

const schema = z.object({
  transactionRef: z.string().trim().min(1, "Vui lòng nhập mã giao dịch ngân hàng").max(100, "Tối đa 100 ký tự"),
  receipt: z
    .instanceof(File)
    .nullable()
    .refine((f) => !f || IMAGE_TYPES.includes(f.type), "Chỉ nhận ảnh JPG, PNG hoặc WebP")
    .refine((f) => !f || f.size <= MAX_BYTES, "Ảnh tối đa 5MB"),
});

type Values = z.infer<typeof schema>;

/** Admin ghi nhận đã chuyển khoản: mã giao dịch bắt buộc, ảnh biên lai không bắt buộc. */
export function PayDialog({
  settlement,
  onClose,
  onDone,
}: {
  settlement: SettlementDetail;
  onClose: () => void;
  onDone: (updated: SettlementDetail) => void;
}) {
  const form = useForm<Values>({
    resolver: zodResolver(schema),
    mode: "onTouched",
    defaultValues: { transactionRef: "", receipt: null },
  });
  const submitting = form.formState.isSubmitting;
  const s = settlement.summary;

  const submit = async (values: Values) => {
    try {
      onDone(await paySettlement(s.id, values.transactionRef.trim(), values.receipt));
      toast.success("Đã ghi nhận thanh toán, đơn vị sẽ nhận thông báo");
      onClose();
    } catch (error) {
      toast.error(errorMessage(error));
    }
  };

  return (
    <Dialog open onOpenChange={(open) => !open && !submitting && onClose()}>
      <DialogContent className="rounded-2xl sm:max-w-md">
        <DialogHeader>
          <DialogTitle>Ghi nhận đã chuyển khoản</DialogTitle>
          <DialogDescription>
            {formatPrice(s.payoutAmount)} cho {s.agentName}
            {settlement.bankAccount && ` · ${settlement.bankAccount.bankName} ${settlement.bankAccount.accountNumber}`}. Sau khi ghi
            nhận, bảng đối soát không sửa được nữa.
          </DialogDescription>
        </DialogHeader>
        <form id="pay-settlement-form" noValidate onSubmit={form.handleSubmit(submit)} className="flex flex-col gap-5">
          <TextField
            control={form.control}
            name="transactionRef"
            label="Mã giao dịch ngân hàng"
            placeholder="Ví dụ: FT26305123456789"
            description="Mã trên biên lai / lịch sử giao dịch của ngân hàng"
          />
          <Controller
            control={form.control}
            name="receipt"
            render={({ field, fieldState }) => (
              <Field data-invalid={fieldState.invalid}>
                <FieldLabel htmlFor="receipt">Ảnh biên lai (không bắt buộc)</FieldLabel>
                <Input
                  id="receipt"
                  type="file"
                  accept={IMAGE_TYPES.join(",")}
                  onChange={(e) => {
                    field.onChange(e.target.files?.[0] ?? null);
                    field.onBlur();
                  }}
                  aria-invalid={fieldState.invalid}
                  className="rounded-xl"
                />
                <FieldDescription>Ảnh chụp màn hình giao dịch thành công, tối đa 5MB.</FieldDescription>
                {fieldState.invalid && <FieldError errors={[fieldState.error]} />}
              </Field>
            )}
          />
        </form>
        <DialogFooter>
          <Button variant="outline" className="rounded-xl" disabled={submitting} onClick={onClose}>
            Hủy
          </Button>
          <Button type="submit" form="pay-settlement-form" className="rounded-xl" disabled={submitting}>
            {submitting && <Spinner />} Ghi nhận đã thanh toán
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
