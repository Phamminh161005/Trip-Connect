import type { UserProfileResponse } from "@/types/auth";

/**
 * Trang mặc định sau khi đăng nhập (khi không có ?redirect=).
 *  - Admin                 -> Trang quản trị
 *  - Agent chưa được duyệt -> Hồ sơ kinh doanh (việc cần làm ngay: tải giấy tờ, nộp hồ sơ)
 *  - Agent đã duyệt        -> Khu vực Quản lý kinh doanh (nơi làm việc)
 *  - Khách hàng            -> Trang chủ
 */
export function defaultLandingPath(user: UserProfileResponse): string {
  if (user.role === "ADMIN") return "/admin";
  if (user.role === "AGENT") {
    return user.agentStatus === "APPROVED" ? "/agent" : "/agent/profile";
  }
  return "/";
}
