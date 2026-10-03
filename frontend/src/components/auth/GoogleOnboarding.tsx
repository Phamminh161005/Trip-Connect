"use client";

import { useState } from "react";
import { Button } from "@/components/ui/button";
import { useAuth } from "@/lib/auth/AuthProvider";
import { errorMessage } from "@/lib/api/errors";
import type { AgentBusinessValues } from "@/lib/validation/auth";
import type { UserProfileResponse } from "@/types/auth";
import { toast } from "sonner";
import { AgentBusinessForm } from "./register/AgentBusinessForm";
import { AgentExpertiseForm } from "./register/AgentExpertiseForm";
import { RoleChooser, type RegisterRole } from "./register/RoleChooser";

export interface PendingGoogleAccount {
  idToken: string;
  email: string;
  fullName: string;
  /** Vai trò đã chọn trước khi bấm nút Google (ở trang Đăng ký) -> không hỏi lại. */
  presetRole?: RegisterRole;
}

interface GoogleOnboardingProps {
  account: PendingGoogleAccount;
  onDone: (user: UserProfileResponse) => void;
  onCancel: () => void;
}

type Step = "role" | "business" | "expertise";

/** Tài khoản Google lần đầu vào TripConnect: chọn vai trò (Agent thì nhập thêm thông tin doanh nghiệp). */
export function GoogleOnboarding({ account, onDone, onCancel }: GoogleOnboardingProps) {
  const { signInWithGoogle } = useAuth();
  // Đã chọn "Đối tác" từ trước -> vào thẳng bước thông tin doanh nghiệp
  const [step, setStep] = useState<Step>(account.presetRole === "AGENT" ? "business" : "role");
  const [business, setBusiness] = useState<AgentBusinessValues | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const finish = async (request: Parameters<typeof signInWithGoogle>[0]) => {
    try {
      const result = await signInWithGoogle(request);
      if (!result.needsRoleSelection) onDone(result.user);
    } catch (error) {
      toast.error(errorMessage(error));
    }
  };

  if (step === "business") {
    return (
      <AgentBusinessForm
        defaultValues={business ?? undefined}
        onBack={() => (account.presetRole === "AGENT" ? onCancel() : setStep("role"))}
        onNext={(values) => {
          setBusiness(values);
          setStep("expertise");
        }}
      />
    );
  }

  if (step === "expertise" && business) {
    return (
      <AgentExpertiseForm
        submitLabel="Hoàn tất đăng ký"
        onBack={() => setStep("business")}
        onSubmit={(expertise) =>
          finish({
            idToken: account.idToken,
            role: "AGENT",
            agentProfile: { ...business, ...expertise },
          })
        }
      />
    );
  }

  return (
    <div className="flex flex-col gap-4">
      <p className="text-center text-sm text-muted-foreground">
        Chào <span className="font-semibold text-foreground">{account.fullName}</span> ({account.email}). Bạn muốn sử dụng
        TripConnect với vai trò nào?
      </p>
      <RoleChooser
        onSelect={async (role) => {
          if (role === "AGENT") {
            setStep("business");
            return;
          }
          setSubmitting(true);
          await finish({ idToken: account.idToken, role: "CUSTOMER" });
          setSubmitting(false);
        }}
      />
      <Button variant="ghost" onClick={onCancel} disabled={submitting}>
        Dùng cách đăng nhập khác
      </Button>
    </div>
  );
}
