"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import type { UseFormSetError } from "react-hook-form";
import { toast } from "sonner";
import { AuthCard } from "@/components/auth/AuthCard";
import { GoogleOnboarding } from "@/components/auth/GoogleOnboarding";
import { GoogleSignInButton, isGoogleEnabled } from "@/components/auth/GoogleSignInButton";
import { useGoogleSignIn } from "@/components/auth/useGoogleSignIn";
import { AccountInfoForm } from "@/components/auth/register/AccountInfoForm";
import { AgentBusinessForm } from "@/components/auth/register/AgentBusinessForm";
import { AgentExpertiseForm } from "@/components/auth/register/AgentExpertiseForm";
import { RoleChooser, type RegisterRole } from "@/components/auth/register/RoleChooser";
import { Button } from "@/components/ui/button";
import { FieldSeparator } from "@/components/ui/field";
import { register } from "@/lib/api/auth";
import { ApiError, errorMessage } from "@/lib/api/errors";
import { defaultLandingPath } from "@/lib/auth/landing";
import { safeRedirect } from "@/lib/auth/redirect";
import { applyApiError } from "@/lib/form/applyApiError";
import type { AccountInfoValues, AgentBusinessValues, AgentExpertiseValues } from "@/lib/validation/auth";
import type { RegisterRequest, UserProfileResponse } from "@/types/auth";

type Step = "role" | "account" | "business" | "expertise";

const AGENT_STEPS: Record<Exclude<Step, "role">, { index: number; title: string }> = {
  account: { index: 1, title: "Thông tin tài khoản" },
  business: { index: 2, title: "Thông tin doanh nghiệp" },
  expertise: { index: 3, title: "Khu vực & loại hình tour" },
};

interface RegisterFlowProps {
  initialRole?: RegisterRole;
  redirectTo?: string;
}

