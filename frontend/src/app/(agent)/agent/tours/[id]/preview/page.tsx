import type { Metadata } from "next";
import { TourPreviewView } from "@/components/tour/TourPreviewView";
import { parseIdParam } from "@/lib/route";
import { ApprovedAgentGate } from "../../../ApprovedAgentGate";

export const metadata: Metadata = { title: "Xem trước tour" };

export default async function PreviewAgentTourPage({ params }: PageProps<"/agent/tours/[id]/preview">) {
  const id = parseIdParam((await params).id);
  return (
    <ApprovedAgentGate>
      <TourPreviewView scope="agent" id={id} basePath="/agent/tours" />
    </ApprovedAgentGate>
  );
}
