import Image from "next/image";
import Link from "next/link";
import { CalendarDays, Clock, ImageOff, PencilRuler, Users } from "lucide-react";
import { Button } from "@/components/ui/button";
import { customRequestUrl } from "@/lib/customRequest/prefill";
import { formatDay, formatDuration, formatPrice } from "@/lib/tour/labels";
import type { CustomRequestDraft } from "@/types/assistant";
import type { TourCard } from "@/types/search";

/** Thẻ tour thu gọn trong khung trợ lý. */
export function AssistantTourCard({ tour, onNavigate }: { tour: TourCard; onNavigate?: () => void }) {
  return (
    <Link
      href={`/tours/${tour.id}`}
      onClick={onNavigate}
      className="flex gap-3 rounded-xl border bg-card p-2 transition-colors outline-none hover:bg-muted/60 focus-visible:ring-3 focus-visible:ring-ring/50"
    >
      <div className="relative size-16 shrink-0 overflow-hidden rounded-lg bg-muted">
        {tour.coverImageUrl ? (
          <Image src={tour.coverImageUrl} alt={tour.title} fill sizes="64px" className="object-cover" />
        ) : (
          <ImageOff className="absolute inset-0 m-auto size-5 text-muted-foreground" />
        )}
      </div>
      <div className="flex min-w-0 flex-1 flex-col gap-0.5">
        <p className="line-clamp-2 text-sm font-medium leading-snug">{tour.title}</p>
        <p className="flex flex-wrap items-center gap-x-2 text-xs text-muted-foreground">
          <span className="flex items-center gap-1">
            <Clock className="size-3" /> {formatDuration(tour.durationDays, tour.durationNights)}
          </span>
          {tour.nextDepartureDate && (
            <span className="flex items-center gap-1">
              <CalendarDays className="size-3" /> {formatDay(tour.nextDepartureDate)}
            </span>
          )}
        </p>
        {tour.departureCount > 0 && (
          <p className="text-xs">
            Từ <span className="font-semibold text-primary">{formatPrice(tour.minPrice)}</span>
          </p>
        )}
      </div>
    </Link>
  );
}

/** Nút mở form yêu cầu tour riêng với thông tin trợ lý đã điền sẵn. */
export function CustomRequestCta({ draft, onNavigate }: { draft: CustomRequestDraft; onNavigate?: () => void }) {
  const travellers = [
    draft.adults ? `${draft.adults} người lớn` : null,
    draft.children ? `${draft.children} trẻ em` : null,
    draft.infants ? `${draft.infants} em bé` : null,
  ].filter(Boolean);

  return (
    <div className="flex flex-col gap-2 rounded-xl border border-primary/30 bg-primary/5 p-3">
      <p className="flex items-center gap-1.5 text-sm font-semibold">
        <PencilRuler className="size-4 text-primary" /> Thiết kế tour riêng
      </p>
      <ul className="flex flex-col gap-0.5 text-xs text-muted-foreground">
        {draft.destinationNames.length > 0 && <li>Điểm đến: {draft.destinationNames.join(", ")}</li>}
        {draft.durationDays && <li>Thời gian: {draft.durationDays} ngày</li>}
        {draft.earliestStart && (
          <li>
            Khởi hành: {formatDay(draft.earliestStart)}
            {draft.latestStart && draft.latestStart !== draft.earliestStart && ` – ${formatDay(draft.latestStart)}`}
          </li>
        )}
        {travellers.length > 0 && (
          <li className="flex items-center gap-1">
            <Users className="size-3" /> {travellers.join(", ")}
          </li>
        )}
        {draft.budgetMax && <li>Ngân sách tối đa: {formatPrice(draft.budgetMax)}</li>}
      </ul>
      <Button asChild size="sm" className="self-start rounded-lg">
        <Link href={customRequestUrl(draft)} onClick={onNavigate}>
          Gửi yêu cầu tour riêng
        </Link>
      </Button>
    </div>
  );
}
