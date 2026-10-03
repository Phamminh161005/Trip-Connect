"use client";

import { useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
import type { SelectOption } from "@/components/form/SearchableSelect";
import { getBanks, getLocations, isVietnamProvince } from "@/lib/api/catalog";

/** Danh sách Tỉnh/Thành Việt Nam cho ô chọn (dữ liệu dùng chung, chỉ tải 1 lần). */
export function useProvinceOptions() {
  const query = useQuery({ queryKey: ["locations"], queryFn: getLocations, staleTime: Infinity });
  const options = useMemo<SelectOption<number>[]>(
    () =>
      (query.data ?? [])
        .filter(isVietnamProvince)
        .map((l) => ({ value: l.id, label: l.province ?? l.country }))
        .sort((a, b) => a.label.localeCompare(b.label, "vi")),
    [query.data],
  );
  return { options, isPending: query.isPending };
}

/** Danh sách ngân hàng (chuẩn Napas / VietQR) cho ô chọn. */
export function useBankOptions() {
  const query = useQuery({ queryKey: ["banks"], queryFn: getBanks, staleTime: Infinity });
  const options = useMemo<SelectOption<string>[]>(
    () =>
      (query.data ?? []).map((bank) => ({
        value: bank.bin,
        label: `${bank.shortName} — ${bank.name}`,
        keywords: `${bank.shortName} ${bank.code} ${bank.name}`,
        render: (
          <span className="flex flex-col">
            <span className="font-medium">{bank.shortName}</span>
            <span className="text-xs text-muted-foreground">{bank.name}</span>
          </span>
        ),
      })),
    [query.data],
  );
  return { options, isPending: query.isPending };
}
