import type { Metadata } from "next";
import { parseIdParam } from "@/lib/route";
import { AdminSettlementView } from "./AdminSettlementView";

export const metadata: Metadata = { title: "Chi tiết đối soát" };

export default async function AdminSettlementPage({ params }: { params: Promise<{ id: string }> }) {
  return <AdminSettlementView id={parseIdParam((await params).id)} />;
}
