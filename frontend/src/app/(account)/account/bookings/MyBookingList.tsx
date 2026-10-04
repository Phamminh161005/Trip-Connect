"use client";

import Link from "next/link";
import { FilterTabs } from "@/components/common/FilterTabs";
import { ListState } from "@/components/common/ListState";
import { PaginationBar } from "@/components/common/PaginationBar";
import { BookingTable } from "@/components/booking/BookingTable";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { usePagedQuery } from "@/hooks/usePagedQuery";
import { useSearchParamsState } from "@/hooks/useSearchParamsState";
import { listMyBookings } from "@/lib/api/bookings";
import type { BookingStatus } from "@/types/booking";

export const MY_BOOKINGS_KEY = ["me", "bookings"] as const;

type Tab = BookingStatus | "ALL";
const TABS: { value: Tab; label: string }[] = [
  { value: "ALL", label: "Tất cả" },
  { value: "PENDING_PAYMENT", label: "Chờ thanh toán" },
  { value: "PAID", label: "Sắp đi" },
  { value: "COMPLETED", label: "Hoàn thành" },
  { value: "CANCELLED", label: "Đã hủy" },
];
const PAGE_SIZE = 10;

export function MyBookingList() {
  const params = useSearchParamsState();
  const tab = TABS.find((t) => t.value === params.get("status"))?.value ?? "ALL";
  const setPage = (next: number) => params.set({ page: next || undefined }, { resetPage: false });

  const query = usePagedQuery({
    queryKey: MY_BOOKINGS_KEY,
    page: params.page,
    paramsFor: (page) => ({ status: tab === "ALL" ? undefined : tab, page, size: PAGE_SIZE }),
    queryFn: listMyBookings,
    onPageChange: setPage,
  });
  const rows = query.data?.content ?? [];

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight">Đơn đặt của tôi</h1>
        <p className="text-muted-foreground">Theo dõi thanh toán, chuyến đi sắp tới và hoàn tiền.</p>
      </div>
      <Card className="rounded-2xl">
        <CardContent className="flex flex-col gap-4">
          <FilterTabs
            label="Lọc theo trạng thái"
            value={tab}
            options={TABS}
            onChange={(value) => params.set({ status: value === "ALL" ? undefined : value })}
          />
          {!query.isPending && rows.length === 0 && tab === "ALL" ? (
            <div className="flex flex-col items-center gap-3 py-10 text-center">
              <p className="text-muted-foreground">Bạn chưa đặt tour nào.</p>
              <Button asChild className="rounded-xl">
                <Link href="/tours">Khám phá tour</Link>
              </Button>
            </div>
          ) : (
            <ListState isPending={query.isPending} error={query.error} isEmpty={rows.length === 0} emptyText="Không có đơn nào ở trạng thái này." />
          )}
          {rows.length > 0 && (
            <>
              <BookingTable bookings={rows} basePath="/account/bookings" dimmed={query.isPlaceholderData} />
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
    </div>
  );
}
