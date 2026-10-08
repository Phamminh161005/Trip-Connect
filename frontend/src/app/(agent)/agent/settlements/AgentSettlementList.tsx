"use client";

import { FilterTabs } from "@/components/common/FilterTabs";
import { ListState } from "@/components/common/ListState";
import { PageHeader } from "@/components/common/PageHeader";
import { PaginationBar } from "@/components/common/PaginationBar";
import { SettlementTable } from "@/components/settlement/SettlementTable";
import { Card, CardContent } from "@/components/ui/card";
import { usePagedQuery } from "@/hooks/usePagedQuery";
import { useSearchParamsState } from "@/hooks/useSearchParamsState";
import { listSettlements } from "@/lib/api/settlements";
import { SETTLEMENT_RULES } from "@/lib/settlement/labels";
import type { SettlementStatus } from "@/types/settlement";

export const AGENT_SETTLEMENTS_KEY = ["agent", "settlements"] as const;
const PAGE_SIZE = 20;
type Tab = SettlementStatus | "ALL";
const TABS: { value: Tab; label: string }[] = [
  { value: "ALL", label: "Tất cả" },
  { value: "PENDING_CONFIRM", label: "Chờ xác nhận" },
  { value: "DISPUTED", label: "Đang khiếu nại" },
  { value: "AWAITING_PAYMENT", label: "Chờ thanh toán" },
  { value: "PAID", label: "Đã nhận tiền" },
];

export function AgentSettlementList() {
  const params = useSearchParamsState();
  const tab = TABS.find((t) => t.value === params.get("status"))?.value ?? "ALL";
  const setPage = (next: number) => params.set({ page: next || undefined }, { resetPage: false });
  const query = usePagedQuery({
    queryKey: AGENT_SETTLEMENTS_KEY,
    page: params.page,
    paramsFor: (page) => ({ status: tab === "ALL" ? undefined : tab, page, size: PAGE_SIZE }),
    queryFn: (p) => listSettlements("agent", p),
    onPageChange: setPage,
  });
  const rows = query.data?.content ?? [];

  return (
    <>
      <PageHeader
        title="Đối soát"
        description={`Ngày 1 hằng tháng TripConnect lập bảng đối soát các đơn của tháng trước. Kiểm tra và xác nhận (hoặc khiếu nại) trong ${SETTLEMENT_RULES.confirmDays} ngày, sau đó TripConnect chuyển khoản vào tài khoản trong hồ sơ của bạn.`}
      />
      <Card className="rounded-2xl">
        <CardContent className="flex flex-col gap-4">
          <FilterTabs label="Lọc theo trạng thái" value={tab} options={TABS} onChange={(value) => params.set({ status: value === "ALL" ? undefined : value })} />
          <ListState isPending={query.isPending} error={query.error} isEmpty={rows.length === 0} emptyText="Chưa có bảng đối soát nào." />
          {rows.length > 0 && (
            <>
              <SettlementTable rows={rows} basePath="/agent/settlements" admin={false} dimmed={query.isPlaceholderData} />
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
