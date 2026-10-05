import type { Metadata } from "next";
import { parseIdParam } from "@/lib/route";
import { ApprovedAgentGate } from "../../ApprovedAgentGate";
import { AgentRequestView } from "./AgentRequestView";

export const metadata: Metadata = { title: "Chi tiết yêu cầu tư vấn" };

export default async function AgentRequestPage({ params }: { params: Promise<{ id: string }> }) {
  const id = parseIdParam((await params).id);
  return (
    <ApprovedAgentGate>
      <AgentRequestView id={id} />
    </ApprovedAgentGate>
  );
}
