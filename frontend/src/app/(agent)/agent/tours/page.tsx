import type { Metadata } from "next";
import { ApprovedAgentGate } from "../ApprovedAgentGate";
import { AgentTourList } from "./AgentTourList";

export const metadata: Metadata = { title: "Tour của tôi" };

export default function AgentToursPage() {
  return (
    <ApprovedAgentGate>
      <AgentTourList />
    </ApprovedAgentGate>
  );
}
