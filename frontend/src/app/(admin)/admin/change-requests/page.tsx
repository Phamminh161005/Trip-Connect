import type { Metadata } from "next";
import { ChangeRequestList } from "./ChangeRequestList";

export const metadata: Metadata = { title: "Yêu cầu cập nhật hồ sơ" };

export default function AdminChangeRequestsPage() {
  return <ChangeRequestList />;
}
