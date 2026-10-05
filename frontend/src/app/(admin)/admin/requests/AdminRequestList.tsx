"use client";

import { FilterTabs } from "@/components/common/FilterTabs";
import { ListState } from "@/components/common/ListState";
import { PageHeader } from "@/components/common/PageHeader";
import { PaginationBar } from "@/components/common/PaginationBar";
import { SearchBox } from "@/components/common/SearchBox";
import { RequestTable } from "@/components/customRequest/RequestTable";
import { Card, CardContent } from "@/components/ui/card";
import { usePagedQuery } from "@/hooks/usePagedQuery";
import { useSearchParamsState } from "@/hooks/useSearchParamsState";
import { listAdminCustomRequests } from "@/lib/api/customRequests";
import type { CustomRequestStatus } from "@/types/customRequest";

export const ADMIN_REQUESTS_KEY = ["admin", "custom-requests"] as const;
const PAGE_SIZE = 20;
type Tab = CustomRequestStatus | "ALL";
const TABS: { value: Tab; label: string }[] = [
  { value: "NEW", label: "Chờ giao" },
  { value: "WAITING_AGENT", label: "Chờ Agent nhận" },
  { value: "IN_PROGRESS", label: "Đang xử lý" },
  { value: "AGREED", label: "Đã chốt" },
  { value: "CLOSED", label: "Đã đóng" },
  { value: "CANCELLED", label: "Đã hủy" },
  { value: "ALL", label: "Tất cả" },
];

/** Mặc định mở tab "Chờ giao" — việc Admin cần làm; cũ nhất lên trước. */
export function AdminRequestList() {
  const params = useSearchParamsState();
  const tab = TABS.find((t) => t.value === params.get("status"))?.value ?? "NEW";
  const keyword = params.get("q") ?? "";
  const setPage = (next: number) => params.set({ page: next || undefined }, { resetPage: false });
  const query = usePagedQuery({
    queryKey: ADMIN_REQUESTS_KEY,
    page: params.page,
    paramsFor: (page) => ({ status: tab === "ALL" ? undefined : tab, q: keyword, page, size: PAGE_SIZE }),
    queryFn: listAdminCustomRequests,
    onPageChange: setPage,
  });
  const rows = query.data?.content ?? [];

  return (
    <>
      <PageHeader
        title="Yêu cầu tour riêng"
        description="Giao yêu cầu thiết kế tour cho đơn vị phù hợp. Hệ thống gợi ý theo khu vực phục vụ và điểm đánh giá."
      />
      <Card className="rounded-2xl">
        <CardContent className="flex flex-col gap-4">
          <FilterTabs label="Lọc theo trạng thái" value={tab} options={TABS} onChange={(value) => params.set({ status: value === "NEW" ? undefined : value })} />
          <SearchBox value={keyword} onSearch={(q) => params.set({ q })} placeholder="Tìm theo mã yêu cầu, tên hoặc email khách" />
          <ListState isPending={query.isPending} error={query.error} isEmpty={rows.length === 0} emptyText="Không có yêu cầu nào." />
          {rows.length > 0 && (
            <>
              <RequestTable rows={rows} basePath="/admin/requests" viewer="admin" dimmed={query.isPlaceholderData} />
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
