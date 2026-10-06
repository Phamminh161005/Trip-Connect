"use client";

import { FilterTabs } from "@/components/common/FilterTabs";
import { ListState } from "@/components/common/ListState";
import { PageHeader } from "@/components/common/PageHeader";
import { PaginationBar } from "@/components/common/PaginationBar";
import { SearchBox } from "@/components/common/SearchBox";
import { Card, CardContent } from "@/components/ui/card";
import { usePagedQuery } from "@/hooks/usePagedQuery";
import { useSearchParamsState } from "@/hooks/useSearchParamsState";
import { listManagedBookings, type BookingScope } from "@/lib/api/bookings";
import type { BookingStatus } from "@/types/booking";
import { BookingTable } from "./BookingTable";

type Tab = BookingStatus | "ALL" | "MANUAL_REFUND";

/** Danh sách đơn cho Agent (đơn tour của mình) và Admin (mọi đơn, thêm tab "Cần hoàn tiền thủ công"). */
export function ManagedBookingList({ scope }: { scope: BookingScope }) {
  const isAdmin = scope === "admin";
  const tabs: { value: Tab; label: string }[] = [
    { value: "ALL", label: "Tất cả" },
    { value: "PAID", label: "Đã thanh toán" },
    { value: "PENDING_PAYMENT", label: "Chờ thanh toán" },
    { value: "DEPOSIT_PAID", label: "Đã đặt cọc" },
    { value: "COMPLETED", label: "Hoàn thành" },
    { value: "CANCELLED", label: "Đã hủy" },
    ...(isAdmin ? [{ value: "MANUAL_REFUND" as Tab, label: "Cần hoàn thủ công" }] : []),
  ];
  const params = useSearchParamsState();
  const tab = tabs.find((t) => t.value === params.get("status"))?.value ?? "ALL";
  const keyword = params.get("q") ?? "";
  const setPage = (next: number) => params.set({ page: next || undefined }, { resetPage: false });
  const basePath = `/${scope}/bookings`;

  const query = usePagedQuery({
    queryKey: [scope, "bookings"],
    page: params.page,
    paramsFor: (page) => ({
      status: tab === "ALL" || tab === "MANUAL_REFUND" ? undefined : tab,
      refundStatus: tab === "MANUAL_REFUND" ? ("MANUAL_REQUIRED" as const) : undefined,
      q: keyword,
      page,
      size: 20,
    }),
    queryFn: (p) => listManagedBookings(scope, p),
    onPageChange: setPage,
  });
  const rows = query.data?.content ?? [];

  return (
    <>
      <PageHeader
        title="Đơn đặt tour"
        description={isAdmin ? "Mọi đơn đặt tour trên TripConnect, hoàn tiền và hủy vì bất khả kháng." : "Đơn đặt các tour do bạn tổ chức."}
      />
      <Card className="rounded-2xl">
        <CardContent className="flex flex-col gap-4">
          <FilterTabs
            label="Lọc theo trạng thái"
            value={tab}
            options={tabs}
            onChange={(value) => params.set({ status: value === "ALL" ? undefined : value })}
          />
          <SearchBox value={keyword} onSearch={(q) => params.set({ q })} placeholder="Tìm theo mã đơn, tên khách, email, SĐT, tên tour" />
          <ListState isPending={query.isPending} error={query.error} isEmpty={rows.length === 0} emptyText="Không có đơn nào." />
          {rows.length > 0 && (
            <>
              <BookingTable bookings={rows} basePath={basePath} showCustomer dimmed={query.isPlaceholderData} />
              <PaginationBar
                page={params.page}
                size={20}
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
