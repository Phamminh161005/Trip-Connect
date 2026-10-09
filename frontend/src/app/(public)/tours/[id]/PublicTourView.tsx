"use client";

import { useEffect, useRef } from "react";
import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { MapPinOff } from "lucide-react";
import { TourReviewsSection } from "@/components/review/TourReviewsSection";
import { TourDeparturesTable, TourMobileBookingBar, TourPriceBox } from "@/components/tour/TourDeparturesTable";
import { TourDetailContent } from "@/components/tour/TourDetailContent";
import { TourCard } from "@/components/search/TourCard";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { ApiError, errorMessage } from "@/lib/api/errors";
import { getSimilarTours, recordTourView } from "@/lib/api/search";
import { getPublicItineraryFileUrl, getPublicTour } from "@/lib/api/tours";
import { useAuth } from "@/lib/auth/AuthProvider";

/** Trang chi tiết tour cho khách — ai cũng xem được, chỉ tour đang bán. */
export function PublicTourView({ id }: { id: number }) {
  const { data: tour, isPending, error } = useQuery({ queryKey: ["public-tour", id], queryFn: () => getPublicTour(id) });
  const { status } = useAuth();
  const recorded = useRef<number | null>(null);

  // Ghi lượt xem một lần mỗi tour, sau khi biết đã đăng nhập hay chưa (để gắn đúng người xem)
  useEffect(() => {
    if (status === "loading" || !tour || recorded.current === id) return;
    recorded.current = id;
    void recordTourView(id);
  }, [id, status, tour]);

  return (
    // pb-28: chừa chỗ cho thanh đặt tour cố định cuối màn hình trên điện thoại
    <div className="mx-auto w-full max-w-6xl px-4 pt-8 pb-28 sm:px-6 lg:pb-8">
      {isPending ? (
        <div className="flex flex-col gap-4" aria-busy="true">
          <Skeleton className="h-10 w-2/3" />
          <Skeleton className="aspect-[21/9] rounded-2xl" />
          <Skeleton className="h-64 rounded-2xl" />
        </div>
      ) : error ? (
        <div className="flex flex-col items-center gap-4 py-20 text-center">
          <MapPinOff className="size-12 text-muted-foreground" />
          <div>
            <p className="text-lg font-semibold">
              {error instanceof ApiError && error.status === 404 ? "Tour không tồn tại hoặc đã ngừng bán" : "Không tải được tour"}
            </p>
            <p className="text-muted-foreground">{error instanceof ApiError && error.status === 404 ? "" : errorMessage(error)}</p>
          </div>
          <Button asChild className="rounded-xl">
            <Link href="/tours">Xem tour khác</Link>
          </Button>
        </div>
      ) : (
        <>
          <TourDetailContent
            tour={tour}
            getItineraryFileUrl={() => getPublicItineraryFileUrl(id)}
            departures={<TourDeparturesTable departures={tour.departures} bookTourId={tour.id} />}
            aside={<TourPriceBox departures={tour.departures} />}
          />
          <TourReviewsSection
            tourId={tour.id}
            providerName={tour.platformTour ? "TripConnect" : (tour.provider?.companyName ?? "đơn vị tổ chức")}
          />
          <SimilarTours tourId={tour.id} />
        </>
      )}
      {tour && <TourMobileBookingBar departures={tour.departures} />}
    </div>
  );
}

/** Tour đang bán có nội dung gần giống tour đang xem. Không có thì ẩn. */
function SimilarTours({ tourId }: { tourId: number }) {
  const query = useQuery({ queryKey: ["similar-tours", tourId], queryFn: () => getSimilarTours(tourId, 4) });
  if (!query.data?.length) return null;

  return (
    <section className="mt-12 flex flex-col gap-5">
      <h2 className="text-2xl font-bold tracking-tight">Tour tương tự</h2>
      <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-4">
        {query.data.map((t) => (
          <TourCard key={t.id} tour={t} />
        ))}
      </div>
    </section>
  );
}
