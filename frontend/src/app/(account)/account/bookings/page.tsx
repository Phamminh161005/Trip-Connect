import { Suspense } from "react";
import type { Metadata } from "next";
import { MyBookingList } from "./MyBookingList";

export const metadata: Metadata = { title: "Đơn đặt của tôi" };

export default function MyBookingsPage() {
  return (
    <Suspense>
      <MyBookingList />
    </Suspense>
  );
}
