import { z } from "zod";

// Form "Yêu cầu cập nhật hồ sơ": điền sẵn thông tin hiện tại, người dùng sửa chỗ muốn đổi.
// Luật khớp AgentChangeRequestForm / ValidationPatterns ở Backend.
export const changeRequestSchema = z
  .object({
    companyName: z.string().trim().min(1, "Tên công ty không được để trống").max(255, "Tối đa 255 ký tự"),
    taxCode: z
      .string()
      .trim()
      .regex(/^(\d{10}(-\d{3})?|\d{12})$/, "Mã số thuế gồm 10 số, 10 số-3 số (chi nhánh) hoặc 12 số (hộ kinh doanh)"),
    businessLicense: z.string().trim().min(1, "Số giấy phép lữ hành không được để trống").max(255, "Tối đa 255 ký tự"),
    addressProvinceId: z.number({ error: "Vui lòng chọn Tỉnh/Thành phố" }),
    address: z.string().trim().min(10, "Địa chỉ chi tiết tối thiểu 10 ký tự").max(500, "Tối đa 500 ký tự"),
    changeBank: z.boolean(),
    bankBin: z.string(),
    bankAccountNumber: z.string().trim(),
    bankAccountHolder: z.string().trim().max(255, "Tối đa 255 ký tự"),
    currentPassword: z.string().max(72, "Mật khẩu tối đa 72 ký tự"),
    note: z.string().trim().max(1000, "Ghi chú tối đa 1000 ký tự"),
  })
  // Chỉ kiểm tra các ô ngân hàng khi người dùng chọn "đổi tài khoản ngân hàng"
  .superRefine((data, ctx) => {
    if (!data.changeBank) return;
    if (!/^\d{6}$/.test(data.bankBin)) {
      ctx.addIssue({ code: "custom", path: ["bankBin"], message: "Vui lòng chọn ngân hàng trong danh sách" });
    }
    if (!/^\d{6,19}$/.test(data.bankAccountNumber)) {
      ctx.addIssue({ code: "custom", path: ["bankAccountNumber"], message: "Số tài khoản chỉ gồm chữ số (6-19 số)" });
    }
    if (!data.bankAccountHolder) {
      ctx.addIssue({ code: "custom", path: ["bankAccountHolder"], message: "Tên chủ tài khoản không được để trống" });
    }
    if (!data.currentPassword) {
      ctx.addIssue({
        code: "custom",
        path: ["currentPassword"],
        message: "Nhập mật khẩu hiện tại để xác nhận đổi tài khoản ngân hàng",
      });
    }
  });

export type ChangeRequestValues = z.infer<typeof changeRequestSchema>;
