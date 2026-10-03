import type { Metadata } from "next";
import { RegisterFlow } from "./RegisterFlow";

export const metadata: Metadata = { title: "Đăng ký" };

export default async function RegisterPage({ searchParams }: PageProps<"/register">) {
  const params = await searchParams;
  const redirect = typeof params.redirect === "string" ? params.redirect : undefined;
  return <RegisterFlow initialRole={params.type === "agent" ? "AGENT" : undefined} redirectTo={redirect} />;
}
