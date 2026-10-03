import type { Metadata } from "next";
import { ForgotPasswordForm } from "./ForgotPasswordForm";

export const metadata: Metadata = { title: "Quên mật khẩu" };

export default async function ForgotPasswordPage({ searchParams }: PageProps<"/forgot-password">) {
  const params = await searchParams;
  return <ForgotPasswordForm initialEmail={typeof params.email === "string" ? params.email : undefined} />;
}
