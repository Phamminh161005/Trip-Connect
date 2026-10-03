"use client";

import { useMemo, useState } from "react";
import { Check, ChevronsUpDown, X } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Command, CommandEmpty, CommandGroup, CommandInput, CommandItem, CommandList } from "@/components/ui/command";
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover";
import { cn } from "@/lib/utils";
import type { LocationResponse } from "@/types/catalog";

export function locationLabel(location: LocationResponse): string {
  return location.province ? location.province : location.country;
}

interface LocationMultiSelectProps {
  id?: string;
  locations: LocationResponse[];
  value: number[];
  onChange: (ids: number[]) => void;
  invalid?: boolean;
}

/** Chọn nhiều địa điểm, có ô tìm kiếm. Tỉnh/thành Việt Nam xếp trước, quốc gia khác xếp sau. */
export function LocationMultiSelect({ id, locations, value, onChange, invalid }: LocationMultiSelectProps) {
  const [open, setOpen] = useState(false);

  const { vietnam, international, byId } = useMemo(() => {
    const vn = locations.filter((l) => l.country === "Việt Nam").sort((a, b) => locationLabel(a).localeCompare(locationLabel(b), "vi"));
    const intl = locations.filter((l) => l.country !== "Việt Nam").sort((a, b) => a.country.localeCompare(b.country, "vi"));
    return { vietnam: vn, international: intl, byId: new Map(locations.map((l) => [l.id, l])) };
  }, [locations]);

  const toggle = (locationId: number) =>
    onChange(value.includes(locationId) ? value.filter((v) => v !== locationId) : [...value, locationId]);

  const renderItems = (items: LocationResponse[]) =>
    items.map((location) => {
      const selected = value.includes(location.id);
      return (
        <CommandItem key={location.id} value={`${locationLabel(location)} ${location.country}`} onSelect={() => toggle(location.id)}>
          <Check className={cn("size-4", selected ? "opacity-100" : "opacity-0")} />
          {locationLabel(location)}
        </CommandItem>
      );
    });

  return (
    <div className="flex flex-col gap-2">
      <Popover open={open} onOpenChange={setOpen}>
        <PopoverTrigger
          id={id}
          aria-invalid={invalid}
          className="flex h-12 w-full items-center justify-between rounded-xl border border-input bg-transparent px-3 text-left text-base outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50 aria-invalid:border-destructive"
        >
          <span className="text-muted-foreground">
            {value.length ? `Đã chọn ${value.length} khu vực` : "Tìm và chọn tỉnh/thành, quốc gia..."}
          </span>
          <ChevronsUpDown className="size-4 text-muted-foreground" />
        </PopoverTrigger>
        <PopoverContent className="w-(--radix-popover-trigger-width) p-0" align="start">
          <Command>
            <CommandInput placeholder="Gõ để tìm, ví dụ: Đà Nẵng, Nhật Bản..." />
            <CommandList className="max-h-72">
              <CommandEmpty>Không tìm thấy địa điểm</CommandEmpty>
              <CommandGroup heading="Việt Nam">{renderItems(vietnam)}</CommandGroup>
              <CommandGroup heading="Quốc tế">{renderItems(international)}</CommandGroup>
            </CommandList>
          </Command>
        </PopoverContent>
      </Popover>

      {value.length > 0 && (
        <div className="flex flex-wrap gap-1.5">
          {value.map((locationId) => {
            const location = byId.get(locationId);
            if (!location) return null;
            return (
              <Badge key={locationId} variant="secondary" className="gap-1 py-1 pr-1 pl-2.5">
                {locationLabel(location)}
                <button
                  type="button"
                  onClick={() => toggle(locationId)}
                  className="rounded-full p-0.5 hover:bg-background"
                  aria-label={`Bỏ chọn ${locationLabel(location)}`}
                >
                  <X className="size-3" />
                </button>
              </Badge>
            );
          })}
        </div>
      )}
    </div>
  );
}
