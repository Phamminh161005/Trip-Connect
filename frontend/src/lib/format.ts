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

export function formatFileSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(0)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}
