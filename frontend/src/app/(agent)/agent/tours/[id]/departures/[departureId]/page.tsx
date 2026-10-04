import type { Metadata } from "next";
import { ManifestView } from "@/components/booking/ManifestView";
import { parseIdParam } from "@/lib/route";
import { ApprovedAgentGate } from "../../../../ApprovedAgentGate";

export const metadata: Metadata = { title: "Danh sách đoàn" };

export default async function AgentManifestPage({ params }: PageProps<"/agent/tours/[id]/departures/[departureId]">) {
  const { id, departureId } = await params;
  return (
    <ApprovedAgentGate>
      <ManifestView scope="agent" departureId={parseIdParam(departureId)} backHref={`/agent/tours/${parseIdParam(id)}`} />
    </ApprovedAgentGate>
  );
}
