import { NextRequest, NextResponse } from "next/server";
import type { TokenPairResponse } from "@/types/auth";
import { REFRESH_COOKIE_NAME, clearRefreshCookie, forwardToBackend, setRefreshCookie } from "@/lib/server/authCookie";

export async function POST(request: NextRequest) {
  const refreshToken = request.cookies.get(REFRESH_COOKIE_NAME)?.value;

  if (!refreshToken) {
    return NextResponse.json({ message: "Không có phiên đăng nhập" }, { status: 401 });
  }

  const backendRes = await forwardToBackend("/api/auth/refresh", { refreshToken });

  if (backendRes.status === 502) {
    // Backend không phản hồi: giữ nguyên cookie (phiên vẫn còn), chỉ báo lỗi tạm thời
    return NextResponse.json({ message: "Không kết nối được máy chủ" }, { status: 502 });
  }

  if (!backendRes.ok) {
    // Refresh token sai / hết hạn / đã bị thu hồi -> xóa luôn cookie phía Frontend
    const response = NextResponse.json({ message: "Phiên đăng nhập đã hết hạn" }, { status: 401 });
    clearRefreshCookie(response);
    return response;
  }

  const data: TokenPairResponse = await backendRes.json();

  // Backend xoay vòng refresh token: mỗi lần refresh trả về refresh token MỚI -> ghi đè cookie
  const response = NextResponse.json({ accessToken: data.accessToken });
  setRefreshCookie(response, data.refreshToken);
  return response;
}
