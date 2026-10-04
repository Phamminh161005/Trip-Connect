import type { Metadata } from "next";
import { ManagedBookingView } from "@/components/booking/ManagedBookingView";
import { parseIdParam } from "@/lib/route";

export const metadata: Metadata = { title: "Chi tiết đơn đặt tour" };

export default async function AdminBookingPage({ params }: PageProps<"/admin/bookings/[id]">) {
  return <ManagedBookingView scope="admin" id={parseIdParam((await params).id)} />;
}
