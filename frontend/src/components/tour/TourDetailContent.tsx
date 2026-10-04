"use client";

import type { ReactNode } from "react";
import {
  BedDouble,
  Building2,
  CalendarDays,
  Check,
  Clock,
  FileDown,
  Globe,
  MapPin,
  Plane,
  Sparkles,
  Star,
  UtensilsCrossed,
  X,
} from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { locationLabel } from "@/components/auth/register/LocationMultiSelect";
import { openTemporaryDocument } from "@/lib/agent/documents";
import { formatFileSize } from "@/lib/format";
import { ACCOMMODATION_LABELS, TRANSPORT_LABELS, formatDuration, mealsText } from "@/lib/tour/labels";
import type { TemporaryUrlResponse } from "@/types/agent";
import type { TourDetail } from "@/types/tour";
import { TourGallery } from "./TourGallery";

interface TourDetailContentProps {
  tour: TourDetail;
  /** Xin link tải file PDF (API công khai hoặc API quản lý tùy nơi dùng). */
  getItineraryFileUrl: () => Promise<TemporaryUrlResponse>;
  /** Khối lịch khởi hành (trang khách: bảng đặt tour; trang quản lý: không truyền). */
  departures?: ReactNode;
  /** Cột phải (trang khách: hộp giá + đơn vị tổ chức). */
  aside?: ReactNode;
}

/** Nội dung chi tiết tour — dùng chung cho trang khách, trang Admin duyệt và bản xem trước của Agent. */
export function TourDetailContent({ tour, getItineraryFileUrl, departures, aside }: TourDetailContentProps) {
  return (
    <div className="flex flex-col gap-8">
      <div className="flex flex-col gap-3">
        <div className="flex flex-wrap gap-1.5">
          {tour.international && (
            <Badge className="border-0 bg-violet-100 text-violet-900">
              <Globe /> Tour quốc tế
            </Badge>
          )}
          {tour.categories.map((c) => (
            <Badge key={c.id} variant="secondary">
              {c.name}
            </Badge>
          ))}
        </div>
        <h1 className="text-2xl font-bold tracking-tight text-balance sm:text-3xl">{tour.title}</h1>
        <div className="flex flex-wrap items-center gap-x-5 gap-y-1 text-sm text-muted-foreground">
          {tour.ratingCount > 0 && tour.rating !== null && (
            <a href="#reviews" className="flex items-center gap-1 font-medium text-foreground hover:underline">
              <Star className="size-4 fill-amber-400 text-amber-400" /> {tour.rating.toFixed(1)} ({tour.ratingCount} đánh giá)
            </a>
          )}
          <span className="flex items-center gap-1.5">
            <MapPin className="size-4" /> {tour.destinations.map(locationLabel).join(" · ")}
          </span>
        </div>
      </div>

      <TourGallery images={tour.images} title={tour.title} />

      <div className="grid gap-8 lg:grid-cols-[1fr_340px]">
        <div className="flex min-w-0 flex-col gap-8">
          <div className="grid grid-cols-2 gap-3 rounded-2xl border bg-card p-4 sm:grid-cols-4">
            <Fact icon={<CalendarDays />} label="Thời lượng" value={formatDuration(tour.durationDays, tour.durationNights)} />
            <Fact icon={<MapPin />} label="Khởi hành từ" value={locationLabel(tour.departureLocation)} />
            <Fact icon={<Plane />} label="Phương tiện" value={tour.transportModes.map((m) => TRANSPORT_LABELS[m]).join(", ")} />
            <Fact icon={<BedDouble />} label="Lưu trú" value={ACCOMMODATION_LABELS[tour.accommodationType]} />
          </div>

          <Section title="Điểm nổi bật" icon={<Sparkles />}>
            <ul className="grid gap-2 sm:grid-cols-2">
              {tour.highlights.map((h, index) => (
                <li key={index} className="flex items-start gap-2">
                  <Check className="mt-0.5 size-4 shrink-0 text-primary" />
                  <span>{h}</span>
                </li>
              ))}
            </ul>
          </Section>

          <Section
            title="Lịch trình"
            icon={<CalendarDays />}
            action={
              tour.itineraryFile && (
                <Button variant="outline" size="sm" className="rounded-lg" onClick={() => openTemporaryDocument(getItineraryFileUrl)}>
                  <FileDown /> Tải chương trình (PDF, {formatFileSize(tour.itineraryFile.sizeBytes)})
                </Button>
              )
            }
          >
            <ol className="flex flex-col gap-3">
              {tour.itinerary.map((day) => (
                <li key={day.dayNumber}>
                  <details className="group rounded-2xl border bg-card open:shadow-sm" open={day.dayNumber === 1}>
                    <summary className="flex cursor-pointer list-none items-center gap-3 p-4">
                      <span className="flex size-9 shrink-0 items-center justify-center rounded-full bg-primary/10 text-sm font-semibold text-primary">
                        {day.dayNumber}
                      </span>
                      <span className="font-medium">{day.title}</span>
                    </summary>
                    <div className="flex flex-col gap-3 px-4 pb-4 pl-16">
                      <p className="text-sm leading-relaxed whitespace-pre-line">{day.description}</p>
                      <div className="flex flex-wrap gap-x-5 gap-y-1 text-sm text-muted-foreground">
                        {mealsText(day) && (
                          <span className="flex items-center gap-1.5">
                            <UtensilsCrossed className="size-4" /> {mealsText(day)}
                          </span>
                        )}
                        {day.accommodation && (
                          <span className="flex items-center gap-1.5">
                            <BedDouble className="size-4" /> Nghỉ đêm: {day.accommodation}
                          </span>
                        )}
                      </div>
                    </div>
                  </details>
                </li>
              ))}
            </ol>
          </Section>

          {departures}

          <div className="grid gap-4 md:grid-cols-2">
            <ServiceList title="Giá tour bao gồm" items={tour.includedServices} included />
            {tour.excludedServices.length > 0 && <ServiceList title="Không bao gồm" items={tour.excludedServices} />}
          </div>

          <Section title="Điểm đón & lưu ý" icon={<Clock />}>
            <div className="flex flex-col gap-3 text-sm">
              <p>
                <span className="text-muted-foreground">Tập trung lúc </span>
                <strong>{tour.meetingTime}</strong>
                <span className="text-muted-foreground"> tại </span>
                <strong>{tour.meetingPoint}</strong>
              </p>
              {tour.notes && <p className="leading-relaxed whitespace-pre-line">{tour.notes}</p>}
            </div>
          </Section>
        </div>

        <aside className="flex flex-col gap-4 lg:sticky lg:top-24 lg:self-start">
          {aside}
          <ProviderCard tour={tour} />
        </aside>
      </div>
    </div>
  );
}

