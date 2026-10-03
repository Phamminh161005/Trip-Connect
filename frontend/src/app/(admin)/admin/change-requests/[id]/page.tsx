import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { ChangeRequestReviewView } from "./ChangeRequestReviewView";

export const metadata: Metadata = { title: "Xét duyệt yêu cầu cập nhật" };

export default async function AdminChangeRequestDetailPage({ params }: PageProps<"/admin/change-requests/[id]">) {
  const id = Number((await params).id);
  if (!Number.isSafeInteger(id) || id <= 0) notFound();
  return <ChangeRequestReviewView id={id} />;
}
