import type { Metadata } from "next";
import { VerifyEmailForm } from "./VerifyEmailForm";

export const metadata: Metadata = { title: "Xác thực email" };

export default async function VerifyEmailPage({ searchParams }: PageProps<"/verify-email">) {
  const params = await searchParams;
  const email = typeof params.email === "string" ? params.email : undefined;
  // Đến từ trang đăng nhập (tài khoản cũ chưa xác thực) -> chưa có mã mới nào vừa được gửi
  return <VerifyEmailForm initialEmail={email} codeJustSent={params.from !== "login"} />;
}
