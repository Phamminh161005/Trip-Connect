import { toBankHolderName } from "@/lib/validation/auth";

// Các cụm chỉ loại hình doanh nghiệp — bỏ đi để lấy "phần tên riêng" của công ty
const LEGAL_FORM_WORDS = [
  "TRACH NHIEM HUU HAN",
  "HAI THANH VIEN TRO LEN",
  "MOT THANH VIEN",
  "DOANH NGHIEP TU NHAN",
  "HO KINH DOANH",
  "CONG TY",
  "CO PHAN",
  "TNHH",
  "DNTN",
  "MTV",
  "HKD",
  "CTY",
  "CP",
];

/** Chữ in hoa không dấu, chỉ giữ chữ + số, cách nhau đúng 1 dấu cách. */
function simplify(text: string): string {
  return toBankHolderName(text)
    .replace(/[^A-Z0-9]+/g, " ")
    .trim();
}

function coreName(companyName: string): string {
  let result = ` ${simplify(companyName)} `;
  for (const word of LEGAL_FORM_WORDS) result = result.replaceAll(` ${word} `, " ");
  return result.replace(/\s+/g, " ").trim();
}

/**
 * Gợi ý cho quản trị viên: tên chủ tài khoản có chứa tên riêng của công ty không?
 * Chỉ là cảnh báo mềm (ngân hàng có thể viết tắt), quyết định cuối cùng vẫn do người duyệt.
 * Trả về true khi KHÔNG khớp (cần xem kỹ).
 */
export function bankHolderMismatch(companyName: string | null, holder: string | null): boolean {
  if (!companyName || !holder) return false;
  const core = coreName(companyName);
  if (!core) return false;
  return !` ${simplify(holder)} `.includes(` ${core} `);
}
