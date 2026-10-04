"use client";

import { useId, useMemo, useRef, useState, type KeyboardEvent } from "react";
import { useRouter } from "next/navigation";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { CalendarDays, Clock, MapPin, Search, X } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { locationLabel } from "@/components/auth/register/LocationMultiSelect";
import { getLocations } from "@/lib/api/catalog";
import { clearSearchHistory, deleteRecentSearch, getRecentSearches } from "@/lib/api/search";
import { useAuth } from "@/lib/auth/AuthProvider";
import { normalizeText } from "@/lib/text";
import { localDateString } from "@/lib/tour/labels";
import { cn } from "@/lib/utils";
import type { LocationResponse } from "@/types/catalog";
import type { RecentSearch } from "@/types/search";

export const RECENT_SEARCHES_KEY = ["me", "search-history"] as const;
const MAX_SUGGESTIONS = 6;

export interface SearchBarValue {
  q?: string;
  destinationId?: number;
  dateFrom?: string;
}

/** Ghép link trang kết quả /tours?... từ ô tìm kiếm. */
export function searchHref(value: SearchBarValue): string {
  const params = new URLSearchParams();
  if (value.destinationId) params.set("destinationId", String(value.destinationId));
  else if (value.q?.trim()) params.set("q", value.q.trim());
  if (value.dateFrom) params.set("dateFrom", value.dateFrom);
  const text = params.toString();
  return text ? `/tours?${text}` : "/tours";
}

type Option =
  | { kind: "location"; location: LocationResponse }
  | { kind: "recent"; recent: RecentSearch };

/**
 * Ô tìm kiếm: "Bạn muốn đi đâu?" (gõ tên tour / điểm đến, có gợi ý điểm đến không cần dấu)
 * + ngày khởi hành + nút Tìm. Đăng nhập rồi thì hiện "Tìm kiếm gần đây" khi bấm vào ô.
 */
