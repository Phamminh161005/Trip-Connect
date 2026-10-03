"use client";

import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Button } from "@/components/ui/button";
import { FieldGroup } from "@/components/ui/field";
import { Spinner } from "@/components/ui/spinner";
import { ApiError } from "@/lib/api/errors";
import { applyApiError } from "@/lib/form/applyApiError";
import { otpSchema, type OtpValues } from "@/lib/validation/auth";
import { OtpField } from "./OtpField";
import { ResendCodeButton } from "./ResendCodeButton";

interface OtpVerifyFormProps {
  submitLabel: string;
  onVerify: (otp: string) => Promise<void>;
  onResend: () => Promise<void>;
  /** Mã vừa được gửi ở bước trước -> khóa nút gửi lại 60 giây. */
  codeJustSent?: boolean;
}

/** Nhập mã OTP 6 số + nút gửi lại mã. Lỗi mã sai hiện ngay dưới ô nhập (kèm số lần thử còn lại từ Backend). */
export function OtpVerifyForm({ submitLabel, onVerify, onResend, codeJustSent = true }: OtpVerifyFormProps) {
  const form = useForm<OtpValues>({ resolver: zodResolver(otpSchema), defaultValues: { otp: "" } });

  const onSubmit = async ({ otp }: OtpValues) => {
    try {
      await onVerify(otp);
    } catch (error) {
      if (error instanceof ApiError && error.status === 400 && !error.hasFieldErrors) {
        form.setError("otp", { type: "server", message: error.message });
        form.setValue("otp", "");
        return;
      }
      applyApiError(error, form.setError);
    }
  };

  return (
    <form noValidate onSubmit={form.handleSubmit(onSubmit)}>
      <FieldGroup className="gap-5">
        <OtpField control={form.control} name="otp" />
        <Button type="submit" size="lg" className="h-12 rounded-xl text-base font-semibold" disabled={form.formState.isSubmitting}>
          {form.formState.isSubmitting && <Spinner />}
          {submitLabel}
        </Button>
        <p className="text-center text-sm text-muted-foreground">
          Không nhận được mã? Kiểm tra mục Spam hoặc <ResendCodeButton onResend={onResend} startCoolingDown={codeJustSent} />
        </p>
      </FieldGroup>
    </form>
  );
}
