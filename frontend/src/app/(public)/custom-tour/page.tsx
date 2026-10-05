import { Suspense } from "react";
import type { Metadata } from "next";
import { RequireAuth } from "@/components/auth/RequireAuth";
import { CustomRequestForm } from "./CustomRequestForm";
import { CustomerOnly } from "./CustomerOnly";

export const metadata: Metadata = { title: "Thiết kế tour riêng" };

/** Gửi yêu cầu thiết kế tour riêng — chỉ khách hàng; chưa đăng nhập sẽ được đưa tới trang đăng nhập rồi quay lại. */
export default function CustomTourPage() {
  return (
    <Suspense>
      <RequireAuth>
        <CustomerOnly>
          <CustomRequestForm />
        </CustomerOnly>
      </RequireAuth>
    </Suspense>
  );
}
