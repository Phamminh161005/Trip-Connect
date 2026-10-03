"use client";

import { useQuery } from "@tanstack/react-query";
import { getAdminSummary } from "@/lib/api/admin";

/** Mọi dữ liệu trang quản trị dùng khóa bắt đầu bằng "admin" -> sau khi duyệt/từ chối chỉ cần làm mới 1 lần. */
export const ADMIN_KEY = ["admin"] as const;

export function useAdminSummary() {
  return useQuery({ queryKey: [...ADMIN_KEY, "summary"], queryFn: getAdminSummary, refetchInterval: 60_000 });
}
