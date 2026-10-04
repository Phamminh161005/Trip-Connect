import Image from "next/image";
import Link from "next/link";
import { CalendarDays, Check, Clock, Globe, ImageOff, MapPin, Star } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { formatDay, formatDuration, formatPrice } from "@/lib/tour/labels";
import type { TourCard as TourCardData } from "@/types/search";

/** Thẻ tour ở trang tìm kiếm và trang chủ. Cả thẻ là một link tới trang chi tiết. */
export function TourCard({ tour, priority }: { tour: TourCardData; priority?: boolean }) {
  return (
    <Link
      href={`/tours/${tour.id}`}
      className="group flex flex-col overflow-hidden rounded-2xl border bg-card transition-shadow outline-none hover:shadow-lg focus-visible:ring-3 focus-visible:ring-ring/50"
    >
      <div className="relative aspect-[4/3] overflow-hidden bg-muted">
        {tour.coverImageUrl ? (
          <Image
            src={tour.coverImageUrl}
            alt={tour.title}
            fill
            priority={priority}
            sizes="(min-width: 1280px) 25vw, (min-width: 768px) 33vw, (min-width: 640px) 50vw, 100vw"
            className="object-cover transition-transform duration-300 group-hover:scale-105"
          />
        ) : (
          <ImageOff className="absolute inset-0 m-auto size-8 text-muted-foreground" />
        )}
        {tour.international && (
          <Badge className="absolute top-3 left-3 border-0 bg-violet-600 text-white">
            <Globe /> Quốc tế
          </Badge>
        )}
        {tour.ratingCount > 0 && tour.rating !== null && (
          <span className="absolute top-3 right-3 flex items-center gap-1 rounded-full bg-black/60 px-2 py-0.5 text-xs font-medium text-white">
            <Star className="size-3 fill-amber-400 text-amber-400" /> {tour.rating.toFixed(1)}
          </span>
        )}
      </div>

      <div className="flex flex-1 flex-col gap-2.5 p-4">
        <p className="flex items-center gap-1 truncate text-xs text-muted-foreground">
          <MapPin className="size-3.5 shrink-0" /> {tour.destinations.join(" · ")}
        </p>
        <h3 className="line-clamp-2 font-semibold leading-snug group-hover:text-primary">{tour.title}</h3>
        <div className="flex flex-wrap gap-x-3 gap-y-1 text-xs text-muted-foreground">
          <span className="flex items-center gap-1">
            <Clock className="size-3.5" /> {formatDuration(tour.durationDays, tour.durationNights)}
          </span>
          {tour.departureLocation && <span>Khởi hành từ {tour.departureLocation}</span>}
        </div>
        {tour.highlights.length > 0 && (
          <ul className="flex flex-col gap-1 text-sm">
            {tour.highlights.map((h, index) => (
              <li key={index} className="flex items-start gap-1.5">
                <Check className="mt-0.5 size-3.5 shrink-0 text-emerald-600" />
                <span className="line-clamp-1">{h}</span>
              </li>
            ))}
          </ul>
        )}

        <div className="mt-auto flex items-end justify-between gap-2 border-t pt-3">
          <div className="min-w-0 text-xs text-muted-foreground">
            <p className="flex items-center gap-1">
              <CalendarDays className="size-3.5" /> {formatDay(tour.nextDepartureDate)}
              {tour.departureCount > 1 && ` +${tour.departureCount - 1} lịch`}
            </p>
            <p className="truncate">{tour.providerName}</p>
          </div>
          <div className="shrink-0 text-right">
            <p className="text-xs text-muted-foreground">Giá từ</p>
            <p className="text-lg font-bold text-primary">{formatPrice(tour.minPrice)}</p>
          </div>
        </div>
      </div>
    </Link>
  );
}

export function TourCardSkeleton() {
  return (
    <div className="flex flex-col overflow-hidden rounded-2xl border bg-card" aria-hidden>
      <div className="aspect-[4/3] animate-pulse bg-muted" />
      <div className="flex flex-col gap-2 p-4">
        <div className="h-3 w-1/2 animate-pulse rounded bg-muted" />
        <div className="h-5 w-full animate-pulse rounded bg-muted" />
        <div className="h-3 w-2/3 animate-pulse rounded bg-muted" />
        <div className="mt-4 h-6 w-1/3 self-end animate-pulse rounded bg-muted" />
      </div>
    </div>
  );
}
