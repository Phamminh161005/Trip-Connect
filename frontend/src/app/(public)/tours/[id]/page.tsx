import type { Metadata } from "next";
import { parseIdParam } from "@/lib/route";
import type { TourDetail } from "@/types/tour";
import { PublicTourView } from "./PublicTourView";

/** Tiêu đề tab + thẻ chia sẻ lấy từ tên tour (lấy ở server, lưu tạm 60 giây). */
export async function generateMetadata({ params }: PageProps<"/tours/[id]">): Promise<Metadata> {
  const id = Number((await params).id);
  try {
    const res = await fetch(`${process.env.NEXT_PUBLIC_API_URL}/api/tours/${id}`, { next: { revalidate: 60 } });
    if (!res.ok) return { title: "Tour" };
    const tour: TourDetail = await res.json();
    return {
      title: tour.title,
      description: tour.highlights.slice(0, 2).join(" · "),
      openGraph: tour.images[0] ? { images: [tour.images[0].url] } : undefined,
    };
  } catch {
    return { title: "Tour" };
  }
}

export default async function PublicTourPage({ params }: PageProps<"/tours/[id]">) {
  return <PublicTourView id={parseIdParam((await params).id)} />;
}
