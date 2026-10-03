import { NextRequest, NextResponse } from "next/server";
import type { BrowserLoginResponse, LoginRequest, LoginResponse } from "@/types/auth";
import { forwardToBackend, setRefreshCookie } from "@/lib/server/authCookie";

export async function POST(request: NextRequest) {
  const body: LoginRequest = await request.json();
  const backendRes = await forwardToBackend("/api/auth/login", body);

  if (!backendRes.ok) {
    // Trả nguyên lỗi của Backend (401 sai mật khẩu, 423 bị khóa, 403 bị vô hiệu hóa...) để giao diện xử lý
    const errorBody = await backendRes.json().catch(() => null);
    return NextResponse.json(errorBody ?? { message: "Đăng nhập thất bại" }, { status: backendRes.status });
  }

  const { refreshToken, ...browserData }: LoginResponse = await backendRes.json();

  // Chỉ trả accessToken + thông tin user về trình duyệt - refreshToken nằm trong cookie HttpOnly
  const response = NextResponse.json(browserData satisfies BrowserLoginResponse);
  setRefreshCookie(response, refreshToken);
  return response;
}
