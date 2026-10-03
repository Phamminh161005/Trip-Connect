"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { toast } from "sonner";
import { AuthCard } from "@/components/auth/AuthCard";
import { EmailStep } from "@/components/auth/EmailStep";
import { OtpVerifyForm } from "@/components/auth/OtpVerifyForm";
import { resendUnlockOtp, unlockAccount } from "@/lib/api/auth";

export function UnlockAccountForm({ initialEmail }: { initialEmail?: string }) {
  const router = useRouter();
  const [email, setEmail] = useState(initialEmail);

  if (!email) {
    return (
      <AuthCard title="Mở khóa tài khoản" description="Nhập email của tài khoản đang bị tạm khóa">
        <EmailStep
          submitLabel="Gửi mã mở khóa"
          onSubmit={async (value) => {
            await resendUnlockOtp(value).catch(() => undefined);
            setEmail(value);
          }}
        />
      </AuthCard>
    );
  }

  return (
    <AuthCard
      title="Mở khóa tài khoản"
      description={
        <>
          Tài khoản bị tạm khóa do nhập sai mật khẩu nhiều lần. Nhập mã 6 số đã gửi tới{" "}
          <span className="font-semibold text-foreground">{email}</span> để mở khóa ngay.
        </>
      }
    >
      <OtpVerifyForm
        submitLabel="Mở khóa"
        onResend={() => resendUnlockOtp(email)}
        onVerify={async (otp) => {
          await unlockAccount({ email, otp });
          toast.success("Mở khóa tài khoản thành công!");
          router.replace(`/login?email=${encodeURIComponent(email)}&reason=unlocked`);
        }}
      />
    </AuthCard>
  );
}
