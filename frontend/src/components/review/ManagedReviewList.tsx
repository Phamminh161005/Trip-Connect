"use client";

import { useState } from "react";
import Link from "next/link";
import { useQueryClient } from "@tanstack/react-query";
import { Eye, EyeOff, MessageSquareReply, PenLine } from "lucide-react";
import { toast } from "sonner";
import { ConfirmDialog } from "@/components/common/ConfirmDialog";
import { FilterTabs } from "@/components/common/FilterTabs";
import { ListState } from "@/components/common/ListState";
import { PageHeader } from "@/components/common/PageHeader";
import { PaginationBar } from "@/components/common/PaginationBar";
import { ReasonDialog } from "@/components/common/ReasonDialog";
import { SearchBox } from "@/components/common/SearchBox";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Spinner } from "@/components/ui/spinner";
import { Textarea } from "@/components/ui/textarea";
import { usePagedQuery } from "@/hooks/usePagedQuery";
import { useSearchParamsState } from "@/hooks/useSearchParamsState";
import { errorMessage } from "@/lib/api/errors";
import { hideReview, listManagedReviews, replyToReview, unhideReview, type ReviewScope } from "@/lib/api/reviews";
import { formatDate, formatDateTime } from "@/lib/format";
import { formatDay } from "@/lib/tour/labels";
import type { ManagedReview } from "@/types/review";
import { ReviewImages } from "./ReviewImages";
import { Stars } from "./Stars";

const PAGE_SIZE = 20;
type Tab = "ALL" | "UNREPLIED" | "REPLIED" | "HIDDEN";

