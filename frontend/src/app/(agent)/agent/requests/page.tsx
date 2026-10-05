import { Suspense } from "react";
import type { Metadata } from "next";
import { ApprovedAgentGate } from "../ApprovedAgentGate";
import { AgentRequestList } from "./AgentRequestList";

export const metadata: Metadata = { title: "Yêu cầu tư vấn" };

export default function AgentRequestsPage() {
  return (
    <ApprovedAgentGate>
      <Suspense>
        <AgentRequestList />
      </Suspense>
    </ApprovedAgentGate>
  );
}
