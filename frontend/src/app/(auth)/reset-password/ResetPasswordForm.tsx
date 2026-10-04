"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { toast } from "sonner";
import { AuthCard } from "@/components/auth/AuthCard";
import { EmailStep } from "@/components/auth/EmailStep";
import { OtpField } from "@/components/auth/OtpField";
import { ResendCodeButton } from "@/components/auth/ResendCodeButton";
import { TextField } from "@/components/form/TextField";
import { Button } from "@/components/ui/button";
import { FieldGroup } from "@/components/ui/field";
import { Spinner } from "@/components/ui/spinner";
import { forgotPassword, resetPassword } from "@/lib/api/auth";
import { ApiError } from "@/lib/api/errors";
import { applyApiError } from "@/lib/form/applyApiError";
import { resetPasswordSchema, type ResetPasswordValues } from "@/lib/validation/auth";
import { focusNextOnEnter } from "@/lib/form/focusNextOnEnter";

export function ResetPasswordForm({ initialEmail }: { initialEmail?: string }) {
  const router = useRouter();
  const [email, setEmail] = useState(initialEmail);

  const form = useForm<ResetPasswordValues>({
    resolver: zodResolver(resetPasswordSchema),
    defaultValues: { otp: "", newPassword: "", confirmPassword: "" },
  });

  if (!email) {
    return (
      <AuthCard title="Đặt lại mật khẩu" description="Nhập email của tài khoản cần đặt lại mật khẩu">
        <EmailStep
          submitLabel="Gửi mã"
          onSubmit={async (value) => {
            await forgotPassword({ email: value }).catch(() => undefined);
            setEmail(value);
          }}
        />
      </AuthCard>
    );
  }

  const onSubmit = async (values: ResetPasswordValues) => {
    try {
      await resetPassword({ email, otp: values.otp, newPassword: values.newPassword });
      toast.success("Đặt lại mật khẩu thành công!");
      router.replace(`/login?email=${encodeURIComponent(email)}&reason=password-reset`);
    } catch (error) {
      // Lỗi mã OTP (sai / hết hạn / quá số lần thử) -> hiện ngay dưới ô OTP
      if (error instanceof ApiError && error.status === 400 && !error.hasFieldErrors) {
        form.setError("otp", { type: "server", message: error.message });
        form.setValue("otp", "");
        return;
      }
      applyApiError(error, form.setError);
    }
  };

  return (
    <AuthCard
      title="Đặt lại mật khẩu"
      description={
        <>
          Nhập mã 6 số đã gửi tới <span className="font-semibold text-foreground">{email}</span> và mật khẩu mới
        </>
      }
    >
      <form noValidate onKeyDown={focusNextOnEnter} onSubmit={form.handleSubmit(onSubmit)}>
        <FieldGroup className="gap-4">
          <OtpField control={form.control} name="otp" />
          <TextField
            control={form.control}
            name="newPassword"
            label="Mật khẩu mới"
            type="password"
            autoComplete="new-password"
            description="Ít nhất 8 ký tự, gồm chữ hoa, chữ thường và số."
          />
          <TextField control={form.control} name="confirmPassword" label="Nhập lại mật khẩu mới" type="password" autoComplete="new-password" />
          <Button type="submit" size="lg" className="mt-1 h-12 rounded-xl text-base font-semibold" disabled={form.formState.isSubmitting}>
            {form.formState.isSubmitting && <Spinner />}
            Đặt lại mật khẩu
          </Button>
          <p className="text-center text-sm text-muted-foreground">
            Không nhận được mã? Kiểm tra mục Spam hoặc{" "}
            <ResendCodeButton onResend={() => forgotPassword({ email })} />
          </p>
        </FieldGroup>
      </form>
    </AuthCard>
  );
}
