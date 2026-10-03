/** Chỉ nhận đường dẫn nội bộ cho ?redirect= (chống open redirect). */
export function safeRedirect(target: string | null | undefined, fallback = "/"): string {
  if (!target) return fallback;
  // Phải bắt đầu bằng "/" nhưng không được là "//" (trình duyệt hiểu là domain khác) hay "/\"
  if (!target.startsWith("/") || target.startsWith("//") || target.startsWith("/\\")) return fallback;
  // Không quay về chính các trang đăng nhập / đăng ký
  if (AUTH_PAGES.some((page) => target === page || target.startsWith(`${page}?`) || target.startsWith(`${page}/`))) {
    return fallback;
  }
  return target;
}

export function loginUrl(redirectTo?: string): string {
  return redirectTo ? `/login?redirect=${encodeURIComponent(redirectTo)}` : "/login";
}

/** Các trang dành cho khách chưa đăng nhập (đã đăng nhập thì không cần vào). */
export const AUTH_PAGES = ["/login", "/register"];

/** Tiền tố các trang bắt buộc đăng nhập. */
export const PROTECTED_PREFIXES = ["/account", "/agent", "/admin"];
