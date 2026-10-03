import { NextRequest, NextResponse } from "next/server";
import type { BrowserGoogleAuthResponse, GoogleAuthResponse, GoogleLoginRequest } from "@/types/auth";
import { forwardToBackend, setRefreshCookie } from "@/lib/server/authCookie";

export async function POST(request: NextRequest) {
  const body: GoogleLoginRequest = await request.json();
  const backendRes = await forwardToBackend("/api/auth/google", body);

  if (!backendRes.ok) {
    const errorBody = await backendRes.json().catch(() => null);
    return NextResponse.json(errorBody ?? { message: "Đăng nhập Google thất bại" }, { status: backendRes.status });
  }

  const data: GoogleAuthResponse = await backendRes.json();

  // Tài khoản mới -> chưa có token, Frontend hỏi vai trò rồi gọi lại
  if (!data.loginResponse) {
    return NextResponse.json({ ...data, loginResponse: null } satisfies BrowserGoogleAuthResponse);
  }

  const { refreshToken, ...browserLogin } = data.loginResponse;
  const response = NextResponse.json({ ...data, loginResponse: browserLogin } satisfies BrowserGoogleAuthResponse);
  setRefreshCookie(response, refreshToken);
  return response;
}
