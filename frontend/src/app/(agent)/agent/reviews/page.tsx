import type { Metadata } from "next";
import { ManagedReviewList } from "@/components/review/ManagedReviewList";
import { ApprovedAgentGate } from "../ApprovedAgentGate";

export const metadata: Metadata = { title: "Đánh giá" };

export default function AgentReviewsPage() {
  return (
    <ApprovedAgentGate>
      <ManagedReviewList scope="agent" />
    </ApprovedAgentGate>
  );
}
