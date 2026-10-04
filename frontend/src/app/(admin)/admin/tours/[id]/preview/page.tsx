import type { Metadata } from "next";
import { TourPreviewView } from "@/components/tour/TourPreviewView";
import { parseIdParam } from "@/lib/route";

export const metadata: Metadata = { title: "Xem trước tour" };

export default async function PreviewAdminTourPage({ params }: PageProps<"/admin/tours/[id]/preview">) {
  return <TourPreviewView scope="admin" id={parseIdParam((await params).id)} basePath="/admin/tours" />;
}
