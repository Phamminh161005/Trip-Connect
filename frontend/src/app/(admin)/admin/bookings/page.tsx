import type { Metadata } from "next";
import { ManagedBookingList } from "@/components/booking/ManagedBookingList";

export const metadata: Metadata = { title: "Đơn đặt tour" };

export default function AdminBookingsPage() {
  return <ManagedBookingList scope="admin" />;
}