export function RegisterFlow({ initialRole, redirectTo }: RegisterFlowProps) {
  const router = useRouter();
  const [role, setRole] = useState<RegisterRole | null>(initialRole ?? null);
  const [step, setStep] = useState<Step>(initialRole ? "account" : "role");
  const [account, setAccount] = useState<AccountInfoValues | null>(null);
  const [business, setBusiness] = useState<AgentBusinessValues | null>(null);
  const [emailError, setEmailError] = useState<string | null>(null);

  // Nút Google nằm BÊN TRONG từng vai trò -> gửi kèm vai trò đã chọn, không phải hỏi lại sau khi chọn tài khoản Google
  const google = useGoogleSignIn((user: UserProfileResponse) => {
    toast.success(`Chào mừng ${user.fullName}!`);
    router.replace(safeRedirect(redirectTo, defaultLandingPath(user)));
  }, role ?? undefined);

  const goToVerify = (email: string) => {
    toast.success("Đăng ký thành công! Mã xác thực đã được gửi tới email của bạn.");
    router.push(`/verify-email?email=${encodeURIComponent(email)}`);
  };

  /** 409 = email đã được dùng -> quay về bước tài khoản và báo lỗi ngay dưới ô email. */
  const handleEmailTaken = (error: unknown): boolean => {
    if (error instanceof ApiError && error.status === 409) {
      setEmailError(error.message);
      setStep("account");
      return true;
    }
    return false;
  };

  const toRequest = (values: AccountInfoValues, requestRole: RegisterRole): RegisterRequest => ({
    fullName: values.fullName,
    email: values.email,
    phone: values.phone,
    password: values.password,
    role: requestRole,
  });

  // ----- Khách du lịch: 1 bước -----
  const submitCustomer = async (values: AccountInfoValues, setError: UseFormSetError<AccountInfoValues>) => {
    try {
      await register(toRequest(values, "CUSTOMER"));
      goToVerify(values.email);
    } catch (error) {
      if (!handleEmailTaken(error)) applyApiError(error, setError);
    }
  };

  // ----- Agent: bước cuối gửi toàn bộ -----
  const submitAgent = async (expertise: AgentExpertiseValues) => {
    if (!account || !business) return;
    try {
      await register({
        ...toRequest(account, "AGENT"),
        agentProfile: { ...business, ...expertise },
      });
      goToVerify(account.email);
    } catch (error) {
      if (!handleEmailTaken(error)) toast.error(errorMessage(error));
    }
  };

  if (google.pendingAccount) {
    return (
      <AuthCard
        wide
        title={google.pendingAccount.presetRole === "AGENT" ? "Đăng ký đối tác" : "Hoàn tất đăng ký"}
        description={
          google.pendingAccount.presetRole === "AGENT"
            ? `Tài khoản Google: ${google.pendingAccount.email}. Bổ sung thông tin doanh nghiệp để hoàn tất.`
            : "Chỉ còn một bước nữa để bắt đầu với TripConnect"
        }
      >
        <GoogleOnboarding account={google.pendingAccount} onDone={(user) => router.replace(safeRedirect(redirectTo, defaultLandingPath(user)))} onCancel={google.cancel} />
      </AuthCard>
    );
  }

  const loginHref = redirectTo ? `/login?redirect=${encodeURIComponent(redirectTo)}` : "/login";
  const loginFooter = (
    <>
      Đã có tài khoản?{" "}
      <Link href={loginHref} className="font-semibold text-primary underline-offset-4 hover:underline">
        Đăng nhập
      </Link>
    </>
  );

  if (step === "role" || !role) {
    return (
      <AuthCard title="Đăng ký tài khoản" description="Bạn muốn tham gia TripConnect với vai trò nào?" footer={loginFooter}>
        <RoleChooser
          onSelect={(selected) => {
            setRole(selected);
            setStep("account");
          }}
        />
      </AuthCard>
    );
  }

  const backToRole = (
    <Button variant="ghost" size="sm" className="mb-4 -ml-2 text-muted-foreground" onClick={() => setStep("role")}>
      ← Đổi loại tài khoản
    </Button>
  );

  const googleSignup = isGoogleEnabled() && (
    <>
      <FieldSeparator className="my-6">hoặc</FieldSeparator>
      <GoogleSignInButton text="signup_with" onCredential={google.handleCredential} />
    </>
  );

  if (role === "CUSTOMER") {
    return (
      <AuthCard title="Đăng ký khách du lịch" description="Tạo tài khoản để đặt tour và theo dõi chuyến đi" footer={loginFooter}>
        {backToRole}
        <AccountInfoForm defaultValues={account ?? undefined} emailError={emailError} submitLabel="Đăng ký" onSubmit={submitCustomer} />
        {googleSignup}
      </AuthCard>
    );
  }

  const current = AGENT_STEPS[step];
  return (
    <AuthCard
      wide
      title="Đăng ký đối tác"
      description={
        <>
          Bước {current.index}/3 · <span className="font-medium text-foreground">{current.title}</span>
        </>
      }
      footer={loginFooter}
    >
      <div className="mb-6 flex gap-2" aria-hidden="true">
        {[1, 2, 3].map((i) => (
          <div key={i} className={`h-1.5 flex-1 rounded-full ${i <= current.index ? "bg-primary" : "bg-muted"}`} />
        ))}
      </div>

      {step === "account" && (
        <>
          {backToRole}
          <AccountInfoForm
            defaultValues={account ?? undefined}
            emailError={emailError}
            submitLabel="Tiếp tục"
            onSubmit={(values) => {
              setAccount(values);
              setEmailError(null);
              setStep("business");
            }}
          />
          {googleSignup}
        </>
      )}
      {step === "business" && (
        <AgentBusinessForm
          defaultValues={business ?? undefined}
          onBack={() => setStep("account")}
          onNext={(values) => {
            setBusiness(values);
            setStep("expertise");
          }}
        />
      )}
      {step === "expertise" && (
        <AgentExpertiseForm submitLabel="Hoàn tất đăng ký" onBack={() => setStep("business")} onSubmit={submitAgent} />
      )}
    </AuthCard>
  );
}
