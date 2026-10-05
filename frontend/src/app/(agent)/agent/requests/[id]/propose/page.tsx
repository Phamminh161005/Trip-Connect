import type { Metadata } from "next";
import { parseIdParam } from "@/lib/route";
import { ApprovedAgentGate } from "../../../ApprovedAgentGate";
import { ProposalFormView } from "./ProposalFormView";

export const metadata: Metadata = { title: "Gửi đề xuất tour" };

export default async function ProposePage({ params }: { params: Promise<{ id: string }> }) {
  const id = parseIdParam((await params).id);
  return (
    <ApprovedAgentGate>
      <ProposalFormView id={id} />
    </ApprovedAgentGate>
  );
}
