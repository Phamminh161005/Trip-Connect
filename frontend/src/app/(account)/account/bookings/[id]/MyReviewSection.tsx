"use client";

import { useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { EyeOff, MessageSquareReply, PenLine, Star } from "lucide-react";
import { ReviewImages } from "@/components/review/ReviewImages";
import { Stars } from "@/components/review/Stars";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { getMyReview } from "@/lib/api/reviews";
import { formatDate, formatDateTime } from "@/lib/format";
import type { BookingDetail } from "@/types/booking";
import { MY_BOOKINGS_KEY } from "../MyBookingList";
import { ReviewDialog } from "./ReviewDialog";

/** Đánh giá của khách cho đơn đã hoàn thành: mời viết, hoặc hiện đánh giá đã gửi (+ trả lời của đơn vị tổ chức). */
export function MyReviewSection({ booking }: { booking: BookingDetail }) {
  const searchParams = useSearchParams();
  const router = useRouter();
  const queryClient = useQueryClient();
  const key = ["my-review", booking.id];
  const query = useQuery({ queryKey: key, queryFn: () => getMyReview(booking.id), enabled: booking.status === "COMPLETED" });
  // Mở sẵn khung viết khi đến từ thông báo / email mời đánh giá (?review=1)
  const [open, setOpen] = useState(searchParams.get("review") === "1" && booking.canReview);

  if (booking.status !== "COMPLETED") return null;
  if (query.isPending) return <Skeleton className="h-32 rounded-2xl" />;
  const review = query.data ?? null;
  if (!review && !booking.canReview) return null;

  const close = () => {
    setOpen(false);
    if (searchParams.get("review")) router.replace(`/account/bookings/${booking.id}`, { scroll: false });
  };
  const saved = () => {
    close();
    void queryClient.invalidateQueries({ queryKey: key });
    void queryClient.invalidateQueries({ queryKey: MY_BOOKINGS_KEY });
    void queryClient.invalidateQueries({ queryKey: ["tour-reviews", booking.tourId] });
  };

  return (
    <>
      {review ? (
        <Card className="rounded-2xl">
          <CardHeader className="flex flex-wrap items-start justify-between gap-3">
            <CardTitle className="text-lg">Đánh giá của bạn</CardTitle>
            {review.canEdit && (
              <Button variant="outline" className="rounded-xl" onClick={() => setOpen(true)}>
                <PenLine /> Sửa đánh giá
              </Button>
            )}
          </CardHeader>
          <CardContent className="flex flex-col gap-3">
            {review.hidden && (
              <p className="flex items-start gap-2 rounded-xl bg-amber-50 p-3 text-sm text-amber-900 dark:bg-amber-950/40 dark:text-amber-200">
                <EyeOff className="mt-0.5 size-4 shrink-0" /> Đánh giá đã bị quản trị viên ẩn khỏi trang tour. Lý do: {review.hiddenReason}
              </p>
            )}
            <div className="flex items-center gap-2">
              <Stars value={review.rating} />
              <span className="text-xs text-muted-foreground">gửi {formatDateTime(review.createdAt)}</span>
            </div>
            <p className="text-sm leading-relaxed whitespace-pre-line">{review.comment}</p>
            <ReviewImages images={review.images} />
            {review.reply && (
              <div className="rounded-xl bg-muted/60 p-3 text-sm">
                <p className="mb-1 flex items-center gap-1.5 font-semibold">
                  <MessageSquareReply className="size-4 text-primary" /> Phản hồi từ {booking.providerName}
                </p>
                <p className="whitespace-pre-line text-muted-foreground">{review.reply}</p>
              </div>
            )}
            {review.canEdit && (
              <p className="text-xs text-muted-foreground">Bạn sửa được đánh giá tới {formatDate(review.editableUntil)}.</p>
            )}
          </CardContent>
        </Card>
      ) : (
        <Card className="rounded-2xl border-primary/30 bg-primary/5">
          <CardContent className="flex flex-wrap items-center justify-between gap-4">
            <div className="flex items-center gap-3">
              <Star className="size-8 fill-amber-400 text-amber-400" />
              <div>
                <p className="font-semibold">Chuyến đi của bạn thế nào?</p>
                <p className="text-sm text-muted-foreground">Nhận xét của bạn giúp các khách khác chọn được tour phù hợp.</p>
              </div>
            </div>
            <Button className="rounded-xl" onClick={() => setOpen(true)}>
              Viết đánh giá
            </Button>
          </CardContent>
        </Card>
      )}
      {open && <ReviewDialog bookingId={booking.id} tourTitle={booking.tourTitle} review={review} onClose={close} onSaved={saved} />}
    </>
  );
}
