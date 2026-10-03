import type {
  BrowserGoogleAuthResponse,
  BrowserLoginResponse,
  ChangePasswordRequest,
  ForgotPasswordRequest,
  GoogleLoginRequest,
  LoginRequest,
  RegisterRequest,
  ResetPasswordRequest,
  UpdateMyProfileRequest,
  UserProfileResponse,
  VerifyOtpRequest,
} from "@/types/auth";
import { apiRequest, bffRequest } from "./client";

// ----- Đi qua route BFF (có đụng tới cookie refresh token) -----

export const login = (data: LoginRequest) =>
  bffRequest<BrowserLoginResponse>("/api/auth/login", { method: "POST", body: data });

export const loginWithGoogle = (data: GoogleLoginRequest) =>
  bffRequest<BrowserGoogleAuthResponse>("/api/auth/google", { method: "POST", body: data });

export const logout = () => bffRequest<void>("/api/auth/logout", { method: "POST" });

// ----- Gọi thẳng Backend, không cần đăng nhập -----

export const register = (data: RegisterRequest) =>
  apiRequest<void>("/api/auth/register", { method: "POST", body: data, auth: false });

export const verifyEmailOtp = (data: VerifyOtpRequest) =>
  apiRequest<void>("/api/auth/verify-otp", { method: "POST", body: data, auth: false });

export const resendEmailOtp = (email: string) =>
  apiRequest<void>("/api/auth/resend-otp", { method: "POST", body: { email }, auth: false });

export const forgotPassword = (data: ForgotPasswordRequest) =>
  apiRequest<void>("/api/auth/forgot-password", { method: "POST", body: data, auth: false });

export const resetPassword = (data: ResetPasswordRequest) =>
  apiRequest<void>("/api/auth/reset-password", { method: "POST", body: data, auth: false });

export const unlockAccount = (data: VerifyOtpRequest) =>
  apiRequest<void>("/api/auth/unlock-account", { method: "POST", body: data, auth: false });

export const resendUnlockOtp = (email: string) =>
  apiRequest<void>("/api/auth/resend-unlock-otp", { method: "POST", body: { email }, auth: false });

// ----- Cần đăng nhập -----

export const getMe = () => apiRequest<UserProfileResponse>("/api/auth/me");

export const updateMe = (data: UpdateMyProfileRequest) =>
  apiRequest<UserProfileResponse>("/api/users/me", { method: "PUT", body: data });

export const changePassword = (data: ChangePasswordRequest) =>
  apiRequest<void>("/api/auth/change-password", { method: "POST", body: data });
