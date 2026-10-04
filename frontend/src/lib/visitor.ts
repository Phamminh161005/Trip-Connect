// Mã khách ẩn danh: chuỗi ngẫu nhiên lưu ở trình duyệt, KHÔNG chứa thông tin cá nhân.
// Dùng để ghi lịch sử tìm kiếm khi chưa đăng nhập; đăng nhập xong lịch sử được gộp vào tài khoản.

const KEY = "tc_visitor_id";

/** null nếu trình duyệt chặn lưu trữ (chế độ riêng tư nghiêm ngặt) — khi đó chỉ không ghi lịch sử. */
export function getVisitorId(): string | null {
  if (typeof window === "undefined") return null;
  try {
    let id = window.localStorage.getItem(KEY);
    if (!id) {
      id = crypto.randomUUID();
      window.localStorage.setItem(KEY, id);
    }
    return id;
  } catch {
    return null;
  }
}

export function visitorHeaders(): Record<string, string> {
  const id = getVisitorId();
  return id ? { "X-Visitor-Id": id } : {};
}
