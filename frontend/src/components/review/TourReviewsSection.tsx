"use client";

import { useState } from "react";
import { useInfiniteQuery, useQuery } from "@tanstack/react-query";
import { BadgeCheck, MessageSquareReply, Star } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { Spinner } from "@/components/ui/spinner";
import { getTourReviewSummary, listTourReviews } from "@/lib/api/reviews";
import { formatDate } from "@/lib/format";
import { cn } from "@/lib/utils";
import type { PublicReview } from "@/types/review";
import { ReviewImages } from "./ReviewImages";
import { Stars } from "./Stars";

const PAGE_SIZE = 10;

/** "2026-10-15" -> "tháng 10/2026" */
const departureMonth = (date: string) => `tháng ${Number(date.slice(5, 7))}/${date.slice(0, 4)}`;

/** Khung đánh giá trên trang tour: điểm trung bình, biểu đồ số sao (bấm để lọc), danh sách + "Xem thêm". */
export function TourReviewsSection({ tourId, providerName }: { tourId: number; providerName: string }) {
  const [rating, setRating] = useState<number | null>(null);
  const summary = useQuery({ queryKey: ["tour-reviews", tourId, "summary"], queryFn: () => getTourReviewSummary(tourId) });
  const list = useInfiniteQuery({
    queryKey: ["tour-reviews", tourId, "list", { rating }],
    queryFn: ({ pageParam }) => listTourReviews(tourId, { before: pageParam ?? undefined, size: PAGE_SIZE, rating: rating ?? undefined }),
    initialPageParam: null as number | null,
    getNextPageParam: (last) => last.nextCursor,
  });
  const reviews = list.data?.pages.flatMap((p) => p.items) ?? [];
  const s = summary.data;

  return (
    <section id="reviews" className="mt-10 flex scroll-mt-24 flex-col gap-5">
      <h2 className="text-xl font-semibold">Đánh giá của khách</h2>

      {summary.isPending ? (
        <Skeleton className="h-36 rounded-2xl" />
      ) : !s || s.count === 0 ? (
        <p className="rounded-2xl border border-dashed p-8 text-center text-muted-foreground">
          Chưa có đánh giá nào. Đánh giá chỉ đến từ khách đã đi tour.
        </p>
      ) : (
        <div className="grid gap-6 rounded-2xl border p-5 sm:grid-cols-[180px_1fr]">
          <div className="flex flex-col items-center justify-center gap-1 text-center">
            <span className="text-5xl font-bold tabular-nums">{s.average?.toFixed(1)}</span>
            <Stars value={s.average ?? 0} />
            <span className="text-sm text-muted-foreground">{s.count} đánh giá</span>
          </div>
          <div className="flex flex-col gap-1.5">
            {[5, 4, 3, 2, 1].map((stars) => {
              const n = s.distribution[String(stars)] ?? 0;
              const active = rating === stars;
              return (
                <button
                  key={stars}
                  type="button"
                  disabled={n === 0}
                  onClick={() => setRating(active ? null : stars)}
                  aria-pressed={active}
                  className={cn(
                    "flex items-center gap-3 rounded-lg px-2 py-1 text-sm transition-colors disabled:opacity-50 enabled:hover:bg-accent",
                    active && "bg-accent",
                  )}
                >
                  <span className="flex w-10 items-center gap-1 tabular-nums">
                    {stars} <Star className="size-3.5 fill-amber-400 text-amber-400" />
                  </span>
                  <span className="h-2 flex-1 overflow-hidden rounded-full bg-muted">
                    <span className="block h-full rounded-full bg-amber-400" style={{ width: `${(n / s.count) * 100}%` }} />
                  </span>
                  <span className="w-8 text-right text-muted-foreground tabular-nums">{n}</span>
                </button>
              );
            })}
            {rating !== null && (
              <button type="button" onClick={() => setRating(null)} className="self-start px-2 text-sm text-primary hover:underline">
                Bỏ lọc {rating} sao
              </button>
            )}
          </div>
        </div>
      )}

      {s && s.count > 0 && (
        <div className="flex flex-col divide-y rounded-2xl border">
          {list.isPending
            ? Array.from({ length: 2 }, (_, i) => <Skeleton key={i} className="m-4 h-28 rounded-xl" />)
            : reviews.map((review) => <ReviewCard key={review.id} review={review} providerName={providerName} />)}
          {!list.isPending && reviews.length === 0 && (
            <p className="p-6 text-center text-sm text-muted-foreground">Không có đánh giá {rating} sao.</p>
          )}
        </div>
      )}
      {list.hasNextPage && (
        <Button variant="outline" className="self-center rounded-xl" disabled={list.isFetchingNextPage} onClick={() => list.fetchNextPage()}>
          {list.isFetchingNextPage && <Spinner />} Xem thêm đánh giá
        </Button>
      )}
    </section>
  );
}

function ReviewCard({ review, providerName }: { review: PublicReview; providerName: string }) {
  return (
    <article className="flex flex-col gap-3 p-5">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <div className="flex items-center gap-3">
          <span className="flex size-9 items-center justify-center rounded-full bg-primary/10 text-sm font-semibold text-primary">
            {review.reviewerName.split(" ").pop()?.charAt(0)}
          </span>
          <div>
            <p className="text-sm font-semibold">{review.reviewerName}</p>
            <p className="flex items-center gap-1 text-xs text-muted-foreground">
              <BadgeCheck className="size-3.5 text-emerald-600" /> Đã đi tour · khởi hành {departureMonth(review.departureDate)}
            </p>
          </div>
        </div>
        <div className="flex flex-col items-end gap-0.5">
          <Stars value={review.rating} />
          <span className="text-xs text-muted-foreground">
            {formatDate(review.createdAt)}
            {review.edited && " · đã chỉnh sửa"}
          </span>
        </div>
      </div>
      <p className="text-sm leading-relaxed whitespace-pre-line">{review.comment}</p>
      <ReviewImages images={review.images} />
      {review.reply && (
        <div className="rounded-xl bg-muted/60 p-3 text-sm">
          <p className="mb-1 flex items-center gap-1.5 font-semibold">
            <MessageSquareReply className="size-4 text-primary" /> Phản hồi từ {providerName}
          </p>
          <p className="whitespace-pre-line text-muted-foreground">{review.reply}</p>
        </div>
      )}
    </article>
  );
}
