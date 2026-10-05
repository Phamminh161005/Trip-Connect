import { z } from "zod";
import { whenValid } from "./whenValid";

// Luật kiểm tra — khớp các DTO của Backend.

const email = z
  .string()
  .trim()
  .min(1, "Email không được để trống")
  .max(254, "Email quá dài")
  .pipe(z.email("Email không đúng định dạng"));

// Mật khẩu mới: >= 8 ký tự, có chữ hoa, chữ thường và số (tối đa 72 vì giới hạn của BCrypt)
const newPassword = z
  .string()
  .min(1, "Mật khẩu không được để trống")
  .max(72, "Mật khẩu tối đa 72 ký tự")
  .regex(/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{8,}$/, "Mật khẩu phải có ít nhất 8 ký tự, gồm chữ hoa, chữ thường và số");

const phone = z
  .string()
  .trim()
  .min(1, "Số điện thoại không được để trống")
  .regex(/^\+?\d{9,15}$/, "Số điện thoại không hợp lệ");

const fullName = z.string().trim().min(1, "Họ tên không được để trống").max(100, "Họ tên tối đa 100 ký tự");

export const otpCode = z.string().regex(/^\d{6}$/, "Mã OTP gồm 6 chữ số");

export const loginSchema = z.object({
  email,
  password: z.string().min(1, "Mật khẩu không được để trống").max(72, "Mật khẩu tối đa 72 ký tự"),
});

const accountInfoBase = z.object({
  fullName,
  email,
  phone,
  password: newPassword,
  confirmPassword: z.string().min(1, "Vui lòng nhập lại mật khẩu"),
});

export const accountInfoSchema = accountInfoBase.refine((data) => data.password === data.confirmPassword, {
  path: ["confirmPassword"],
  message: "Mật khẩu nhập lại không khớp",
  when: whenValid(accountInfoBase, "password", "confirmPassword"),
});

export const agentBusinessSchema = z.object({
  companyName: z.string().trim().min(1, "Tên công ty không được để trống").max(255, "Tối đa 255 ký tự"),
  // Khớp ValidationPatterns.TAX_CODE ở Backend: 10 số (doanh nghiệp), 10 số-3 số (chi nhánh), 12 số (hộ kinh doanh)
  taxCode: z
    .string()
    .trim()
    .min(1, "Mã số thuế không được để trống")
    .regex(/^(\d{10}(-\d{3})?|\d{12})$/, "Mã số thuế gồm 10 số, 10 số-3 số (chi nhánh) hoặc 12 số (hộ kinh doanh)"),
  // Số giấy phép lữ hành KHÔNG hỏi lúc đăng ký — nhập ở trang Hồ sơ kinh doanh, cạnh ô tải file giấy phép
  addressProvinceId: z.number({ error: "Vui lòng chọn Tỉnh/Thành phố của trụ sở" }),
  address: z
    .string()
    .trim()
    .min(10, "Nhập địa chỉ chi tiết: số nhà, đường, phường/xã (tối thiểu 10 ký tự)")
    .max(500, "Tối đa 500 ký tự"),
  // Khớp ValidationPatterns.BANK_BIN / BANK_ACCOUNT_NUMBER ở Backend
  bankBin: z.string().regex(/^\d{6}$/, "Vui lòng chọn ngân hàng trong danh sách"),
  bankAccountNumber: z
    .string()
    .trim()
    .regex(/^\d{6,19}$/, "Số tài khoản chỉ gồm chữ số (6-19 số)"),
  bankAccountHolder: z.string().trim().min(1, "Tên chủ tài khoản không được để trống").max(255, "Tối đa 255 ký tự"),
});

/** Tên chủ tài khoản theo cách ngân hàng hiển thị: CHỮ IN HOA, KHÔNG DẤU (giống BankAccountHolder ở Backend). */
export function toBankHolderName(name: string): string {
  return name
    .normalize("NFD")
    .replace(/\p{M}/gu, "")
    .replace(/đ/g, "d")
    .replace(/Đ/g, "D")
    .replace(/\s+/g, " ")
    .trimStart()
    .toUpperCase();
}

export const agentExpertiseSchema = z.object({
  locationIds: z.array(z.number()).min(1, "Chọn ít nhất 1 khu vực phụ trách").max(50, "Chọn tối đa 50 địa điểm"),
  categoryIds: z.array(z.number()).min(1, "Chọn ít nhất 1 loại hình tour").max(50, "Chọn tối đa 50 loại hình"),
});

export const emailOnlySchema = z.object({ email });

export const otpSchema = z.object({ otp: otpCode });

const resetPasswordBase = z.object({
  otp: otpCode,
  newPassword,
  confirmPassword: z.string().min(1, "Vui lòng nhập lại mật khẩu"),
});

export const resetPasswordSchema = resetPasswordBase.refine((data) => data.newPassword === data.confirmPassword, {
  path: ["confirmPassword"],
  message: "Mật khẩu nhập lại không khớp",
  when: whenValid(resetPasswordBase, "newPassword", "confirmPassword"),
});

const changePasswordBase = z.object({
  oldPassword: z.string().min(1, "Vui lòng nhập mật khẩu hiện tại").max(72, "Mật khẩu tối đa 72 ký tự"),
  newPassword,
  confirmPassword: z.string().min(1, "Vui lòng nhập lại mật khẩu mới"),
});

export const changePasswordSchema = changePasswordBase
  .refine((data) => data.newPassword === data.confirmPassword, {
    path: ["confirmPassword"],
    message: "Mật khẩu nhập lại không khớp",
    when: whenValid(changePasswordBase, "newPassword", "confirmPassword"),
  })
  .refine((data) => data.newPassword !== data.oldPassword, {
    path: ["newPassword"],
    message: "Mật khẩu mới phải khác mật khẩu hiện tại",
    when: whenValid(changePasswordBase, "oldPassword", "newPassword"),
  });

export const profileSchema = z.object({ fullName, phone });

export type LoginValues = z.infer<typeof loginSchema>;
export type AccountInfoValues = z.infer<typeof accountInfoSchema>;
export type AgentBusinessValues = z.infer<typeof agentBusinessSchema>;
export type AgentExpertiseValues = z.infer<typeof agentExpertiseSchema>;
export type EmailOnlyValues = z.infer<typeof emailOnlySchema>;
export type OtpValues = z.infer<typeof otpSchema>;
export type ResetPasswordValues = z.infer<typeof resetPasswordSchema>;
export type ChangePasswordValues = z.infer<typeof changePasswordSchema>;
export type ProfileValues = z.infer<typeof profileSchema>;
