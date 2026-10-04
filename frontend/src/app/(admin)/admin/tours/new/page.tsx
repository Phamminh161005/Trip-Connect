import type { Metadata } from "next";
import { TourEditorView } from "@/components/tour/TourEditorView";

export const metadata: Metadata = { title: "Tạo tour TripConnect" };

export default function NewAdminTourPage() {
  return <TourEditorView scope="admin" basePath="/admin/tours" />;
}
