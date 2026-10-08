"use client";

import { useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { FilePlus2 } from "lucide-react";
import { toast } from "sonner";
import { ConfirmDialog } from "@/components/common/ConfirmDialog";
import { FilterTabs } from "@/components/common/FilterTabs";
import { ListState } from "@/components/common/ListState";
import { PageHeader } from "@/components/common/PageHeader";
import { PaginationBar } from "@/components/common/PaginationBar";
import { SettlementTable } from "@/components/settlement/SettlementTable";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { usePagedQuery } from "@/hooks/usePagedQuery";
import { useSearchParamsState } from "@/hooks/useSearchParamsState";
import { generateSettlements, listSettlements } from "@/lib/api/settlements";
import { withErrorToast } from "@/lib/withErrorToast";
import type { SettlementStatus } from "@/types/settlement";
import { ADMIN_KEY } from "../adminQueries";

export const ADMIN_SETTLEMENTS_KEY = ["admin", "settlements"] as const;
const PAGE_SIZE = 20;
type Tab = SettlementStatus | "ALL";
const TABS: { value: Tab; label: string }[] = [
  { value: "AWAITING_PAYMENT", label: "Chờ thanh toán" },
  { value: "DISPUTED", label: "Có khiếu nại" },
  { value: "PENDING_CONFIRM", label: "Chờ Agent xác nhận" },
  { value: "PAID", label: "Đã thanh toán" },
  { value: "ALL", label: "Tất cả" },
];

export function AdminSettlementList() {
  const params = useSearchParamsState();
  const queryClient = useQueryClient();
  const [generating, setGenerating] = useState(false);
  const tab = TABS.find((t) => t.value === params.get("status"))?.value ?? "AWAITING_PAYMENT";
  const setPage = (next: number) => params.set({ page: next || undefined }, { resetPage: false });
  const query = usePagedQuery({
    queryKey: ADMIN_SETTLEMENTS_KEY,
    page: params.page,
    paramsFor: (page) => ({ status: tab === "ALL" ? undefined : tab, page, size: PAGE_SIZE }),
    queryFn: (p) => listSettlements("admin", p),
    onPageChange: setPage,
  });
  const rows = query.data?.content ?? [];

  return (
    <>
      <PageHeader
        title="Đối soát"
        description="Ngày 1 hằng tháng hệ thống tự lập bảng đối soát tháng trước cho từng đơn vị. Đơn vị xác nhận (hoặc khiếu nại), sau đó TripConnect chuyển khoản và ghi mã giao dịch."
        actions={
          <Button className="rounded-xl" onClick={() => setGenerating(true)}>
            <FilePlus2 /> Lập đối soát ngay
          </Button>
        }
      />
      <Card className="rounded-2xl">
        <CardContent className="flex flex-col gap-4">
          <FilterTabs
            label="Lọc theo trạng thái"
            value={tab}
            options={TABS}
            onChange={(value) => params.set({ status: value === "AWAITING_PAYMENT" ? undefined : value })}
          />
          <ListState isPending={query.isPending} error={query.error} isEmpty={rows.length === 0} emptyText="Không có bảng đối soát nào." />
          {rows.length > 0 && (
            <>
              <SettlementTable rows={rows} basePath="/admin/settlements" admin dimmed={query.isPlaceholderData} />
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
      <ConfirmDialog
        open={generating}
        onOpenChange={setGenerating}
        title="Lập đối soát ngay?"
        description="Gom mọi đơn chưa đối soát tính tới thời điểm này (đơn hoàn thành, đơn hủy mà vẫn giữ tiền) thành bảng đối soát cho từng đơn vị. Dùng khi không muốn đợi tới ngày 1."
        confirmLabel="Lập đối soát"
        onConfirm={async () => {
          const { created } = await withErrorToast(generateSettlements);
          toast.success(created > 0 ? `Đã lập ${created} bảng đối soát` : "Không có đơn nào cần đối soát");
          void queryClient.invalidateQueries({ queryKey: ADMIN_SETTLEMENTS_KEY });
          void queryClient.invalidateQueries({ queryKey: [...ADMIN_KEY, "summary"] });
        }}
      />
    </>
  );
}
