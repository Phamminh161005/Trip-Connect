"use client";

import Link from "next/link";
import { ArrowLeft } from "lucide-react";
import { Skeleton } from "@/components/ui/skeleton";
import { errorMessage } from "@/lib/api/errors";
import { getManagedItineraryFileUrl, type TourScope } from "@/lib/api/tours";
import { TourDeparturesTable, TourPriceBox } from "./TourDeparturesTable";
import { TourDetailContent } from "./TourDetailContent";
import { useManagedTour } from "./manage/tourQueries";
import { TourStatusBadge } from "./TourStatusBadge";

/** Xem trước tour giống trang khách (dùng khi tour chưa công khai). */
export function TourPreviewView({ scope, id, basePath }: { scope: TourScope; id: number; basePath: string }) {
  const { data: tour, isPending, error } = useManagedTour(scope, id);
  if (isPending) return <Skeleton className="h-96 rounded-2xl" />;
  if (error) return <p className="text-destructive">{errorMessage(error)}</p>;

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-dashed border-primary/40 bg-primary/5 px-4 py-3 text-sm">
        <span className="flex items-center gap-2">
          <strong>Bản xem trước</strong> — khách sẽ thấy tour như bên dưới khi tour được công khai.
          <TourStatusBadge status={tour.status} />
        </span>
        <Link href={`${basePath}/${id}`} className="flex items-center gap-1 font-medium text-primary hover:underline">
          <ArrowLeft className="size-4" /> Quay lại quản lý tour
        </Link>
      </div>
      <TourDetailContent
        tour={tour}
        getItineraryFileUrl={() => getManagedItineraryFileUrl(scope, id)}
        departures={<TourDeparturesTable departures={tour.departures} />}
        aside={<TourPriceBox departures={tour.departures} />}
      />
    </div>
  );
}
