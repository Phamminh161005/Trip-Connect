import type { Metadata } from "next";
import { ChangePasswordForm } from "./ChangePasswordForm";

export const metadata: Metadata = { title: "Đổi mật khẩu" };

export default function ChangePasswordPage() {
  return <ChangePasswordForm />;
}
