import type { Metadata } from "next";
import { LoginForm } from "./LoginForm";

export const metadata: Metadata = { title: "Đăng nhập" };

function param(value: string | string[] | undefined): string | undefined {
  return typeof value === "string" ? value : undefined;
}

export default async function LoginPage({ searchParams }: PageProps<"/login">) {
  const params = await searchParams;
  return (
    <LoginForm
      redirectTo={param(params.redirect)}
      initialEmail={param(params.email)}
      reason={param(params.reason)}
    />
  );
}
