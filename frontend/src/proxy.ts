import { NextResponse, type NextRequest } from "next/server";
import { AUTH_PAGES, PROTECTED_PREFIXES, safeRedirect } from "@/lib/auth/redirect";

const REFRESH_COOKIE_NAME = "refreshToken";

/** Chuyển trang sớm dựa trên cookie phiên. Phân quyền thật ở Backend. */
export function proxy(request: NextRequest) {
  const { pathname, search } = request.nextUrl;
  const hasSession = request.cookies.has(REFRESH_COOKIE_NAME);

  // Trang cần đăng nhập mà chưa có phiên -> sang trang đăng nhập, nhớ trang cũ để quay lại
  if (!hasSession && PROTECTED_PREFIXES.some((prefix) => pathname === prefix || pathname.startsWith(`${prefix}/`))) {
    const loginUrl = new URL("/login", request.url);
    loginUrl.searchParams.set("redirect", pathname + search);
    return NextResponse.redirect(loginUrl);
  }

  // Đã đăng nhập mà vào trang đăng nhập / đăng ký -> quay về trang đích (hoặc trang chủ)
  if (hasSession && AUTH_PAGES.includes(pathname)) {
    const target = safeRedirect(request.nextUrl.searchParams.get("redirect"));
    return NextResponse.redirect(new URL(target, request.url));
  }

  return NextResponse.next();
}

export const config = {
  // Không chạy cho API, file tĩnh, ảnh — tránh chặn nhầm CSS/JS/ảnh
  matcher: ["/((?!api|_next/static|_next/image|images|favicon.ico|.*\\.(?:png|jpg|jpeg|webp|svg|ico)$).*)"],
};
