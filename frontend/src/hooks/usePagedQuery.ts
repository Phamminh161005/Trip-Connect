"use client";

import { useEffect } from "react";
import { hashKey, keepPreviousData, useQuery, useQueryClient, type QueryKey } from "@tanstack/react-query";
import type { PageResponse } from "@/types/common";

/** Trang kế tiếp tải sẵn được coi là còn mới trong 30 giây (bấm "Sau" hiện ngay, không tải lại). */
const PREFETCH_STALE_MS = 30_000;

/**
 * Truy vấn một danh sách có phân trang, dùng chung cho mọi bảng / lưới kết quả:
 *  - Giữ dữ liệu trang cũ trong lúc tải trang mới (không nhấp nháy về trạng thái "đang tải").
 *  - Tải sẵn trang kế tiếp ở nền -> bấm "Sau" thấy ngay.
 *  - Trang đang xem vượt quá số trang (vd vừa duyệt hết hồ sơ ở trang cuối) -> tự lùi về trang cuối còn dữ liệu.
 *
 * Khóa cache = [...queryKey, paramsFor(page)] — giữ dạng "tiền tố + tham số" để làm mới theo tiền tố như trước.
 */
export function usePagedQuery<P, T>({
  queryKey,
  page,
  paramsFor,
  queryFn,
  onPageChange,
}: {
  queryKey: QueryKey;
  /** Trang đang xem, bắt đầu từ 0. */
  page: number;
  /** Tham số gọi API cho một trang bất kỳ (để tải sẵn trang sau với đúng tham số). */
  paramsFor: (page: number) => P;
  queryFn: (params: P) => Promise<PageResponse<T>>;
  onPageChange: (page: number) => void;
}) {
  const queryClient = useQueryClient();
  const params = paramsFor(page);
  const query = useQuery({
    queryKey: [...queryKey, params],
    queryFn: () => queryFn(params),
    placeholderData: keepPreviousData,
  });

  const totalPages = query.data?.totalPages ?? 0;
  const settled = query.data !== undefined && !query.isPlaceholderData;
  const nextParams = settled && page + 1 < totalPages ? paramsFor(page + 1) : null;
  const nextKey = nextParams === null ? null : [...queryKey, nextParams];
  const nextHash = nextKey === null ? null : hashKey(nextKey);

  // Tải sẵn trang sau (chạy lại chỉ khi trang sau thật sự đổi — so bằng mã băm của khóa)
  useEffect(() => {
    if (nextKey === null || nextParams === null) return;
    void queryClient.prefetchQuery({ queryKey: nextKey, queryFn: () => queryFn(nextParams), staleTime: PREFETCH_STALE_MS });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [nextHash, queryClient]);

  // Trang vượt quá số trang -> về trang cuối (hoặc trang đầu nếu không còn dòng nào)
  useEffect(() => {
    if (settled && page > 0 && page >= totalPages) onPageChange(Math.max(totalPages - 1, 0));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [settled, page, totalPages]);

  return query;
}
