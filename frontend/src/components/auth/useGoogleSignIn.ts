"use client";

import { useState } from "react";
import { toast } from "sonner";
import { useAuth } from "@/lib/auth/AuthProvider";
import { errorMessage } from "@/lib/api/errors";
import type { UserProfileResponse } from "@/types/auth";
import type { PendingGoogleAccount } from "./GoogleOnboarding";
import type { RegisterRole } from "./register/RoleChooser";

/**
 * Xử lý kết quả nút Google.
 *  - Tài khoản đã có -> đăng nhập luôn (giữ vai trò cũ).
 *  - Tài khoản mới:
 *      presetRole = CUSTOMER -> tạo tài khoản khách ngay (gửi kèm role trong cùng 1 lần gọi)
 *      presetRole = AGENT    -> chuyển sang bước điền thông tin doanh nghiệp
 *      không có presetRole   -> hỏi vai trò (dùng ở trang Đăng nhập)
 */
export function useGoogleSignIn(onSignedIn: (user: UserProfileResponse) => void, presetRole?: RegisterRole) {
  const { signInWithGoogle } = useAuth();
  const [pendingAccount, setPendingAccount] = useState<PendingGoogleAccount | null>(null);

  const handleCredential = async (idToken: string) => {
    try {
      const result = await signInWithGoogle(presetRole === "CUSTOMER" ? { idToken, role: "CUSTOMER" } : { idToken });
      if (result.needsRoleSelection) {
        setPendingAccount({ idToken, email: result.email, fullName: result.fullName, presetRole });
      } else {
        onSignedIn(result.user);
      }
    } catch (error) {
      toast.error(errorMessage(error));
    }
  };

  return { pendingAccount, handleCredential, cancel: () => setPendingAccount(null) };
}
