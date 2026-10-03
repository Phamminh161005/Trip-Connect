import type { Metadata } from "next";
import { UnlockAccountForm } from "./UnlockAccountForm";

export const metadata: Metadata = { title: "Mở khóa tài khoản" };

export default async function UnlockAccountPage({ searchParams }: PageProps<"/unlock-account">) {
  const params = await searchParams;
  return <UnlockAccountForm initialEmail={typeof params.email === "string" ? params.email : undefined} />;
}
