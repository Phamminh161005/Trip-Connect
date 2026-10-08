import { Suspense } from "react";
import type { Metadata } from "next";
import { AdminSettlementList } from "./AdminSettlementList";

export const metadata: Metadata = { title: "Đối soát" };

export default function AdminSettlementsPage() {
  return (
    <Suspense>
      <AdminSettlementList />
    </Suspense>
  );
}
