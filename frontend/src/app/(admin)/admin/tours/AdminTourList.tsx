"use client";

import Link from "next/link";
import { Plus } from "lucide-react";
import { FilterTabs } from "@/components/common/FilterTabs";
import { ListState } from "@/components/common/ListState";
import { PageHeader } from "@/components/common/PageHeader";
import { PaginationBar } from "@/components/common/PaginationBar";
import { SearchBox } from "@/components/common/SearchBox";
import { TourTable } from "@/components/tour/TourTable";
import { tourKeys } from "@/components/tour/manage/tourQueries";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { usePagedQuery } from "@/hooks/usePagedQuery";
import { useSearchParamsState } from "@/hooks/useSearchParamsState";
import { listToursForAdmin } from "@/lib/api/tours";
import type { TourStatus } from "@/types/tour";

type Tab = TourStatus | "ALL";

const TABS: { value: Tab; label: string }[] = [
  { value: "PENDING_APPROVAL", label: "Chờ duyệt" },
  { value: "PUBLISHED", label: "Đang bán" },
  { value: "NEEDS_REVISION", label: "Cần chỉnh sửa" },
  { value: "HIDDEN", label: "Tạm ẩn" },
  { value: "SUSPENDED", label: "Bị đình chỉ" },
  { value: "DRAFT", label: "Nháp" },
  { value: "ALL", label: "Tất cả" },
];

const PROVIDERS = [
  { value: "ALL", label: "Mọi đơn vị" },
  { value: "AGENT", label: "Tour của đối tác" },
  { value: "PLATFORM", label: "Tour của TripConnect" },
] as const;

const PAGE_SIZE = 20;

export function AdminTourList() {
  const params = useSearchParamsState();
  const tab = TABS.find((t) => t.value === params.get("status"))?.value ?? "PENDING_APPROVAL";
  const provider = PROVIDERS.find((p) => p.value === params.get("provider"))?.value ?? "ALL";
  const keyword = params.get("q") ?? "";
  const page = params.page;

  const pending = tab === "PENDING_APPROVAL";
  const filters = {
    status: tab === "ALL" ? undefined : tab,
    provider: provider === "ALL" ? undefined : provider,
    q: keyword,
    size: PAGE_SIZE,
    // Hàng chờ duyệt: tour gửi sớm nhất lên đầu
    sort: pending ? "submittedAt,asc" : undefined,
  };
  const setPage = (next: number) => params.set({ page: next || undefined }, { resetPage: false });
  const query = usePagedQuery({
    queryKey: [...tourKeys.scope("admin"), "tours"],
    page,
    paramsFor: (p) => ({ ...filters, page: p }),
    queryFn: listToursForAdmin,
    onPageChange: setPage,
  });
  const rows = query.data?.content ?? [];

  return (
    <>
      <PageHeader
        title="Tour"
        description="Duyệt tour của đối tác và quản lý tour do TripConnect tự tổ chức."
        actions={
          <Button asChild className="rounded-xl">
            <Link href="/admin/tours/new">
              <Plus /> Tạo tour TripConnect
            </Link>
          </Button>
        }
      />
      <Card className="rounded-2xl">
        <CardContent className="flex flex-col gap-4">
          <FilterTabs
            label="Lọc theo trạng thái"
            value={tab}
            options={TABS}
            onChange={(value) => params.set({ status: value === "PENDING_APPROVAL" ? undefined : value })}
          />
          <div className="flex flex-col gap-3 md:flex-row md:items-center">
            <SearchBox value={keyword} onSearch={(q) => params.set({ q })} placeholder="Tìm theo tên tour hoặc tên công ty" />
            <Select value={provider} onValueChange={(value) => params.set({ provider: value === "ALL" ? undefined : value })}>
              <SelectTrigger className="h-10! w-52 rounded-xl" aria-label="Lọc theo đơn vị tổ chức">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {PROVIDERS.map((p) => (
                  <SelectItem key={p.value} value={p.value}>
                    {p.label}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <ListState
            isPending={query.isPending}
            error={query.error}
            isEmpty={rows.length === 0}
            emptyText={pending && !keyword ? "Không có tour nào đang chờ duyệt." : "Không tìm thấy tour phù hợp."}
          />

          {rows.length > 0 && (
            <>
              <TourTable
                tours={rows}
                basePath="/admin/tours"
                showProvider
                dimmed={query.isPlaceholderData}
                timeColumn={pending ? "submittedAt" : "updatedAt"}
              />
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
