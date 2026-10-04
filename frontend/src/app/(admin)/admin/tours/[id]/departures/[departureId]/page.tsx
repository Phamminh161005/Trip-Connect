import type { Metadata } from "next";
import { ManifestView } from "@/components/booking/ManifestView";
import { parseIdParam } from "@/lib/route";

export const metadata: Metadata = { title: "Danh sách đoàn" };

export default async function AdminManifestPage({ params }: PageProps<"/admin/tours/[id]/departures/[departureId]">) {
  const { id, departureId } = await params;
  return <ManifestView scope="admin" departureId={parseIdParam(departureId)} backHref={`/admin/tours/${parseIdParam(id)}`} />;
}
