import { Suspense } from "react";
import type { Metadata } from "next";
import { TourSearchView } from "./TourSearchView";

export const metadata: Metadata = {
  title: "Tìm tour",
  description: "Tìm tour du lịch ghép đoàn trong nước và quốc tế theo điểm đến, ngày đi, giá và thời lượng.",
};

export default function ToursPage() {
  // Suspense: trang đọc bộ lọc từ đường link (useSearchParams)
  return (
    <Suspense>
      <TourSearchView />
    </Suspense>
  );
}
