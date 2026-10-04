import type { Metadata } from "next";
import { TourManageView } from "@/components/tour/manage/TourManageView";
import { parseIdParam } from "@/lib/route";
import { ApprovedAgentGate } from "../../ApprovedAgentGate";

export const metadata: Metadata = { title: "Quản lý tour" };

export default async function AgentTourPage({ params }: PageProps<"/agent/tours/[id]">) {
  const id = parseIdParam((await params).id);
  return (
    <ApprovedAgentGate>
      <TourManageView scope="agent" id={id} basePath="/agent/tours" />
    </ApprovedAgentGate>
  );
}
