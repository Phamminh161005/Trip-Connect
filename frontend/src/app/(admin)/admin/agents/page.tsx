import type { Metadata } from "next";
import { AgentProfileList } from "./AgentProfileList";

export const metadata: Metadata = { title: "Hồ sơ đối tác" };

export default function AdminAgentsPage() {
  return <AgentProfileList />;
}
