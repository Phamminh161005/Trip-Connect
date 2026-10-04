"use client";

import { useState } from "react";
import { useSearchParams } from "next/navigation";
import { useQuery } from "@tanstack/react-query";
import { SearchX, SlidersHorizontal } from "lucide-react";
import { PaginationBar } from "@/components/common/PaginationBar";
import { SearchFilters, countActiveFilters, type FilterValues } from "@/components/search/SearchFilters";
import { TourCard, TourCardSkeleton } from "@/components/search/TourCard";
import { TourSearchBar } from "@/components/search/TourSearchBar";
import { locationLabel } from "@/components/auth/register/LocationMultiSelect";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { usePagedQuery } from "@/hooks/usePagedQuery";
import { useSearchParamsState } from "@/hooks/useSearchParamsState";
import { getLocations } from "@/lib/api/catalog";
import { errorMessage } from "@/lib/api/errors";
import { searchTours } from "@/lib/api/search";
import type { TourSearchParams, TourSort } from "@/types/search";

const PAGE_SIZE = 12;

const SORTS: { value: TourSort; label: string }[] = [
  { value: "RECOMMENDED", label: "Phù hợp nhất" },
  { value: "PRICE_ASC", label: "Giá thấp → cao" },
  { value: "PRICE_DESC", label: "Giá cao → thấp" },
  { value: "DEPARTURE_SOON", label: "Khởi hành sớm nhất" },
  { value: "NEWEST", label: "Mới nhất" },
  { value: "RATING", label: "Đánh giá cao" },
];

// Các tham số lọc trên đường link (trùng tên tham số API)
const FILTER_KEYS = [
  "departureLocationId",
  "dateFrom",
  "dateTo",
  "priceMin",
  "priceMax",
  "durationMin",
  "durationMax",
  "categoryIds",
  "international",
] as const;

const num = (value: string | undefined) => {
  const n = Number(value);
  return value && Number.isFinite(n) ? n : undefined;
};

