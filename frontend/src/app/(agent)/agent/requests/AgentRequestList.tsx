"use client";

import { FilterTabs } from "@/components/common/FilterTabs";
import { ListState } from "@/components/common/ListState";
import { PageHeader } from "@/components/common/PageHeader";
import { PaginationBar } from "@/components/common/PaginationBar";
import { RequestTable } from "@/components/customRequest/RequestTable";
import { Card, CardContent } from "@/components/ui/card";
import { usePagedQuery } from "@/hooks/usePagedQuery";
import { useSearchParamsState } from "@/hooks/useSearchParamsState";
import { listAgentCustomRequests, type AgentRequestTab } from "@/lib/api/customRequests";
import { CUSTOM_REQUEST_RULES } from "@/lib/customRequest/labels";

export const AGENT_REQUESTS_KEY = ["agent", "custom-requests"] as const;
const PAGE_SIZE = 20;
const TABS: { value: AgentRequestTab; label: string }[] = [
  { value: "PENDING", label: "Chờ nhận" },
  { value: "ACCEPTED", label: "Đã nhận" },
  { value: "HISTORY", label: "Đã từ chối / hết hạn" },
];

export function AgentRequestList() {
  const params = useSearchParamsState();
  const tab = TABS.find((t) => t.value === params.get("tab"))?.value ?? "PENDING";
  const setPage = (next: number) => params.set({ page: next || undefined }, { resetPage: false });
  const query = usePagedQuery({
    queryKey: AGENT_REQUESTS_KEY,
    page: params.page,
    paramsFor: (page) => ({ tab, page, size: PAGE_SIZE }),
    queryFn: listAgentCustomRequests,
    onPageChange: setPage,
  });
  const rows = query.data?.content ?? [];

  return (
    <>
      <PageHeader
        title="Yêu cầu tư vấn"
        description={`Yêu cầu thiết kế tour riêng TripConnect giao cho bạn. Nhận hoặc từ chối trong ${CUSTOM_REQUEST_RULES.acceptHours} giờ — quá hạn, yêu cầu sẽ được chuyển cho đơn vị khác.`}
      />
      <Card className="rounded-2xl">
        <CardContent className="flex flex-col gap-4">
          <FilterTabs label="Lọc yêu cầu" value={tab} options={TABS} onChange={(value) => params.set({ tab: value === "PENDING" ? undefined : value })} />
          <ListState
            isPending={query.isPending}
            error={query.error}
            isEmpty={rows.length === 0}
            emptyText={tab === "PENDING" ? "Chưa có yêu cầu nào chờ bạn nhận." : "Không có yêu cầu nào."}
          />
          {rows.length > 0 && (
            <>
              <RequestTable rows={rows} basePath="/agent/requests" viewer="agent" dimmed={query.isPlaceholderData} />
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