/** Đánh giá cho Agent (tour của mình: trả lời) và Admin (mọi đánh giá: ẩn / hiện, trả lời tour của TripConnect). */
export function ManagedReviewList({ scope }: { scope: ReviewScope }) {
  const isAdmin = scope === "admin";
  const tabs: { value: Tab; label: string }[] = [
    { value: "ALL", label: "Tất cả" },
    { value: "UNREPLIED", label: "Chưa trả lời" },
    { value: "REPLIED", label: "Đã trả lời" },
    ...(isAdmin ? [{ value: "HIDDEN" as Tab, label: "Đã ẩn" }] : []),
  ];
  const params = useSearchParamsState();
  const tab = tabs.find((t) => t.value === params.get("tab"))?.value ?? "ALL";
  const rating = Number(params.get("rating")) || undefined;
  const keyword = params.get("q") ?? "";
  const setPage = (next: number) => params.set({ page: next || undefined }, { resetPage: false });
  const queryClient = useQueryClient();

  const query = usePagedQuery({
    queryKey: [scope, "reviews"],
    page: params.page,
    paramsFor: (page) => ({
      replied: tab === "UNREPLIED" ? false : tab === "REPLIED" ? true : undefined,
      hidden: tab === "HIDDEN" ? true : undefined,
      rating,
      q: isAdmin ? keyword : undefined,
      page,
      size: PAGE_SIZE,
    }),
    queryFn: (p) => listManagedReviews(scope, p),
    onPageChange: setPage,
  });
  const rows = query.data?.content ?? [];
  const refresh = () => queryClient.invalidateQueries({ queryKey: [scope, "reviews"] });

  return (
    <>
      <PageHeader
        title="Đánh giá"
        description={
          isAdmin
            ? "Mọi đánh giá của khách. Ẩn đánh giá vi phạm (xúc phạm, quảng cáo, sai sự thật) — đánh giá bị ẩn không tính điểm."
            : "Đánh giá của khách cho các tour bạn tổ chức. Trả lời công khai giúp khách sau yên tâm hơn."
        }
      />
      <Card className="rounded-2xl">
        <CardContent className="flex flex-col gap-4">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <FilterTabs label="Lọc đánh giá" value={tab} options={tabs} onChange={(value) => params.set({ tab: value === "ALL" ? undefined : value })} />
            <Select value={rating ? String(rating) : "all"} onValueChange={(v) => params.set({ rating: v === "all" ? undefined : v })}>
              <SelectTrigger className="w-36 rounded-xl" aria-label="Lọc theo số sao">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">Mọi số sao</SelectItem>
                {[5, 4, 3, 2, 1].map((n) => (
                  <SelectItem key={n} value={String(n)}>
                    {n} sao
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
          {isAdmin && <SearchBox value={keyword} onSearch={(q) => params.set({ q })} placeholder="Tìm theo tên tour, mã đơn, tên khách" />}
          <ListState isPending={query.isPending} error={query.error} isEmpty={rows.length === 0} emptyText="Không có đánh giá nào." />
          {rows.length > 0 && (
            <>
              <div className={`flex flex-col divide-y rounded-2xl border ${query.isPlaceholderData ? "opacity-60" : ""}`}>
                {rows.map((review) => (
                  <ReviewRow key={review.id} review={review} scope={scope} onChanged={refresh} />
                ))}
              </div>
              <PaginationBar
                page={params.page}
                size={PAGE_SIZE}
                totalElements={query.data?.totalElements ?? 0}
                totalPages={query.data?.totalPages ?? 0}
                onPageChange={setPage}
              />
            </>
          )}
        </CardContent>
      </Card>
    </>
  );
}

function ReviewRow({ review, scope, onChanged }: { review: ManagedReview; scope: ReviewScope; onChanged: () => void }) {
  const isAdmin = scope === "admin";
  const canReply = !review.hidden && (!isAdmin || review.platformTour);
  const [replying, setReplying] = useState(false);
  const [hiding, setHiding] = useState(false);
  const [unhiding, setUnhiding] = useState(false);

  return (
    <article className="flex flex-col gap-3 p-4">
      <div className="flex flex-wrap items-start justify-between gap-2">
        <div className="min-w-0">
          <Link href={`/${scope}/tours/${review.tourId}`} className="line-clamp-1 font-semibold hover:underline">
            {review.tourTitle}
          </Link>
          <p className="text-xs text-muted-foreground">
            {review.customerName}
            {isAdmin && ` · ${review.customerEmail}`} · đơn{" "}
            <Link href={`/${scope}/bookings/${review.bookingId}`} className="font-mono hover:underline">
              {review.bookingCode}
            </Link>{" "}
            · khởi hành {formatDay(review.departureDate)}
          </p>
        </div>
        <div className="flex flex-col items-end gap-1">
          <Stars value={review.rating} />
          <span className="text-xs text-muted-foreground">{formatDateTime(review.createdAt)}</span>
        </div>
      </div>

      {review.hidden && (
        <div className="flex items-start gap-2 rounded-xl bg-amber-50 p-3 text-sm text-amber-900 dark:bg-amber-950/40 dark:text-amber-200">
          <EyeOff className="mt-0.5 size-4 shrink-0" />
          <span>
            Đã ẩn {review.hiddenAt && `ngày ${formatDate(review.hiddenAt)}`} — lý do: {review.hiddenReason}
          </span>
        </div>
      )}
      <p className="text-sm leading-relaxed whitespace-pre-line">{review.comment}</p>
      <ReviewImages images={review.images} />

      {review.reply ? (
        <div className="rounded-xl bg-muted/60 p-3 text-sm">
          <p className="mb-1 flex items-center gap-1.5 font-semibold">
            <MessageSquareReply className="size-4 text-primary" /> Đã trả lời {review.repliedAt && `· ${formatDateTime(review.repliedAt)}`}
          </p>
          <p className="whitespace-pre-line text-muted-foreground">{review.reply}</p>
        </div>
      ) : (
        !review.hidden && (isAdmin && !review.platformTour ? null : <Badge variant="outline" className="w-fit">Chưa trả lời</Badge>)
      )}

      <div className="flex flex-wrap gap-2">
        {canReply && (
          <Button variant="outline" size="sm" className="rounded-lg" onClick={() => setReplying(true)}>
            {review.reply ? <PenLine /> : <MessageSquareReply />} {review.reply ? "Sửa trả lời" : "Trả lời"}
          </Button>
        )}
        {isAdmin &&
          (review.hidden ? (
            <Button variant="outline" size="sm" className="rounded-lg" onClick={() => setUnhiding(true)}>
              <Eye /> Hiện lại
            </Button>
          ) : (
            <Button variant="outline" size="sm" className="rounded-lg text-destructive hover:text-destructive" onClick={() => setHiding(true)}>
              <EyeOff /> Ẩn đánh giá
            </Button>
          ))}
      </div>

      {replying && (
        <ReplyDialog
          review={review}
          scope={scope}
          onClose={() => setReplying(false)}
          onDone={() => {
            setReplying(false);
            onChanged();
          }}
        />
      )}
      <ReasonDialog
        open={hiding}
        onOpenChange={setHiding}
        title="Ẩn đánh giá này?"
        description="Đánh giá sẽ không hiện trên trang tour và không tính vào điểm trung bình. Có thể hiện lại sau."
        label="Lý do ẩn"
        placeholder="Ví dụ: nội dung quảng cáo, ngôn từ xúc phạm"
        confirmLabel="Ẩn đánh giá"
        hint="Người viết sẽ nhận thông báo kèm lý do này."
        onConfirm={async (reason) => {
          await hideReview(review.id, reason);
          toast.success("Đã ẩn đánh giá");
          onChanged();
        }}
      />
      <ConfirmDialog
        open={unhiding}
        onOpenChange={setUnhiding}
        title="Hiện lại đánh giá?"
        description="Đánh giá sẽ hiện lại trên trang tour và được tính vào điểm trung bình."
        confirmLabel="Hiện lại"
        onConfirm={async () => {
          await unhideReview(review.id);
          toast.success("Đã hiện lại đánh giá");
          onChanged();
        }}
      />
    </article>
  );
}

function ReplyDialog({
  review,
  scope,
  onClose,
  onDone,
}: {
  review: ManagedReview;
  scope: ReviewScope;
  onClose: () => void;
  onDone: () => void;
}) {
  const [text, setText] = useState(review.reply ?? "");
  const [saving, setSaving] = useState(false);

  const save = async () => {
    if (!text.trim()) return;
    setSaving(true);
    try {
      await replyToReview(scope, review.id, text.trim());
      toast.success(review.reply ? "Đã cập nhật trả lời" : "Đã gửi trả lời");
      onDone();
    } catch (error) {
      toast.error(errorMessage(error));
      setSaving(false);
    }
  };

  return (
    <Dialog open onOpenChange={(open) => !open && !saving && onClose()}>
      <DialogContent className="rounded-2xl sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>{review.reply ? "Sửa trả lời" : "Trả lời đánh giá"}</DialogTitle>
          <DialogDescription>Trả lời hiện công khai dưới đánh giá trên trang tour.</DialogDescription>
        </DialogHeader>
        <div className="rounded-xl bg-muted/60 p-3 text-sm">
          <Stars value={review.rating} />
          <p className="mt-1 line-clamp-4 text-muted-foreground">{review.comment}</p>
        </div>
        <Textarea
          value={text}
          onChange={(e) => setText(e.target.value)}
          rows={5}
          maxLength={2000}
          placeholder="Cảm ơn góp ý của anh/chị..."
          aria-label="Nội dung trả lời"
          className="rounded-xl text-base"
        />
        <DialogFooter>
          <Button variant="outline" className="rounded-xl" disabled={saving} onClick={onClose}>
            Hủy
          </Button>
          <Button className="rounded-xl" disabled={saving || !text.trim()} onClick={save}>
            {saving && <Spinner />} {review.reply ? "Lưu" : "Gửi trả lời"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
