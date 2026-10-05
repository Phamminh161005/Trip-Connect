import type { Metadata } from "next";
import { parseIdParam } from "@/lib/route";
import { AdminRequestView } from "./AdminRequestView";

export const metadata: Metadata = { title: "Điều phối yêu cầu" };

export default async function AdminRequestPage({ params }: { params: Promise<{ id: string }> }) {
  return <AdminRequestView id={parseIdParam((await params).id)} />;
}
