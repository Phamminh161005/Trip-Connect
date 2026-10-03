"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { toast } from "sonner";
import { AuthCard } from "@/components/auth/AuthCard";
import { EmailStep } from "@/components/auth/EmailStep";
import { OtpVerifyForm } from "@/components/auth/OtpVerifyForm";
import { resendEmailOtp, verifyEmailOtp } from "@/lib/api/auth";

export function VerifyEmailForm({ initialEmail, codeJustSent }: { initialEmail?: string; codeJustSent: boolean }) {
  const router = useRouter();
  const [email, setEmail] = useState(initialEmail);

  if (!email) {
    return (
      <AuthCard title="Xác thực email" description="Nhập email bạn đã dùng để đăng ký">
        <EmailStep
          submitLabel="Gửi mã xác thực"
          onSubmit={async (value) => {
            await resendEmailOtp(value).catch(() => undefined);
            setEmail(value);
          }}
        />
      </AuthCard>
    );
  }

  return (
    <AuthCard
      title="Xác thực email"
      description={
        <>
          Nhập mã 6 số đã được gửi tới <span className="font-semibold text-foreground">{email}</span>
        </>
      }
    >
      <OtpVerifyForm
        submitLabel="Xác thực"
        codeJustSent={codeJustSent}
        onResend={() => resendEmailOtp(email)}
        onVerify={async (otp) => {
          await verifyEmailOtp({ email, otp });
          toast.success("Xác thực email thành công!");
          router.replace(`/login?email=${encodeURIComponent(email)}&reason=verified`);
        }}
      />
    </AuthCard>
  );
}
