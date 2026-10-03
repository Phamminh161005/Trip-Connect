"use client";

import { useCallback } from "react";
import { usePathname, useRouter, useSearchParams } from "next/navigation";

/**
 * Đọc / ghi bộ lọc trên thanh địa chỉ (?status=...&page=2).
 * Tải lại trang hoặc gửi link cho người khác vẫn giữ nguyên kết quả lọc.
 */
export function useSearchParamsState() {
  const router = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();

  const get = useCallback((key: string) => searchParams.get(key) ?? undefined, [searchParams]);

  /** Cập nhật nhiều tham số; giá trị rỗng = xóa khỏi URL. Đổi bộ lọc thì mặc định quay về trang đầu. */
  const set = useCallback(
    (updates: Record<string, string | number | undefined | null>, options: { resetPage?: boolean } = { resetPage: true }) => {
      const next = new URLSearchParams(searchParams.toString());
      Object.entries(updates).forEach(([key, value]) => {
        if (value === undefined || value === null || value === "") next.delete(key);
        else next.set(key, String(value));
      });
      if (options.resetPage && !("page" in updates)) next.delete("page");
      const text = next.toString();
      router.replace(text ? `${pathname}?${text}` : pathname, { scroll: false });
    },
    [router, pathname, searchParams],
  );

  const page = Math.max(Number(searchParams.get("page") ?? 0) || 0, 0);

  return { get, set, page };
}
