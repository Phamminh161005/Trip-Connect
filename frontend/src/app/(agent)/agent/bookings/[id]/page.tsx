import type { Metadata } from "next";
import { ManagedBookingView } from "@/components/booking/ManagedBookingView";
import { parseIdParam } from "@/lib/route";
import { ApprovedAgentGate } from "../../ApprovedAgentGate";

export const metadata: Metadata = { title: "Chi tiết đơn đặt tour" };

export default async function AgentBookingPage({ params }: PageProps<"/agent/bookings/[id]">) {
  const id = parseIdParam((await params).id);
  return (
    <ApprovedAgentGate>
      <ManagedBookingView scope="agent" id={id} />
    </ApprovedAgentGate>
  );
}
