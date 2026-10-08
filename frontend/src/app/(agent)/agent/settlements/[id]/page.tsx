import type { Metadata } from "next";
import { parseIdParam } from "@/lib/route";
import { ApprovedAgentGate } from "../../ApprovedAgentGate";
import { AgentSettlementView } from "./AgentSettlementView";

export const metadata: Metadata = { title: "Chi tiết đối soát" };

export default async function AgentSettlementPage({ params }: { params: Promise<{ id: string }> }) {
  const id = parseIdParam((await params).id);
  return (
    <ApprovedAgentGate>
      <AgentSettlementView id={id} />
    </ApprovedAgentGate>
  );
}
