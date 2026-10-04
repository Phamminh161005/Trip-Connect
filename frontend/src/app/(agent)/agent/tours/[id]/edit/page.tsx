import type { Metadata } from "next";
import { TourEditorView } from "@/components/tour/TourEditorView";
import { parseIdParam } from "@/lib/route";
import { ApprovedAgentGate } from "../../../ApprovedAgentGate";

export const metadata: Metadata = { title: "Sửa nội dung tour" };

export default async function EditAgentTourPage({ params }: PageProps<"/agent/tours/[id]/edit">) {
  const id = parseIdParam((await params).id);
  return (
    <ApprovedAgentGate>
      <TourEditorView scope="agent" id={id} basePath="/agent/tours" />
    </ApprovedAgentGate>
  );
}
