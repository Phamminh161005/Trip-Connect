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
import { usePagedQuery } from "@/hooks/usePagedQuery";
import { useSearchParamsState } from "@/hooks/useSearchParamsState";
import { listMyTours } from "@/lib/api/tours";
import type { TourStatus } from "@/types/tour";

type Tab = TourStatus | "ALL";

const TABS: { value: Tab; label: string }[] = [
  { value: "ALL", label: "Tất cả" },
  { value: "PUBLISHED", label: "Đang bán" },
  { value: "DRAFT", label: "Nháp" },
  { value: "PENDING_APPROVAL", label: "Chờ duyệt" },
  { value: "NEEDS_REVISION", label: "Cần chỉnh sửa" },
  { value: "HIDDEN", label: "Tạm ẩn" },
  { value: "SUSPENDED", label: "Bị đình chỉ" },
];

const PAGE_SIZE = 20;

export function AgentTourList() {
  const params = useSearchParamsState();
  const tab = TABS.find((t) => t.value === params.get("status"))?.value ?? "ALL";
  const keyword = params.get("q") ?? "";
  const page = params.page;

  const filters = { status: tab === "ALL" ? undefined : tab, q: keyword, size: PAGE_SIZE };
  const setPage = (next: number) => params.set({ page: next || undefined }, { resetPage: false });
  const query = usePagedQuery({
    queryKey: [...tourKeys.scope("agent"), "tours"],
    page,
    paramsFor: (p) => ({ ...filters, page: p }),
    queryFn: listMyTours,
    onPageChange: setPage,
  });
  const rows = query.data?.content ?? [];
  const noToursAtAll = !query.isPending && !keyword && tab === "ALL" && rows.length === 0;

  return (
    <>
      <PageHeader
        title="Tour của tôi"
        description="Tạo tour, thêm lịch khởi hành và theo dõi trạng thái duyệt."
        actions={
          <Button asChild className="rounded-xl">
            <Link href="/agent/tours/new">
              <Plus /> Tạo tour
            </Link>
          </Button>
        }
      />
      <Card className="rounded-2xl">
        <CardContent className="flex flex-col gap-4">
          <div className="flex flex-col gap-3 xl:flex-row xl:items-center xl:justify-between">
            <FilterTabs
              label="Lọc theo trạng thái"
              value={tab}
              options={TABS}
              onChange={(value) => params.set({ status: value === "ALL" ? undefined : value })}
            />
            <SearchBox value={keyword} onSearch={(q) => params.set({ q })} placeholder="Tìm theo tên tour" />
          </div>

          {noToursAtAll ? (
            <div className="flex flex-col items-center gap-3 py-10 text-center">
              <p className="text-muted-foreground">Bạn chưa có tour nào.</p>
              <Button asChild className="rounded-xl">
                <Link href="/agent/tours/new">
                  <Plus /> Tạo tour đầu tiên
                </Link>
              </Button>
            </div>
          ) : (
            <ListState
              isPending={query.isPending}
              error={query.error}
              isEmpty={rows.length === 0}
              emptyText={keyword ? "Không tìm thấy tour phù hợp." : "Không có tour nào ở trạng thái này."}
            />
          )}

          {rows.length > 0 && (
            <>
              <TourTable tours={rows} basePath="/agent/tours" dimmed={query.isPlaceholderData} />
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