export function TourSearchBar({ initial, variant = "hero" }: { initial?: SearchBarValue; variant?: "hero" | "compact" }) {
  const router = useRouter();
  const queryClient = useQueryClient();
  const { status } = useAuth();
  const listId = useId();
  const inputRef = useRef<HTMLInputElement>(null);

  const locationsQuery = useQuery({ queryKey: ["locations"], queryFn: getLocations, staleTime: Infinity });
  const recentQuery = useQuery({
    queryKey: RECENT_SEARCHES_KEY,
    queryFn: getRecentSearches,
    enabled: status === "authenticated",
  });

  const initialLocation = locationsQuery.data?.find((l) => l.id === initial?.destinationId);
  const [text, setText] = useState(initial?.q ?? "");
  const [destination, setDestination] = useState<LocationResponse | null>(null);
  const [dateFrom, setDateFrom] = useState(initial?.dateFrom ?? "");
  const [open, setOpen] = useState(false);
  const [active, setActive] = useState(-1);
  /** Người dùng đã sửa ô điểm đến -> bỏ giá trị lấy từ đường link. */
  const [edited, setEdited] = useState(false);

  // Điểm đến lấy từ đường link (khi danh sách địa điểm tải xong) — người dùng chưa chọn lại thì dùng giá trị này
  const chosen = destination ?? (!edited && initial?.destinationId ? (initialLocation ?? null) : null);
  const shownText = chosen && !destination ? locationLabel(chosen) : text;

  const options = useMemo<Option[]>(() => {
    const keyword = normalizeText(shownText);
    if (!keyword) {
      return (recentQuery.data ?? []).map((recent) => ({ kind: "recent", recent }) as Option);
    }
    return (locationsQuery.data ?? [])
      .filter((l) => ` ${normalizeText(locationLabel(l))}`.includes(` ${keyword}`))
      .slice(0, MAX_SUGGESTIONS)
      .map((location) => ({ kind: "location", location }) as Option);
  }, [shownText, locationsQuery.data, recentQuery.data]);

  const go = (value: SearchBarValue) => {
    setOpen(false);
    router.push(searchHref(value));
  };

  const submit = () => go({ q: chosen ? undefined : text, destinationId: chosen?.id, dateFrom: dateFrom || undefined });

  const pick = (option: Option) => {
    if (option.kind === "location") {
      setDestination(option.location);
      setText(locationLabel(option.location));
      setEdited(true);
      setOpen(false);
    } else {
      const r = option.recent;
      go({ q: r.keyword ?? undefined, destinationId: r.destination?.id, dateFrom: r.dateFrom ?? undefined });
    }
  };

  const onKeyDown = (e: KeyboardEvent<HTMLInputElement>) => {
    if (e.key === "ArrowDown" && options.length) {
      e.preventDefault();
      setOpen(true);
      setActive((i) => (i + 1) % options.length);
    } else if (e.key === "ArrowUp" && options.length) {
      e.preventDefault();
      setActive((i) => (i <= 0 ? options.length - 1 : i - 1));
    } else if (e.key === "Enter") {
      e.preventDefault();
      if (open && active >= 0 && options[active]) pick(options[active]);
      else submit();
    } else if (e.key === "Escape") {
      setOpen(false);
    }
  };

  const removeRecent = async (id: number) => {
    await deleteRecentSearch(id).catch(() => {});
    await queryClient.invalidateQueries({ queryKey: RECENT_SEARCHES_KEY });
  };

  const clearRecent = async () => {
    await clearSearchHistory().catch(() => {});
    await queryClient.invalidateQueries({ queryKey: RECENT_SEARCHES_KEY });
  };

  const hero = variant === "hero";
  const showList = open && options.length > 0;

  return (
    <form
      role="search"
      onSubmit={(e) => {
        e.preventDefault();
        submit();
      }}
      className={cn(
        "flex w-full flex-col gap-2 bg-background sm:flex-row sm:items-center",
        hero ? "rounded-2xl p-2 shadow-xl sm:rounded-full" : "rounded-2xl border p-1.5 sm:rounded-full",
      )}
    >
      <div className="relative min-w-0 flex-1">
        <MapPin className="pointer-events-none absolute top-1/2 left-4 size-5 -translate-y-1/2 text-primary" />
        <Input
          ref={inputRef}
          value={shownText}
          onChange={(e) => {
            setText(e.target.value);
            setDestination(null);
            setEdited(true);
            setActive(-1);
            setOpen(true);
          }}
          onFocus={() => setOpen(true)}
          onBlur={() => setOpen(false)}
          onKeyDown={onKeyDown}
          placeholder="Bạn muốn đi đâu? Ví dụ: Hạ Long, Nhật Bản, du thuyền..."
          aria-label="Điểm đến hoặc tên tour"
          role="combobox"
          aria-expanded={showList}
          aria-controls={listId}
          aria-autocomplete="list"
          className={cn("rounded-full border-0 pr-9 pl-12 shadow-none focus-visible:ring-0", hero ? "h-14 text-base" : "h-11")}
        />
        {shownText && (
          <button
            type="button"
            onMouseDown={(e) => e.preventDefault()}
            onClick={() => {
              setText("");
              setDestination(null);
              setEdited(true);
              inputRef.current?.focus();
            }}
            className="absolute top-1/2 right-2 -translate-y-1/2 rounded-full p-1.5 text-muted-foreground hover:bg-accent"
            aria-label="Xóa nội dung tìm kiếm"
          >
            <X className="size-4" />
          </button>
        )}

        {showList && (
          <div
            id={listId}
            role="listbox"
            className="absolute top-full right-0 left-0 z-40 mt-2 overflow-hidden rounded-2xl border bg-popover py-2 text-popover-foreground shadow-xl"
          >
            {options[0].kind === "recent" && (
              <div className="flex items-center justify-between px-4 pb-1 text-xs font-semibold text-muted-foreground uppercase">
                Tìm kiếm gần đây
                <button
                  type="button"
                  onMouseDown={(e) => e.preventDefault()}
                  onClick={clearRecent}
                  className="font-normal normal-case hover:text-foreground hover:underline"
                >
                  Xóa tất cả
                </button>
              </div>
            )}
            {options[0].kind === "location" && (
              <p className="px-4 pb-1 text-xs font-semibold text-muted-foreground uppercase">Điểm đến</p>
            )}
            {options.map((option, index) => (
              <div
                key={option.kind === "location" ? `l${option.location.id}` : `r${option.recent.id}`}
                role="option"
                aria-selected={index === active}
                onMouseDown={(e) => e.preventDefault()}
                onClick={() => pick(option)}
                onMouseEnter={() => setActive(index)}
                className={cn("flex cursor-pointer items-center gap-3 px-4 py-2.5 text-sm", index === active && "bg-accent")}
              >
                {option.kind === "location" ? (
                  <>
                    <MapPin className="size-4 shrink-0 text-muted-foreground" />
                    <span className="flex-1">{locationLabel(option.location)}</span>
                    <span className="text-xs text-muted-foreground">{option.location.province ? "Việt Nam" : "Quốc tế"}</span>
                  </>
                ) : (
                  <>
                    <Clock className="size-4 shrink-0 text-muted-foreground" />
                    <span className="flex-1 truncate">
                      {[option.recent.keyword, option.recent.destination && locationLabel(option.recent.destination)]
                        .filter(Boolean)
                        .join(" · ")}
                    </span>
                    <button
                      type="button"
                      onClick={(e) => {
                        e.stopPropagation();
                        void removeRecent(option.recent.id);
                      }}
                      className="rounded-full p-1 text-muted-foreground hover:bg-background"
                      aria-label="Xóa khỏi lịch sử"
                    >
                      <X className="size-3.5" />
                    </button>
                  </>
                )}
              </div>
            ))}
            {options[0].kind === "location" && shownText.trim() && (
              <div
                role="option"
                aria-selected={false}
                onMouseDown={(e) => e.preventDefault()}
                onClick={submit}
                className="flex cursor-pointer items-center gap-3 border-t px-4 py-2.5 text-sm hover:bg-accent"
              >
                <Search className="size-4 shrink-0 text-muted-foreground" />
                Tìm tour có chữ “{shownText.trim()}”
              </div>
            )}
          </div>
        )}
      </div>

      <div className={cn("relative sm:w-52", hero && "sm:border-l")}>
        <CalendarDays className="pointer-events-none absolute top-1/2 left-4 size-5 -translate-y-1/2 text-primary" />
        <Input
          type="date"
          value={dateFrom}
          min={localDateString(1)}
          onChange={(e) => setDateFrom(e.target.value)}
          aria-label="Khởi hành từ ngày"
          className={cn("rounded-full border-0 pl-12 shadow-none focus-visible:ring-0", hero ? "h-14 text-base" : "h-11")}
        />
      </div>

      <Button type="submit" className={cn("rounded-full font-semibold", hero ? "h-14 px-8 text-base" : "h-11 px-6")}>
        <Search /> Tìm tour
      </Button>
    </form>
  );
}
