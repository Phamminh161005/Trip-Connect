"use client";

import type { ReactNode } from "react";
import { useQuery } from "@tanstack/react-query";
import { SearchableSelect } from "@/components/form/SearchableSelect";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { useProvinceOptions } from "@/components/agent/useCatalogOptions";
import { getTourCategories } from "@/lib/api/catalog";
import { normalizeText } from "@/lib/text";
import { localDateString } from "@/lib/tour/labels";
import { cn } from "@/lib/utils";

/** Bộ lọc đang chọn — trùng tên tham số trên đường link và tham số API. */
export interface FilterValues {
  departureLocationId?: number;
  dateFrom?: string;
  dateTo?: string;
  priceMin?: number;
  priceMax?: number;
  durationMin?: number;
  durationMax?: number;
  categoryIds: number[];
  international?: boolean;
}

type Range = { label: string; min?: number; max?: number };

export const PRICE_RANGES: Range[] = [
  { label: "Dưới 3 triệu", max: 3_000_000 },
  { label: "3 – 7 triệu", min: 3_000_000, max: 7_000_000 },
  { label: "7 – 15 triệu", min: 7_000_000, max: 15_000_000 },
  { label: "15 – 30 triệu", min: 15_000_000, max: 30_000_000 },
  { label: "Trên 30 triệu", min: 30_000_000 },
];

export const DURATION_RANGES: Range[] = [
  { label: "1 ngày", min: 1, max: 1 },
  { label: "2 – 3 ngày", min: 2, max: 3 },
  { label: "4 – 7 ngày", min: 4, max: 7 },
  { label: "Trên 7 ngày", min: 8 },
];

/** Số bộ lọc đang bật (hiện trên nút "Bộ lọc" ở điện thoại). */
export function countActiveFilters(v: FilterValues): number {
  return [
    v.departureLocationId,
    v.dateFrom || v.dateTo,
    v.priceMin !== undefined || v.priceMax !== undefined,
    v.durationMin !== undefined || v.durationMax !== undefined,
    v.categoryIds.length > 0,
    v.international !== undefined,
  ].filter(Boolean).length;
}

/**
 * Bộ lọc kết quả tìm tour. Mỗi thay đổi áp dụng ngay (gọi onChange với các tham số cần đổi;
 * undefined = bỏ lọc).
 */
