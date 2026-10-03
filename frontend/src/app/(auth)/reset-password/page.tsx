import type { Metadata } from "next";
import { ResetPasswordForm } from "./ResetPasswordForm";

export const metadata: Metadata = { title: "Đặt lại mật khẩu" };

export default async function ResetPasswordPage({ searchParams }: PageProps<"/reset-password">) {
  const params = await searchParams;
  return <ResetPasswordForm initialEmail={typeof params.email === "string" ? params.email : undefined} />;
}
