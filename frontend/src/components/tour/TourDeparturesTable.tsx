"use client";

import Link from "next/link";
import { CalendarDays } from "lucide-react";
import { Button } from "@/components/ui/button";
import { formatDay, formatDayWithWeekday, formatPrice } from "@/lib/tour/labels";
import { cn } from "@/lib/utils";
import type { TourDeparture } from "@/types/tour";
import { DepartureStatusBadge } from "./TourStatusBadge";

/**
 * Bảng lịch khởi hành. "public": khách xem (chỉ lịch còn hiệu lực, có nút đặt tour nếu truyền bookTourId);
 * "review": Admin xem khi duyệt (mọi lịch kèm trạng thái, không có nút đặt).
 */
export function TourDeparturesTable({
  departures,
  mode = "public",
  bookTourId,
}: {
  departures: TourDeparture[];
  mode?: "public" | "review";
  /** Có = nút "Đặt tour" dẫn tới trang đặt; không có (bản xem trước) = nút bị khóa. */
  bookTourId?: number;
}) {
  const visible = mode === "review" ? departures : departures.filter((d) => d.status !== "CANCELLED" && !d.departed);

  return (
    <section id="lich-khoi-hanh" className="flex scroll-mt-24 flex-col gap-4">
      <h2 className="flex items-center gap-2 text-lg font-semibold">
        <CalendarDays className="size-5 text-primary" /> Lịch khởi hành & giá
      </h2>
      {visible.length === 0 ? (
        <p className="rounded-2xl border border-dashed p-6 text-center text-sm text-muted-foreground">
          Hiện chưa có lịch khởi hành. Vui lòng quay lại sau.
        </p>
      ) : (
        <ul className="flex flex-col gap-2">
          {visible.map((d) => (
            <li
              key={d.id}
              className="grid gap-3 rounded-2xl border bg-card p-4 sm:grid-cols-[1fr_1fr_auto] sm:items-center sm:gap-4"
            >
              <div>
                <p className="font-semibold">{formatDayWithWeekday(d.startDate)}</p>
                <p className="text-sm text-muted-foreground">Về ngày {formatDay(d.endDate)}</p>
              </div>
              <div className="text-sm">
                <p>
                  <span className="text-muted-foreground">Người lớn </span>
                  <strong className="text-lg text-primary">{formatPrice(d.adultPrice)}</strong>
                </p>
                <p className="text-muted-foreground">Trẻ em (2–11 tuổi) {formatPrice(d.childPrice)}</p>
              </div>
              <div className="flex flex-col gap-1.5 sm:items-end">
                {d.bookable && mode === "public" ? (
                  <span className="text-sm text-muted-foreground">Còn {d.seatsAvailable} chỗ</span>
                ) : (
                  <DepartureStatusBadge departure={d} />
                )}
                {mode === "public" && d.bookable && (
                  <BookButton className="w-full sm:w-40" href={bookTourId ? `/tours/${bookTourId}/book?departure=${d.id}` : undefined} />
                )}
              </div>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}

/** Hộp giá ở cột phải: "Giá từ ... / người". */
export function TourPriceBox({ departures }: { departures: TourDeparture[] }) {
  const bookable = departures.filter((d) => d.bookable);
  const minPrice = bookable.length ? Math.min(...bookable.map((d) => d.adultPrice)) : null;

  return (
    <div className="flex flex-col gap-3 rounded-2xl border bg-card p-5 shadow-sm">
      {minPrice !== null ? (
        <>
          <p className="text-sm text-muted-foreground">Giá từ</p>
          <p className="text-2xl font-bold text-primary">
            {formatPrice(minPrice)} <span className="text-sm font-normal text-muted-foreground">/ người</span>
          </p>
          <p className="text-sm text-muted-foreground">{bookable.length} lịch khởi hành đang mở bán</p>
        </>
      ) : (
        <p className="text-sm text-muted-foreground">Hiện chưa có lịch khởi hành đang mở bán.</p>
      )}
      <Button asChild size="lg" variant={minPrice !== null ? "default" : "outline"} className="h-12 rounded-xl text-base font-semibold">
        <a href="#lich-khoi-hanh">{minPrice !== null ? "Chọn lịch & đặt tour" : "Xem lịch khởi hành"}</a>
      </Button>
    </div>
  );
}

/** Nút đặt tour. Không có link (bản xem trước của Agent / Admin) thì khóa. */
function BookButton({ className, href }: { className?: string; href?: string }) {
  if (!href) {
    return (
      <div className={cn("flex flex-col items-stretch gap-1", className)}>
        <Button size="lg" className="h-11 rounded-xl text-base font-semibold" disabled>
          Đặt tour
        </Button>
        <span className="text-center text-xs text-muted-foreground">Bản xem trước</span>
      </div>
    );
  }
  return (
    <Button asChild size="lg" className={cn("h-11 rounded-xl text-base font-semibold", className)}>
      <Link href={href}>Đặt tour</Link>
    </Button>
  );
}

/** Thanh cố định cuối màn hình trên điện thoại: giá thấp nhất + nút đặt tour (màn hình lớn đã có hộp giá ở cột phải). */
export function TourMobileBookingBar({ departures }: { departures: TourDeparture[] }) {
  const bookable = departures.filter((d) => d.bookable);
  if (bookable.length === 0) return null;
  const minPrice = Math.min(...bookable.map((d) => d.adultPrice));

  return (
    <div className="fixed inset-x-0 bottom-0 z-40 border-t bg-background/95 px-4 py-3 shadow-[0_-4px_16px_rgba(0,0,0,0.06)] backdrop-blur lg:hidden">
      <div className="mx-auto flex max-w-6xl items-center justify-between gap-3">
        <div className="min-w-0">
          <p className="text-xs text-muted-foreground">Giá từ</p>
          <p className="truncate text-lg font-bold text-primary">
            {formatPrice(minPrice)} <span className="text-xs font-normal text-muted-foreground">/ người</span>
          </p>
        </div>
        <Button asChild size="lg" className="h-12 shrink-0 rounded-xl px-6 text-base font-semibold">
          <a href="#lich-khoi-hanh">Đặt tour</a>
        </Button>
      </div>
    </div>
  );
}
