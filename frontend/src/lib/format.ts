// Định dạng hiển thị theo chuẩn Việt Nam. Chỉ dùng trong Client Component (tránh lệch giờ server/client).

const dateTimeFormat = new Intl.DateTimeFormat("vi-VN", {
  day: "2-digit",
  month: "2-digit",
  year: "numeric",
  hour: "2-digit",
  minute: "2-digit",
  hour12: false,
});

/** Lấy từng phần (ngày, tháng, năm, giờ, phút) để tự sắp thứ tự — không phụ thuộc cách trình duyệt ghép chuỗi. */
function parts(iso: string): Record<string, string> {
  return Object.fromEntries(dateTimeFormat.formatToParts(new Date(iso)).map((p) => [p.type, p.value]));
}

/** Ví dụ: 28/09/2026 23:59 */
export function formatDateTime(iso: string | null | undefined): string {
  if (!iso) return "";
  const p = parts(iso);
  return `${p.day}/${p.month}/${p.year} ${p.hour}:${p.minute}`;
}

/** Ví dụ: 28/09/2026 */
export function formatDate(iso: string | null | undefined): string {
  if (!iso) return "";
  const p = parts(iso);
  return `${p.day}/${p.month}/${p.year}`;
}

/** Ví dụ: "Vừa xong", "5 phút trước", "3 giờ trước", "Hôm qua", "4 ngày trước"; cũ hơn 1 tuần thì ghi ngày. */
export function formatRelative(iso: string, now: number = Date.now()): string {
  const minutes = Math.floor((now - new Date(iso).getTime()) / 60_000);
  if (minutes < 1) return "Vừa xong";
  if (minutes < 60) return `${minutes} phút trước`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours} giờ trước`;
  const days = Math.floor(hours / 24);
  if (days === 1) return "Hôm qua";
  if (days < 7) return `${days} ngày trước`;
  return formatDate(iso);
}

export function formatFileSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(0)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}
