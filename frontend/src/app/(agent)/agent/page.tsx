import type { Metadata } from "next";
import { AgentDashboard } from "./AgentDashboard";

export const metadata: Metadata = { title: "Tổng quan kinh doanh" };

export default function AgentHomePage() {
  return <AgentDashboard />;
}
