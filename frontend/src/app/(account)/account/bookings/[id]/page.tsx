import type { Metadata } from "next";
import { parseIdParam } from "@/lib/route";
import { MyBookingView } from "./MyBookingView";

export const metadata: Metadata = { title: "Chi tiết đơn đặt tour" };

export default async function MyBookingPage({ params }: PageProps<"/account/bookings/[id]">) {
  return <MyBookingView id={parseIdParam((await params).id)} />;
}
