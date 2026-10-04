import type { Metadata } from "next";
import { TourEditorView } from "@/components/tour/TourEditorView";
import { ApprovedAgentGate } from "../../ApprovedAgentGate";

export const metadata: Metadata = { title: "Tạo tour mới" };

export default function NewAgentTourPage() {
  return (
    <ApprovedAgentGate>
      <TourEditorView scope="agent" basePath="/agent/tours" />
    </ApprovedAgentGate>
  );
}
