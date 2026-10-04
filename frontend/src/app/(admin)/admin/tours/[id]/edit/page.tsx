import type { Metadata } from "next";
import { TourEditorView } from "@/components/tour/TourEditorView";
import { parseIdParam } from "@/lib/route";

export const metadata: Metadata = { title: "Sửa nội dung tour" };

export default async function EditAdminTourPage({ params }: PageProps<"/admin/tours/[id]/edit">) {
  return <TourEditorView scope="admin" id={parseIdParam((await params).id)} basePath="/admin/tours" />;
}
