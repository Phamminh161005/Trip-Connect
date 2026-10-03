"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { FilterTabs } from "@/components/common/FilterTabs";
import { ListState } from "@/components/common/ListState";
import { PageHeader } from "@/components/common/PageHeader";
import { PaginationBar } from "@/components/common/PaginationBar";
import { ChangeRequestStatusBadge } from "@/components/admin/StatusBadges";
import { Card, CardContent } from "@/components/ui/card";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { usePagedQuery } from "@/hooks/usePagedQuery";
import { useSearchParamsState } from "@/hooks/useSearchParamsState";
import { listChangeRequests } from "@/lib/api/admin";
import { formatDateTime } from "@/lib/format";
import type { ChangeRequestStatus } from "@/types/agent";
import { ADMIN_KEY } from "../adminQueries";

const TABS: { value: ChangeRequestStatus; label: string }[] = [
  { value: "PENDING", label: "Chờ duyệt" },
  { value: "APPROVED", label: "Đã duyệt" },
  { value: "REJECTED", label: "Bị từ chối" },
  { value: "CANCELLED", label: "Đã hủy" },
];

const PAGE_SIZE = 20;

export function ChangeRequestList() {
  const router = useRouter();
  const params = useSearchParamsState();
  const status = TABS.find((t) => t.value === params.get("status"))?.value ?? "PENDING";
  const page = params.page;

  const filters = {
    status,
    size: PAGE_SIZE,
    // Chờ duyệt: cũ nhất lên đầu (xử lý theo thứ tự đến); các tab lịch sử: mới nhất lên đầu
    sort: status === "PENDING" ? "createdAt,asc" : "createdAt,desc",
  };
  const setPage = (next: number) => params.set({ page: next || undefined }, { resetPage: false });
  const query = usePagedQuery({
    queryKey: [...ADMIN_KEY, "change-requests"],
    page,
    paramsFor: (p) => ({ ...filters, page: p }),
    queryFn: listChangeRequests,
    onPageChange: setPage,
  });
  const rows = query.data?.content ?? [];

  return (
    <>
      <PageHeader
        title="Yêu cầu cập nhật hồ sơ"
        description="Đối tác đã được duyệt muốn đổi thông tin pháp lý (tên, mã số thuế, địa chỉ, tài khoản, giấy tờ)."
      />
      <Card className="rounded-2xl">
        <CardContent className="flex flex-col gap-4">
          <FilterTabs
            label="Lọc theo trạng thái"
            value={status}
            options={TABS}
            onChange={(value) => params.set({ status: value === "PENDING" ? undefined : value })}
          />

          <ListState
            isPending={query.isPending}
            error={query.error}
            isEmpty={rows.length === 0}
            emptyText="Không có yêu cầu nào ở trạng thái này."
          />

          {rows.length > 0 && (
            <>
              <Table className={query.isPlaceholderData ? "opacity-60" : undefined}>
                <TableHeader>
                  <TableRow>
                    <TableHead>Đối tác</TableHead>
                    <TableHead>Trạng thái</TableHead>
                    <TableHead className="hidden sm:table-cell">Gửi lúc</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {rows.map((row) => {
                    const href = `/admin/change-requests/${row.id}`;
                    return (
                      <TableRow key={row.id} className="cursor-pointer" onClick={() => router.push(href)}>
                        <TableCell className="max-w-80">
                          <Link href={href} className="block truncate font-medium hover:underline" onClick={(e) => e.stopPropagation()}>
                            {row.currentCompanyName ?? `Hồ sơ #${row.agentProfileId}`}
                          </Link>
                          <p className="truncate text-xs text-muted-foreground">{row.agentEmail}</p>
                        </TableCell>
                        <TableCell>
                          <ChangeRequestStatusBadge status={row.status} />
                        </TableCell>
                        <TableCell className="hidden text-muted-foreground sm:table-cell">{formatDateTime(row.createdAt)}</TableCell>
                      </TableRow>
                    );
                  })}
                </TableBody>
              </Table>
              <PaginationBar
                page={page}
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
