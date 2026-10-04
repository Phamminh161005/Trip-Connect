"use client";

import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from "react";
import * as authApi from "@/lib/api/auth";
import { claimSearchHistory } from "@/lib/api/search";
import { onSessionExpired, refreshAccessToken } from "@/lib/api/client";
import { setAccessToken } from "@/lib/tokenStore";
import type { GoogleLoginRequest, LoginRequest, UserProfileResponse } from "@/types/auth";

type AuthStatus = "loading" | "authenticated" | "guest";

export type GoogleSignInResult =
  | { needsRoleSelection: false; user: UserProfileResponse }
  | { needsRoleSelection: true; email: string; fullName: string };

interface AuthContextValue {
  status: AuthStatus;
  user: UserProfileResponse | null;
  /** Đăng nhập bằng email + mật khẩu. Ném ApiError nếu thất bại. */
  signIn: (data: LoginRequest) => Promise<UserProfileResponse>;
  /**
   * Đăng nhập bằng Google. Tài khoản mới -> { needsRoleSelection: true } để giao diện hỏi vai trò,
   * rồi gọi lại kèm `role` để hoàn tất đăng ký.
   */
  signInWithGoogle: (data: GoogleLoginRequest) => Promise<GoogleSignInResult>;
  signOut: () => Promise<void>;
  /** Tải lại thông tin người dùng (sau khi sửa hồ sơ...). */
  reloadUser: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [status, setStatus] = useState<AuthStatus>("loading");
  const [user, setUser] = useState<UserProfileResponse | null>(null);

  const becomeGuest = useCallback(() => {
    setAccessToken(null);
    setUser(null);
    setStatus("guest");
  }, []);

  const loadUser = useCallback(async () => {
    const me = await authApi.getMe();
    setUser(me);
    setStatus("authenticated");
    // Gộp lịch sử tìm kiếm lúc chưa đăng nhập vào tài khoản (chạy ngầm; lỗi cũng không ảnh hưởng đăng nhập)
    claimSearchHistory().catch(() => {});
    return me;
  }, []);

  // Mở web: âm thầm khôi phục phiên từ cookie refresh token. Không có phiên -> ở chế độ khách, KHÔNG chuyển trang.
  useEffect(() => {
    let cancelled = false;
    (async () => {
      const token = await refreshAccessToken();
      if (cancelled) return;
      if (!token) {
        becomeGuest();
        return;
      }
      try {
        await loadUser();
      } catch {
        if (!cancelled) becomeGuest();
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [becomeGuest, loadUser]);

  // Refresh thất bại giữa chừng (phiên bị thu hồi / hết hạn hẳn) -> về chế độ khách
  useEffect(() => onSessionExpired(becomeGuest), [becomeGuest]);

  const signIn = useCallback(
    async (data: LoginRequest) => {
      const result = await authApi.login(data);
      setAccessToken(result.accessToken);
      return loadUser();
    },
    [loadUser],
  );

  const signInWithGoogle = useCallback(
    async (data: GoogleLoginRequest): Promise<GoogleSignInResult> => {
      const result = await authApi.loginWithGoogle(data);
      if (result.needsRoleSelection || !result.loginResponse) {
        return { needsRoleSelection: true, email: result.email, fullName: result.fullName };
      }
      setAccessToken(result.loginResponse.accessToken);
      return { needsRoleSelection: false, user: await loadUser() };
    },
    [loadUser],
  );

  const signOut = useCallback(async () => {
    try {
      await authApi.logout();
    } finally {
      becomeGuest();
    }
  }, [becomeGuest]);

  const reloadUser = useCallback(async () => {
    await loadUser();
  }, [loadUser]);

  const value = useMemo(
    () => ({ status, user, signIn, signInWithGoogle, signOut, reloadUser }),
    [status, user, signIn, signInWithGoogle, signOut, reloadUser],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) throw new Error("useAuth phải được dùng bên trong <AuthProvider>");
  return context;
}
