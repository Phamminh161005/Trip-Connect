import type { Metadata } from "next";
import { parseIdParam } from "@/lib/route";
import { AdminTourView } from "./AdminTourView";

export const metadata: Metadata = { title: "Chi tiết tour" };

export default async function AdminTourPage({ params }: PageProps<"/admin/tours/[id]">) {
  return <AdminTourView id={parseIdParam((await params).id)} />;
}
