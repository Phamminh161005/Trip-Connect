"use client";

import Link from "next/link";
import { Plus, Sparkles } from "lucide-react";
import { ListState } from "@/components/common/ListState";
import { PaginationBar } from "@/components/common/PaginationBar";
import { RequestTable } from "@/components/customRequest/RequestTable";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { usePagedQuery } from "@/hooks/usePagedQuery";
import { useSearchParamsState } from "@/hooks/useSearchParamsState";
import { listMyCustomRequests } from "@/lib/api/customRequests";

export const MY_REQUESTS_KEY = ["me", "custom-requests"] as const;
const PAGE_SIZE = 10;

export function MyRequestList() {
  const params = useSearchParamsState();
  const setPage = (next: number) => params.set({ page: next || undefined }, { resetPage: false });
  const query = usePagedQuery({
    queryKey: MY_REQUESTS_KEY,
    page: params.page,
    paramsFor: (page) => ({ page, size: PAGE_SIZE }),
    queryFn: listMyCustomRequests,
    onPageChange: setPage,
  });
  const rows = query.data?.content ?? [];

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">Yêu cầu tour riêng</h1>
          <p className="text-muted-foreground">Theo dõi yêu cầu thiết kế tour bạn đã gửi.</p>
        </div>
        <Button asChild className="rounded-xl">
          <Link href="/custom-tour">
            <Plus /> Gửi yêu cầu mới
          </Link>
        </Button>
      </div>
      <Card className="rounded-2xl">
        <CardContent className="flex flex-col gap-4">
          {!query.isPending && rows.length === 0 ? (
            <div className="flex flex-col items-center gap-3 py-10 text-center">
              <Sparkles className="size-8 text-primary" />
              <p className="text-muted-foreground">Bạn chưa gửi yêu cầu nào. Mô tả chuyến đi mơ ước, đối tác sẽ thiết kế riêng cho bạn.</p>
              <Button asChild className="rounded-xl">
                <Link href="/custom-tour">Thiết kế tour riêng</Link>
              </Button>
            </div>
          ) : (
            <ListState isPending={query.isPending} error={query.error} isEmpty={false} emptyText="" />
          )}
          {rows.length > 0 && (
            <>
              <RequestTable rows={rows} basePath="/account/requests" viewer="customer" dimmed={query.isPlaceholderData} />
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
