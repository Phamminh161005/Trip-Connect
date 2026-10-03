import type { Metadata } from "next";
import { AgentProfileView } from "./AgentProfileView";

export const metadata: Metadata = { title: "Hồ sơ kinh doanh" };

export default function AgentProfilePage() {
  return <AgentProfileView />;
}
