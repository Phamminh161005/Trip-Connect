"use client";

import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { MapPinOff } from "lucide-react";
import { TourReviewsSection } from "@/components/review/TourReviewsSection";
import { TourDeparturesTable, TourMobileBookingBar, TourPriceBox } from "@/components/tour/TourDeparturesTable";
import { TourDetailContent } from "@/components/tour/TourDetailContent";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { ApiError, errorMessage } from "@/lib/api/errors";
import { getPublicItineraryFileUrl, getPublicTour } from "@/lib/api/tours";

/** Trang chi tiết tour cho khách — ai cũng xem được, chỉ tour đang bán. */
export function PublicTourView({ id }: { id: number }) {
  const { data: tour, isPending, error } = useQuery({ queryKey: ["public-tour", id], queryFn: () => getPublicTour(id) });

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
        </>
      )}
      {tour && <TourMobileBookingBar departures={tour.departures} />}
    </div>
  );
}