/** Trang kết quả tìm tour. Mọi bộ lọc nằm trên đường link -> tải lại trang / gửi link vẫn giữ kết quả. */
export function TourSearchView() {
  const params = useSearchParamsState();
  const searchParams = useSearchParams();
  const [filtersOpen, setFiltersOpen] = useState(false);

  const q = params.get("q");
  const destinationId = num(params.get("destinationId"));
  const sort = (SORTS.find((s) => s.value === params.get("sort"))?.value ?? "RECOMMENDED") as TourSort;
  const filters: FilterValues = {
    departureLocationId: num(params.get("departureLocationId")),
    dateFrom: params.get("dateFrom"),
    dateTo: params.get("dateTo"),
    priceMin: num(params.get("priceMin")),
    priceMax: num(params.get("priceMax")),
    durationMin: num(params.get("durationMin")),
    durationMax: num(params.get("durationMax")),
    categoryIds: (params.get("categoryIds") ?? "").split(",").map(Number).filter((n) => Number.isInteger(n) && n > 0),
    international: params.get("international") === "true" ? true : params.get("international") === "false" ? false : undefined,
  };

  const setPage = (next: number) => params.set({ page: next || undefined }, { resetPage: false });
  const query = usePagedQuery({
    queryKey: ["tour-search"],
    page: params.page,
    paramsFor: (page): TourSearchParams => ({
      ...filters,
      q,
      destinationId,
      sort,
      page,
      size: PAGE_SIZE,
      // Chỉ ghi lịch sử ở trang đầu (lật trang / tải sẵn trang sau không phải một lần tìm mới)
      track: page === 0,
    }),
    queryFn: searchTours,
    onPageChange: setPage,
  });

  const locationsQuery = useQuery({ queryKey: ["locations"], queryFn: getLocations, staleTime: Infinity });
  const destination = locationsQuery.data?.find((l) => l.id === destinationId);

  const resetFilters = () => params.set(Object.fromEntries(FILTER_KEYS.map((k) => [k, undefined])));
  const activeCount = countActiveFilters(filters);
  const tours = query.data?.content ?? [];
  const heading = destination ? `Tour ${locationLabel(destination)}` : q ? `Kết quả cho “${q}”` : "Tất cả tour";

  const filterPanel = <SearchFilters values={filters} onChange={(updates) => params.set(updates)} onReset={resetFilters} />;

  return (
    <div className="mx-auto w-full max-w-7xl px-4 py-6 sm:px-6 lg:px-10">
      {/* key: ô tìm kiếm hiện lại đúng điều kiện mỗi khi đường link đổi (vd bấm "tìm kiếm gần đây") */}
      <TourSearchBar
        key={`${q ?? ""}|${destinationId ?? ""}|${filters.dateFrom ?? ""}`}
        variant="compact"
        initial={{ q, destinationId, dateFrom: filters.dateFrom }}
      />

      <div className="mt-6 grid gap-8 lg:grid-cols-[260px_1fr]">
        <aside className="hidden lg:block">
          <div className="sticky top-24 max-h-[calc(100svh-7rem)] overflow-y-auto pr-1">{filterPanel}</div>
        </aside>

        <section className="flex min-w-0 flex-col gap-4" aria-busy={query.isFetching}>
          <div className="flex flex-wrap items-end justify-between gap-3">
            <div>
              <h1 className="text-2xl font-bold tracking-tight">{heading}</h1>
              <p className="text-sm text-muted-foreground">
                {query.isPending ? "Đang tìm..." : `${query.data?.totalElements ?? 0} tour đang mở bán`}
              </p>
            </div>
            <div className="flex gap-2">
              <Button variant="outline" className="h-10 rounded-xl lg:hidden" onClick={() => setFiltersOpen(true)}>
                <SlidersHorizontal /> Bộ lọc{activeCount > 0 && ` (${activeCount})`}
              </Button>
              <Select value={sort} onValueChange={(value) => params.set({ sort: value === "RECOMMENDED" ? undefined : value })}>
                <SelectTrigger className="h-10! w-48 rounded-xl" aria-label="Sắp xếp">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent align="end">
                  {SORTS.map((s) => (
                    <SelectItem key={s.value} value={s.value}>
                      {s.label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          </div>

          {query.isPending ? (
            <div className="grid gap-5 sm:grid-cols-2 xl:grid-cols-3">
              {Array.from({ length: 6 }, (_, i) => (
                <TourCardSkeleton key={i} />
              ))}
            </div>
          ) : query.isError ? (
            <p className="py-16 text-center text-destructive">{errorMessage(query.error)}</p>
          ) : tours.length === 0 ? (
            <div className="flex flex-col items-center gap-3 rounded-2xl border border-dashed py-16 text-center">
              <SearchX className="size-10 text-muted-foreground" />
              <div>
                <p className="font-semibold">Không tìm thấy tour phù hợp</p>
                <p className="text-sm text-muted-foreground">Thử bỏ bớt bộ lọc, đổi ngày đi hoặc tìm điểm đến khác.</p>
              </div>
              {(activeCount > 0 || searchParams.toString()) && (
                <Button variant="outline" className="rounded-xl" onClick={() => (activeCount > 0 ? resetFilters() : params.set({ q: undefined, destinationId: undefined }))}>
                  {activeCount > 0 ? "Xóa bộ lọc" : "Xem tất cả tour"}
                </Button>
              )}
            </div>
          ) : (
            <>
              <div className={`grid gap-5 sm:grid-cols-2 xl:grid-cols-3 ${query.isPlaceholderData ? "opacity-60" : ""}`}>
                {tours.map((tour, index) => (
                  <TourCard key={tour.id} tour={tour} priority={index < 3} />
                ))}
              </div>
              <PaginationBar
                page={params.page}
                size={PAGE_SIZE}
                totalElements={query.data?.totalElements ?? 0}
                totalPages={query.data?.totalPages ?? 0}
                onPageChange={(next) => {
                  setPage(next);
                  window.scrollTo({ top: 0, behavior: "smooth" });
                }}
              />
            </>
          )}
        </section>
      </div>

      <Dialog open={filtersOpen} onOpenChange={setFiltersOpen}>
        <DialogContent className="max-h-[90svh] overflow-y-auto rounded-2xl">
          <DialogHeader>
            <DialogTitle className="sr-only">Bộ lọc</DialogTitle>
          </DialogHeader>
          {filterPanel}
          <DialogFooter>
            <Button className="h-11 w-full rounded-xl" onClick={() => setFiltersOpen(false)}>
              Xem {query.data?.totalElements ?? 0} tour
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
