"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useForm, useWatch } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { toast } from "sonner";
import { CircleAlert, Info } from "lucide-react";
import { AuthCard } from "@/components/auth/AuthCard";
import { GoogleOnboarding } from "@/components/auth/GoogleOnboarding";
import { GoogleSignInButton, isGoogleEnabled } from "@/components/auth/GoogleSignInButton";
import { useGoogleSignIn } from "@/components/auth/useGoogleSignIn";
import { TextField } from "@/components/form/TextField";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { FieldGroup, FieldSeparator } from "@/components/ui/field";
import { Spinner } from "@/components/ui/spinner";
import { ApiError } from "@/lib/api/errors";
import { useAuth } from "@/lib/auth/AuthProvider";
import { defaultLandingPath } from "@/lib/auth/landing";
import { safeRedirect } from "@/lib/auth/redirect";
import { applyApiError } from "@/lib/form/applyApiError";
import { loginSchema, type LoginValues } from "@/lib/validation/auth";
import type { UserProfileResponse } from "@/types/auth";
import { focusNextOnEnter } from "@/lib/form/focusNextOnEnter";

const REASON_MESSAGES: Record<string, string> = {
  "password-changed": "Đổi mật khẩu thành công. Vui lòng đăng nhập lại bằng mật khẩu mới.",
  "password-reset": "Đặt lại mật khẩu thành công. Hãy đăng nhập bằng mật khẩu mới.",
  verified: "Xác thực email thành công. Hãy đăng nhập để bắt đầu.",
  unlocked: "Mở khóa tài khoản thành công. Hãy đăng nhập lại.",
};

/** Tình huống đặc biệt khi đăng nhập thất bại — hiện hộp hướng dẫn thay vì chỉ báo lỗi. */
type Notice =
  | { kind: "locked"; message: string; email: string }
  | { kind: "unverified"; email: string }
  | { kind: "disabled"; message: string };

interface LoginFormProps {
  redirectTo?: string;
  initialEmail?: string;
  reason?: string;
}

export function LoginForm({ redirectTo, initialEmail, reason }: LoginFormProps) {
  const router = useRouter();
  const { signIn } = useAuth();
  const [notice, setNotice] = useState<Notice | null>(null);

  const goToDestination = (user: UserProfileResponse) => {
    toast.success(`Chào mừng ${user.fullName} quay trở lại!`);
    router.replace(safeRedirect(redirectTo, defaultLandingPath(user)));
  };

  const google = useGoogleSignIn(goToDestination);

  const form = useForm<LoginValues>({
    resolver: zodResolver(loginSchema),
    defaultValues: { email: initialEmail ?? "", password: "" },
  });
  // useWatch (không dùng form.watch) để tương thích React Compiler đang bật trong next.config
  const emailValue = useWatch({ control: form.control, name: "email" });

  const onSubmit = async (values: LoginValues) => {
    setNotice(null);
    try {
      goToDestination(await signIn(values));
    } catch (error) {
      if (error instanceof ApiError) {
        if (error.status === 423) return setNotice({ kind: "locked", message: error.message, email: values.email });
        if (error.status === 403) return setNotice({ kind: "disabled", message: error.message });
        if (error.status === 400 && error.message.includes("chưa được xác thực")) {
          return setNotice({ kind: "unverified", email: values.email });
        }
        if (error.status === 401) {
          form.setError("password", { type: "server", message: error.message }, { shouldFocus: true });
          return;
        }
      }
      applyApiError(error, form.setError);
    }
  };

  if (google.pendingAccount) {
    return (
      <AuthCard title="Hoàn tất đăng ký" description="Chỉ còn một bước nữa để bắt đầu với TripConnect" wide>
        <GoogleOnboarding account={google.pendingAccount} onDone={goToDestination} onCancel={google.cancel} />
      </AuthCard>
    );
  }

  const registerHref = redirectTo ? `/register?redirect=${encodeURIComponent(redirectTo)}` : "/register";

  return (
    <AuthCard
      title="Đăng nhập"
      description="Chào mừng bạn quay lại TripConnect"
      footer={
        <>
          Chưa có tài khoản?{" "}
          <Link href={registerHref} className="font-semibold text-primary underline-offset-4 hover:underline">
            Đăng ký ngay
          </Link>
        </>
      }
    >
      {reason && REASON_MESSAGES[reason] && !notice && (
        <Alert className="mb-5">
          <Info />
          <AlertDescription>{REASON_MESSAGES[reason]}</AlertDescription>
        </Alert>
      )}

      {notice && (
        <Alert variant="destructive" className="mb-5">
          <CircleAlert />
          <AlertDescription className="flex flex-col gap-1.5">
            {notice.kind === "locked" && (
              <>
                <span>{notice.message}</span>
                <Link
                  href={`/unlock-account?email=${encodeURIComponent(notice.email)}`}
                  className="font-semibold underline underline-offset-4"
                >
                  Mở khóa ngay bằng mã OTP
                </Link>
              </>
            )}
            {notice.kind === "unverified" && (
              <>
                <span>Tài khoản chưa được xác thực email.</span>
                <Link
                  href={`/verify-email?email=${encodeURIComponent(notice.email)}&from=login`}
                  className="font-semibold underline underline-offset-4"
                >
                  Xác thực email ngay
                </Link>
              </>
            )}
            {notice.kind === "disabled" && <span>{notice.message}</span>}
          </AlertDescription>
        </Alert>
      )}

      <form noValidate onKeyDown={focusNextOnEnter} onSubmit={form.handleSubmit(onSubmit)}>
        <FieldGroup className="gap-4">
          <TextField control={form.control} name="email" label="Email" type="email" autoComplete="email" autoFocus={!initialEmail} />
          <TextField
            control={form.control}
            name="password"
            label="Mật khẩu"
            type="password"
            autoComplete="current-password"
            autoFocus={Boolean(initialEmail)}
          />
          <div className="-mt-1 flex justify-end">
            <Link
              href={emailValue ? `/forgot-password?email=${encodeURIComponent(emailValue)}` : "/forgot-password"}
              className="text-sm font-medium text-primary underline-offset-4 hover:underline"
            >
              Quên mật khẩu?
            </Link>
          </div>
          <Button type="submit" size="lg" className="h-12 rounded-xl text-base font-semibold" disabled={form.formState.isSubmitting}>
            {form.formState.isSubmitting && <Spinner />}
            Đăng nhập
          </Button>
        </FieldGroup>
      </form>

      {isGoogleEnabled() && (
        <>
          <FieldSeparator className="my-6">hoặc</FieldSeparator>
          <GoogleSignInButton onCredential={google.handleCredential} />
        </>
      )}
    </AuthCard>
  );
}