function Fact({ icon, label, value }: { icon: ReactNode; label: string; value: string }) {
  return (
    <div className="flex items-start gap-2.5">
      <span className="mt-0.5 text-primary [&_svg]:size-5">{icon}</span>
      <div className="min-w-0">
        <p className="text-xs text-muted-foreground">{label}</p>
        <p className="text-sm font-medium">{value}</p>
      </div>
    </div>
  );
}

function Section({ title, icon, action, children }: { title: string; icon: ReactNode; action?: ReactNode; children: ReactNode }) {
  return (
    <section className="flex flex-col gap-4">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <h2 className="flex items-center gap-2 text-lg font-semibold [&_svg]:size-5 [&_svg]:text-primary">
          {icon} {title}
        </h2>
        {action}
      </div>
      {children}
    </section>
  );
}

function ServiceList({ title, items, included }: { title: string; items: string[]; included?: boolean }) {
  return (
    <Card className="rounded-2xl">
      <CardHeader>
        <CardTitle className="text-base">{title}</CardTitle>
      </CardHeader>
      <CardContent>
        <ul className="flex flex-col gap-2 text-sm">
          {items.map((item, index) => (
            <li key={index} className="flex items-start gap-2">
              {included ? (
                <Check className="mt-0.5 size-4 shrink-0 text-emerald-600" />
              ) : (
                <X className="mt-0.5 size-4 shrink-0 text-red-500" />
              )}
              {item}
            </li>
          ))}
        </ul>
      </CardContent>
    </Card>
  );
}

function ProviderCard({ tour }: { tour: TourDetail }) {
  const name = tour.platformTour ? "TripConnect" : (tour.provider?.companyName ?? "Đối tác TripConnect");
  return (
    <Card className="rounded-2xl">
      <CardContent className="flex items-center gap-3">
        <span className="flex size-11 shrink-0 items-center justify-center rounded-xl bg-primary/10 text-primary">
          <Building2 className="size-5" />
        </span>
        <div className="min-w-0">
          <p className="text-xs text-muted-foreground">Đơn vị tổ chức</p>
          <p className="truncate font-semibold">{name}</p>
          {!tour.platformTour && tour.provider && tour.provider.ratingCount > 0 && tour.provider.rating !== null && (
            <p className="flex items-center gap-1 text-xs text-muted-foreground">
              <Star className="size-3 fill-amber-400 text-amber-400" /> {tour.provider.rating.toFixed(1)} ·{" "}
              {tour.provider.ratingCount} đánh giá
            </p>
          )}
        </div>
      </CardContent>
    </Card>
  );
}
