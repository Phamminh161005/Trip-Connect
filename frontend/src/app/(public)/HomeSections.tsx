"use client";

import type { ReactNode } from "react";
import Image from "next/image";
import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { ArrowRight, MapPin } from "lucide-react";
import { TourCard, TourCardSkeleton } from "@/components/search/TourCard";
import { locationLabel } from "@/components/auth/register/LocationMultiSelect";
import { getTourCategories } from "@/lib/api/catalog";
import { getPopularDestinations, searchTours } from "@/lib/api/search";
import type { TourSearchParams } from "@/types/search";

function Section({ title, description, href, children }: { title: string; description?: string; href?: string; children: ReactNode }) {
  return (
    <section className="flex flex-col gap-5">
      <div className="flex flex-wrap items-end justify-between gap-2">
        <div>
          <h2 className="text-2xl font-bold tracking-tight">{title}</h2>
          {description && <p className="text-muted-foreground">{description}</p>}
        </div>
        {href && (
          <Link href={href} className="flex items-center gap-1 text-sm font-semibold text-primary hover:underline">
            Xem tất cả <ArrowRight className="size-4" />
          </Link>
        )}
      </div>
      {children}
    </section>
  );
}

/** Lưới thẻ tour theo một điều kiện tìm kiếm (không ghi lịch sử tìm kiếm). Không có tour thì ẩn cả khối. */
function TourGridSection({
  title,
  description,
  href,
  params,
}: {
  title: string;
  description: string;
  href: string;
  params: TourSearchParams;
}) {
  const query = useQuery({ queryKey: ["home-tours", params], queryFn: () => searchTours(params) });
  if (!query.isPending && (query.isError || !query.data?.content.length)) return null;

  return (
    <Section title={title} description={description} href={href}>
      <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-4">
        {query.isPending
          ? Array.from({ length: 4 }, (_, i) => <TourCardSkeleton key={i} />)
          : query.data.content.map((tour) => <TourCard key={tour.id} tour={tour} />)}
      </div>
    </Section>
  );
}

function PopularDestinations() {
  const query = useQuery({ queryKey: ["popular-destinations"], queryFn: () => getPopularDestinations(8) });
  if (!query.isPending && (query.isError || !query.data?.length)) return null;

  return (
    <Section title="Điểm đến nổi bật" description="Những nơi có nhiều tour đang mở bán nhất.">
      <div className="grid grid-cols-2 gap-4 md:grid-cols-4">
        {query.isPending
          ? Array.from({ length: 4 }, (_, i) => <div key={i} className="aspect-[4/5] animate-pulse rounded-2xl bg-muted" />)
          : query.data.map((d) => (
              <Link
                key={d.location.id}
                href={`/tours?destinationId=${d.location.id}`}
                className="group relative aspect-[4/5] overflow-hidden rounded-2xl bg-muted"
              >
                {d.coverImageUrl && (
                  <Image
                    src={d.coverImageUrl}
                    alt={locationLabel(d.location)}
                    fill
                    sizes="(min-width: 768px) 25vw, 50vw"
                    className="object-cover transition-transform duration-300 group-hover:scale-105"
                  />
                )}
                <div className="absolute inset-0 bg-linear-to-t from-black/70 via-black/10 to-transparent" />
                <div className="absolute right-4 bottom-4 left-4 text-white">
                  <p className="flex items-center gap-1 text-lg font-semibold">
                    <MapPin className="size-4" /> {locationLabel(d.location)}
                  </p>
                  <p className="text-sm text-white/85">{d.tourCount} tour</p>
                </div>
              </Link>
            ))}
      </div>
    </Section>
  );
}

function Categories() {
  const query = useQuery({ queryKey: ["tour-categories"], queryFn: getTourCategories, staleTime: Infinity });
  if (!query.data?.length) return null;

  return (
    <Section title="Khám phá theo loại hình">
      <div className="flex flex-wrap gap-2">
        {query.data.map((c) => (
          <Link
            key={c.id}
            href={`/tours?categoryIds=${c.id}`}
            className="rounded-full border bg-card px-4 py-2 text-sm font-medium transition-colors hover:border-primary hover:text-primary"
          >
            {c.name}
          </Link>
        ))}
      </div>
    </Section>
  );
}

/** Các khối nội dung trang chủ (dưới ô tìm kiếm). */
export function HomeSections() {
  return (
    <div className="mx-auto flex max-w-7xl flex-col gap-14 px-4 py-14 sm:px-6 lg:px-10">
      <PopularDestinations />
      <TourGridSection
        title="Tour mới mở bán"
        description="Vừa được các đối tác lữ hành đăng lên TripConnect."
        href="/tours?sort=NEWEST"
        params={{ sort: "NEWEST", size: 4 }}
      />
      <TourGridSection
        title="Tour quốc tế"
        description="Khám phá thế giới cùng các đơn vị lữ hành uy tín."
        href="/tours?international=true"
        params={{ international: true, size: 4 }}
      />
      <Categories />
    </div>
  );
}
