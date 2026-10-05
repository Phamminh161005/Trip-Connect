import { Suspense } from "react";
import type { Metadata } from "next";
import { AdminRequestList } from "./AdminRequestList";

export const metadata: Metadata = { title: "Yêu cầu tour riêng" };

export default function AdminRequestsPage() {
  return (
    <Suspense>
      <AdminRequestList />
    </Suspense>
  );
}
