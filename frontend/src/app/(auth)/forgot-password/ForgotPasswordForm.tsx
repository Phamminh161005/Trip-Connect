"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { toast } from "sonner";
import { AuthCard } from "@/components/auth/AuthCard";
import { EmailStep } from "@/components/auth/EmailStep";
import { forgotPassword } from "@/lib/api/auth";
import { errorMessage } from "@/lib/api/errors";

export function ForgotPasswordForm({ initialEmail }: { initialEmail?: string }) {
  const router = useRouter();

  return (
    <AuthCard
      title="Quên mật khẩu"
      description="Nhập email của bạn, chúng tôi sẽ gửi mã để đặt lại mật khẩu"
      footer={
        <Link href="/login" className="font-semibold text-primary underline-offset-4 hover:underline">
          ← Quay lại đăng nhập
        </Link>
      }
    >
      <EmailStep
        defaultEmail={initialEmail}
        submitLabel="Gửi mã đặt lại mật khẩu"
        onSubmit={async (email) => {
          try {
            await forgotPassword({ email });
          } catch (error) {
            toast.error(errorMessage(error));
            return;
          }
          // Backend luôn trả 200 (kể cả email không tồn tại) để không lộ thông tin tài khoản
          toast.success("Nếu email đã đăng ký, mã đặt lại mật khẩu sẽ được gửi trong giây lát");
          router.push(`/reset-password?email=${encodeURIComponent(email)}`);
        }}
      />
    </AuthCard>
  );
}
