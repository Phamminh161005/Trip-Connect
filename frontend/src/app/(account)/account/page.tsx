import type { Metadata } from "next";
import { ProfileForm } from "./ProfileForm";

export const metadata: Metadata = { title: "Thông tin cá nhân" };

export default function AccountPage() {
  return <ProfileForm />;
}
