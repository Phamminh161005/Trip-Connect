"use client";

import type { ComponentProps } from "react";
import { Input } from "@/components/ui/input";

const format = new Intl.NumberFormat("vi-VN");

/** Ô nhập số tiền VNĐ: hiển thị có dấu chấm ngăn cách (3.490.000), giá trị thật là số. */
export function PriceInput({
  value,
  onChange,
  ...props
}: Omit<ComponentProps<typeof Input>, "value" | "onChange" | "type"> & {
  value: number | undefined;
  onChange: (value: number | undefined) => void;
}) {
  return (
    <div className="relative">
      <Input
        {...props}
        type="text"
        inputMode="numeric"
        value={value === undefined || Number.isNaN(value) ? "" : format.format(value)}
        onChange={(e) => {
          const digits = e.target.value.replace(/\D/g, "").slice(0, 10);
          onChange(digits === "" ? undefined : Number(digits));
        }}
        className="h-12 rounded-xl pr-9 text-base"
      />
      <span className="pointer-events-none absolute top-1/2 right-3 -translate-y-1/2 text-muted-foreground">đ</span>
    </div>
  );
}
