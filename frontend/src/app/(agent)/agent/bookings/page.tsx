import type { Metadata } from "next";
import { ManagedBookingList } from "@/components/booking/ManagedBookingList";
import { ApprovedAgentGate } from "../ApprovedAgentGate";

export const metadata: Metadata = { title: "Đơn đặt tour" };

export default function AgentBookingsPage() {
  return (
    <ApprovedAgentGate>
      <ManagedBookingList scope="agent" />
    </ApprovedAgentGate>
  );
}
