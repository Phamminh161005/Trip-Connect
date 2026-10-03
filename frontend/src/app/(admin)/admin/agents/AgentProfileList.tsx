"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { FilterTabs } from "@/components/common/FilterTabs";
import { ListState } from "@/components/common/ListState";
import { PageHeader } from "@/components/common/PageHeader";
import { PaginationBar } from "@/components/common/PaginationBar";
import { SearchBox } from "@/components/common/SearchBox";
import { AgentStatusBadge } from "@/components/admin/StatusBadges";
import { Card, CardContent } from "@/components/ui/card";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { usePagedQuery } from "@/hooks/usePagedQuery";
import { useSearchParamsState } from "@/hooks/useSearchParamsState";
import { listAgentProfiles } from "@/lib/api/admin";
import { formatDateTime } from "@/lib/format";
import type { AgentStatus } from "@/types/auth";
import { ADMIN_KEY } from "../adminQueries";

type StatusTab = AgentStatus | "ALL";

const TABS: { value: StatusTab; label: string }[] = [
  { value: "PENDING_APPROVAL", label: "Chờ duyệt" },
  { value: "NEEDS_REVISION", label: "Cần bổ sung" },
  { value: "APPROVED", label: "Đã duyệt" },
  { value: "DRAFT", label: "Nháp" },
  { value: "ALL", label: "Tất cả" },
];

const PAGE_SIZE = 20;

export function AgentProfileList() {
  const router = useRouter();
  const params = useSearchParamsState();
  const tab = (TABS.find((t) => t.value === params.get("status"))?.value ?? "PENDING_APPROVAL") as StatusTab;
  const keyword = params.get("q") ?? "";
  const page = params.page;

  const filters = {
    status: tab === "ALL" ? undefined : tab,
    q: keyword,
    size: PAGE_SIZE,
    // Hàng chờ duyệt: hồ sơ nộp sớm nhất lên đầu (xử lý theo thứ tự đến)
    sort: tab === "PENDING_APPROVAL" ? "submittedAt,asc" : undefined,
  };
  const setPage = (next: number) => params.set({ page: next || undefined }, { resetPage: false });
  const query = usePagedQuery({
    queryKey: [...ADMIN_KEY, "agent-profiles"],
    page,
    paramsFor: (p) => ({ ...filters, page: p }),
    queryFn: listAgentProfiles,
    onPageChange: setPage,
  });
  const rows = query.data?.content ?? [];

  return (
    <>
      <PageHeader title="Hồ sơ đối tác" description="Xét duyệt hồ sơ đăng ký của các công ty lữ hành." />
      <Card className="rounded-2xl">
        <CardContent className="flex flex-col gap-4">
          <div className="flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
            <FilterTabs
              label="Lọc theo trạng thái"
              value={tab}
              options={TABS}
              onChange={(value) => params.set({ status: value === "PENDING_APPROVAL" ? undefined : value })}
            />
            <SearchBox
              value={keyword}
              onSearch={(q) => params.set({ q })}
              placeholder="Tìm theo tên công ty, email, mã số thuế"
            />
          </div>

          <ListState
            isPending={query.isPending}
            error={query.error}
            isEmpty={rows.length === 0}
            emptyText={keyword ? "Không tìm thấy hồ sơ phù hợp." : "Không có hồ sơ nào ở trạng thái này."}
          />

          {rows.length > 0 && (
            <>
              <Table className={query.isPlaceholderData ? "opacity-60" : undefined}>
                <TableHeader>
                  <TableRow>
                    <TableHead>Công ty</TableHead>
                    <TableHead className="hidden md:table-cell">Mã số thuế</TableHead>
                    <TableHead>Trạng thái</TableHead>
                    <TableHead className="hidden sm:table-cell">{tab === "DRAFT" ? "Tạo lúc" : "Nộp lúc"}</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {rows.map((row) => {
                    const href = `/admin/agents/${row.id}`;
                    return (
                      <TableRow key={row.id} className="cursor-pointer" onClick={() => router.push(href)}>
                        <TableCell className="max-w-72">
                          <Link href={href} className="block truncate font-medium hover:underline" onClick={(e) => e.stopPropagation()}>
                            {row.companyName ?? <span className="text-muted-foreground italic">Chưa đặt tên công ty</span>}
                          </Link>
                          <p className="truncate text-xs text-muted-foreground">
                            {row.fullName} · {row.email}
                          </p>
                        </TableCell>
                        <TableCell className="hidden md:table-cell">{row.taxCode ?? "—"}</TableCell>
                        <TableCell>
                          <AgentStatusBadge status={row.status} />
                        </TableCell>
                        <TableCell className="hidden text-muted-foreground sm:table-cell">
                          {formatDateTime(tab === "DRAFT" ? row.createdAt : (row.submittedAt ?? row.createdAt))}
                        </TableCell>
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
