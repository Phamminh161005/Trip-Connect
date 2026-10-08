import { Suspense } from "react";
import type { Metadata } from "next";
import { ApprovedAgentGate } from "../ApprovedAgentGate";
import { AgentSettlementList } from "./AgentSettlementList";

export const metadata: Metadata = { title: "Đối soát" };

export default function AgentSettlementsPage() {
  return (
    <ApprovedAgentGate>
      <Suspense>
        <AgentSettlementList />
      </Suspense>
    </ApprovedAgentGate>
  );
}
