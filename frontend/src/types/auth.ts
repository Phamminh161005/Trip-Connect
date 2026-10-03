// Khớp với các DTO của backend (package com.tripconnect.backend.dto)

export type UserRole = "CUSTOMER" | "AGENT" | "ADMIN";
export type AgentStatus = "DRAFT" | "PENDING_APPROVAL" | "NEEDS_REVISION" | "APPROVED";

export interface AgentProfileRequest {
  companyName: string;
  taxCode: string;
  businessLicense?: string;
  /** Tỉnh/Thành trụ sở (id trong danh mục địa điểm). */
  addressProvinceId: number;
  /** Địa chỉ chi tiết: số nhà, đường, phường/xã. */
  address: string;
  /** Mã BIN ngân hàng (Napas / VietQR), vd "970436" = Vietcombank. */
  bankBin: string;
  bankAccountNumber: string;
  bankAccountHolder: string;
  locationIds: number[];
  categoryIds: number[];
}

export interface RegisterRequest {
  fullName: string;
  email: string;
  phone: string;
  password: string;
  role: Exclude<UserRole, "ADMIN">;
  agentProfile?: AgentProfileRequest;
}

export interface LoginRequest {
  email: string;
  password: string;
}

/** Response của backend /api/auth/login (có refreshToken — chỉ route BFF thấy). */
export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  userId: number;
  fullName: string;
  email: string;
  role: UserRole;
  agentStatus: AgentStatus | null;
}

/** Response route BFF trả về trình duyệt: KHÔNG có refreshToken (nằm trong cookie HttpOnly). */
export type BrowserLoginResponse = Omit<LoginResponse, "refreshToken">;

export interface GoogleLoginRequest {
  idToken: string;
  role?: Exclude<UserRole, "ADMIN">;
  agentProfile?: AgentProfileRequest;
}

export interface GoogleAuthResponse {
  needsRoleSelection: boolean;
  email: string;
  fullName: string;
  loginResponse: LoginResponse | null;
}

export type BrowserGoogleAuthResponse = Omit<GoogleAuthResponse, "loginResponse"> & {
  loginResponse: BrowserLoginResponse | null;
};

export interface TokenPairResponse {
  accessToken: string;
  refreshToken: string;
}

export interface UserProfileResponse {
  userId: number;
  fullName: string;
  email: string;
  phone: string | null;
  role: UserRole;
  agentStatus: AgentStatus | null;
}

export interface UpdateMyProfileRequest {
  fullName: string;
  phone: string;
}

export interface VerifyOtpRequest {
  email: string;
  otp: string;
}

export interface ResendOtpRequest {
  email: string;
}

export interface ChangePasswordRequest {
  oldPassword: string;
  newPassword: string;
}

export interface ForgotPasswordRequest {
  email: string;
}

export interface ResetPasswordRequest {
  email: string;
  otp: string;
  newPassword: string;
}
