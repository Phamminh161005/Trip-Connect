import { NextRequest, NextResponse } from "next/server";
import { REFRESH_COOKIE_NAME, clearRefreshCookie, forwardToBackend } from "@/lib/server/authCookie";

export async function POST(request: NextRequest) {
  const refreshToken = request.cookies.get(REFRESH_COOKIE_NAME)?.value;

  if (refreshToken) {
    // Backend lỗi cũng không chặn logout phía Frontend - vẫn xóa cookie để
    // người dùng thoát khỏi phiên đăng nhập cục bộ ngay lập tức.
    await forwardToBackend("/api/auth/logout", { refreshToken });
  }

  const response = new NextResponse(null, { status: 204 });
  clearRefreshCookie(response);
  return response;
}