export function SearchFilters({
  values,
  onChange,
  onReset,
}: {
  values: FilterValues;
  onChange: (updates: Partial<Record<keyof FilterValues, string | number | undefined>>) => void;
  onReset: () => void;
}) {
  const provinces = useProvinceOptions();
  const categoriesQuery = useQuery({ queryKey: ["tour-categories"], queryFn: getTourCategories, staleTime: Infinity });
  const provinceOptions = provinces.options.map((o) => ({ ...o, keywords: `${o.label} ${normalizeText(o.label)}` }));

  const rangeSelected = (range: Range, min?: number, max?: number) => range.min === min && range.max === max;
  const toggleCategory = (id: number) => {
    const next = values.categoryIds.includes(id) ? values.categoryIds.filter((c) => c !== id) : [...values.categoryIds, id];
    onChange({ categoryIds: next.length ? next.join(",") : undefined });
  };

  return (
    <div className="flex flex-col gap-6">
      <div className="flex items-center justify-between">
        <p className="font-semibold">Bộ lọc</p>
        {countActiveFilters(values) > 0 && (
          <Button variant="link" size="sm" className="h-auto p-0" onClick={onReset}>
            Xóa bộ lọc
          </Button>
        )}
      </div>

      <Group title="Loại tour">
        <Pills
          options={[
            { label: "Tất cả", selected: values.international === undefined, onClick: () => onChange({ international: undefined }) },
            { label: "Trong nước", selected: values.international === false, onClick: () => onChange({ international: "false" }) },
            { label: "Quốc tế", selected: values.international === true, onClick: () => onChange({ international: "true" }) },
          ]}
        />
      </Group>

      <Group title="Ngày khởi hành">
        <div className="grid grid-cols-2 gap-2">
          <label className="flex flex-col gap-1 text-xs text-muted-foreground">
            Từ ngày
            <Input
              type="date"
              value={values.dateFrom ?? ""}
              min={localDateString(1)}
              onChange={(e) => onChange({ dateFrom: e.target.value || undefined })}
              className="h-10 rounded-xl text-sm text-foreground"
            />
          </label>
          <label className="flex flex-col gap-1 text-xs text-muted-foreground">
            Đến ngày
            <Input
              type="date"
              value={values.dateTo ?? ""}
              min={values.dateFrom ?? localDateString(1)}
              onChange={(e) => onChange({ dateTo: e.target.value || undefined })}
              className="h-10 rounded-xl text-sm text-foreground"
            />
          </label>
        </div>
      </Group>

      <Group title="Giá mỗi người">
        <Pills
          options={PRICE_RANGES.map((range) => {
            const selected = rangeSelected(range, values.priceMin, values.priceMax);
            return {
              label: range.label,
              selected,
              onClick: () => onChange(selected ? { priceMin: undefined, priceMax: undefined } : { priceMin: range.min, priceMax: range.max }),
            };
          })}
        />
      </Group>

      <Group title="Thời lượng">
        <Pills
          options={DURATION_RANGES.map((range) => {
            const selected = rangeSelected(range, values.durationMin, values.durationMax);
            return {
              label: range.label,
              selected,
              onClick: () =>
                onChange(selected ? { durationMin: undefined, durationMax: undefined } : { durationMin: range.min, durationMax: range.max }),
            };
          })}
        />
      </Group>

      <Group title="Nơi khởi hành">
        <div className="flex flex-col gap-2">
          <SearchableSelect
            options={provinceOptions}
            value={values.departureLocationId}
            onChange={(id) => onChange({ departureLocationId: id })}
            placeholder="Mọi nơi"
            searchPlaceholder="Gõ tên tỉnh/thành..."
            emptyText="Không tìm thấy"
          />
          {values.departureLocationId && (
            <Button variant="link" size="sm" className="h-auto w-fit p-0" onClick={() => onChange({ departureLocationId: undefined })}>
              Bỏ chọn
            </Button>
          )}
        </div>
      </Group>

      <Group title="Loại hình">
        <div className="flex flex-wrap gap-1.5">
          {(categoriesQuery.data ?? []).map((category) => {
            const selected = values.categoryIds.includes(category.id);
            return (
              <button
                key={category.id}
                type="button"
                aria-pressed={selected}
                onClick={() => toggleCategory(category.id)}
                className={cn(
                  "rounded-full border px-3 py-1 text-sm transition-colors",
                  selected ? "border-primary bg-primary text-primary-foreground" : "bg-card hover:border-primary/50 hover:bg-accent",
                )}
              >
                {category.name}
              </button>
            );
          })}
        </div>
      </Group>
    </div>
  );
}

function Group({ title, children }: { title: string; children: ReactNode }) {
  return (
    <fieldset className="flex flex-col gap-2.5">
      <legend className="mb-2.5 text-sm font-medium">{title}</legend>
      {children}
    </fieldset>
  );
}

/** Nhóm nút chọn một (bấm lại nút đang chọn = bỏ chọn). */
function Pills({ options }: { options: { label: string; selected: boolean; onClick: () => void }[] }) {
  return (
    <div className="flex flex-wrap gap-1.5">
      {options.map((o) => (
        <button
          key={o.label}
          type="button"
          aria-pressed={o.selected}
          onClick={o.onClick}
          className={cn(
            "rounded-full border px-3 py-1 text-sm transition-colors",
            o.selected ? "border-primary bg-primary/10 font-medium text-primary" : "bg-card hover:bg-accent",
          )}
        >
          {o.label}
        </button>
      ))}
    </div>
  );
}
