/**
 * Chuẩn hóa chữ để so khớp tiếng Việt không dấu: "Đà Nẵng" -> "da nang".
 * Giống SearchText.normalize của Backend.
 */
export function normalizeText(text: string): string {
  return text
    .normalize("NFD")
    .replace(/\p{M}+/gu, "")
    .replace(/[đĐ]/g, "d")
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, " ")
    .trim();
}

/** Mọi từ trong `keyword` đều là đầu một từ trong `text` (cùng quy tắc với tìm kiếm tour ở Backend). */
export function matchesWords(text: string, keyword: string): boolean {
  const haystack = ` ${normalizeText(text)}`;
  return normalizeText(keyword)
    .split(" ")
    .filter(Boolean)
    .every((word) => haystack.includes(` ${word}`));
}
