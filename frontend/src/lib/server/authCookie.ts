import "server-only";
import type { NextResponse } from "next/server";

// Chỉ dùng trong các route BFF (/app/api/auth/*) — chạy trên server Next.js, không bao giờ tới trình duyệt.

export const REFRESH_COOKIE_NAME = "refreshToken";

export const BACKEND_URL = process.env.NEXT_PUBLIC_API_URL;

// Phải bằng thời hạn refresh token của Backend (JWT_REFRESH_EXPIRATION_MS / 1000).
const REFRESH_COOKIE_MAX_AGE_SECONDS = Number(process.env.REFRESH_TOKEN_MAX_AGE_SECONDS ?? 7 * 24 * 60 * 60);

export function setRefreshCookie(response: NextResponse, refreshToken: string): void {
  response.cookies.set(REFRESH_COOKIE_NAME, refreshToken, {
    httpOnly: true, // JavaScript trên trình duyệt không đọc được -> script độc không lấy trộm được
    secure: process.env.NODE_ENV === "production", // chỉ gửi qua HTTPS khi chạy thật
    sameSite: "lax", // trang web khác không gửi kèm cookie này trong request POST
    path: "/",
    maxAge: REFRESH_COOKIE_MAX_AGE_SECONDS,
  });
}

export function clearRefreshCookie(response: NextResponse): void {
  response.cookies.delete(REFRESH_COOKIE_NAME);
}

/** Gửi tiếp request tới Backend; lỗi mạng -> 502 để trình duyệt hiện thông báo phù hợp. */
export async function forwardToBackend(path: string, body: unknown): Promise<Response> {
  try {
    return await fetch(`${BACKEND_URL}${path}`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body),
      cache: "no-store",
      signal: AbortSignal.timeout(10_000), // không chờ Backend quá 10 giây
    });
  } catch {
    return Response.json({ message: "Không kết nối được máy chủ, vui lòng thử lại sau" }, { status: 502 });
  }
}
