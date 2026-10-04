import type { Metadata } from "next";
import { ManagedReviewList } from "@/components/review/ManagedReviewList";

export const metadata: Metadata = { title: "Đánh giá" };

export default function AdminReviewsPage() {
  return <ManagedReviewList scope="admin" />;
}
