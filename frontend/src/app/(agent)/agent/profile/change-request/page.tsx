import type { Metadata } from "next";
import { ChangeRequestView } from "./ChangeRequestView";

export const metadata: Metadata = { title: "Yêu cầu cập nhật hồ sơ" };

export default function ChangeRequestPage() {
  return <ChangeRequestView />;
}
