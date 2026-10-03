"use client";

import { useState, type ReactNode } from "react";
import { Check, ChevronsUpDown } from "lucide-react";
import { Command, CommandEmpty, CommandInput, CommandItem, CommandList } from "@/components/ui/command";
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover";
import { cn } from "@/lib/utils";

export interface SelectOption<V extends string | number> {
  value: V;
  /** Chữ hiển thị sau khi chọn. */
  label: string;
  /** Chuỗi dùng để tìm kiếm (mặc định = label). */
  keywords?: string;
  /** Nội dung dòng trong danh sách (mặc định = label). */
  render?: ReactNode;
}

interface SearchableSelectProps<V extends string | number> {
  id?: string;
  options: SelectOption<V>[];
  value: V | null | undefined;
  onChange: (value: V) => void;
  placeholder: string;
  searchPlaceholder: string;
  emptyText: string;
  invalid?: boolean;
  onBlur?: () => void;
}

/** Ô chọn 1 giá trị trong danh sách dài, có ô gõ để tìm (Tỉnh/Thành, Ngân hàng...). */
export function SearchableSelect<V extends string | number>({
  id,
  options,
  value,
  onChange,
  placeholder,
  searchPlaceholder,
  emptyText,
  invalid,
  onBlur,
}: SearchableSelectProps<V>) {
  const [open, setOpen] = useState(false);
  const selected = options.find((option) => option.value === value);

  return (
    <Popover
      open={open}
      onOpenChange={(next) => {
        setOpen(next);
        if (!next) onBlur?.();
      }}
    >
      <PopoverTrigger
        id={id}
        aria-invalid={invalid}
        className="flex h-12 w-full items-center justify-between gap-2 rounded-xl border border-input bg-transparent px-3 text-left text-base outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50 aria-invalid:border-destructive"
      >
        <span className={cn("truncate", !selected && "text-muted-foreground")}>{selected ? selected.label : placeholder}</span>
        <ChevronsUpDown className="size-4 shrink-0 text-muted-foreground" />
      </PopoverTrigger>
      <PopoverContent className="w-(--radix-popover-trigger-width) p-0" align="start">
        <Command>
          <CommandInput placeholder={searchPlaceholder} />
          <CommandList className="max-h-72">
            <CommandEmpty>{emptyText}</CommandEmpty>
            {options.map((option) => (
              <CommandItem
                key={option.value}
                value={`${option.keywords ?? option.label} ${option.value}`}
                onSelect={() => {
                  onChange(option.value);
                  setOpen(false);
                  onBlur?.();
                }}
              >
                <Check className={cn("size-4", option.value === value ? "opacity-100" : "opacity-0")} />
                {option.render ?? option.label}
              </CommandItem>
            ))}
          </CommandList>
        </Command>
      </PopoverContent>
    </Popover>
  );
}
