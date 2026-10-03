"use client";

import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs";

/** Hàng tab lọc theo trạng thái (chỉ đổi bộ lọc, không có nội dung riêng cho từng tab). Cuộn ngang trên điện thoại. */
export function FilterTabs<T extends string>({
  value,
  options,
  onChange,
  label,
}: {
  value: T;
  options: { value: T; label: string }[];
  onChange: (value: T) => void;
  label: string;
}) {
  return (
    <Tabs value={value} onValueChange={(next) => onChange(next as T)}>
      <div className="-mx-4 overflow-x-auto px-4 sm:mx-0 sm:px-0">
        <TabsList aria-label={label} className="h-9!">
          {options.map((option) => (
            <TabsTrigger key={option.value} value={option.value} className="px-3">
              {option.label}
            </TabsTrigger>
          ))}
        </TabsList>
      </div>
    </Tabs>
  );
}
