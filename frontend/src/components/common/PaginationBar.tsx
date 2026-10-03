"use client";

import { ChevronLeft, ChevronRight } from "lucide-react";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";

/**
 * Các ô số trang cần hiện (đánh số từ 0), "gap" = dấu "…".
 * Luôn có trang đầu, trang cuối và 1 trang mỗi bên trang hiện tại: 1 … 4 [5] 6 … 12
 */
export function pageItems(page: number, totalPages: number): (number | "gap")[] {
  if (totalPages <= 7) return Array.from({ length: totalPages }, (_, i) => i);
  const pages = new Set([0, totalPages - 1, page - 1, page, page + 1]);
  // Gần đầu / gần cuối thì hiện thêm để thanh luôn đủ 7 ô, không bị co giãn khi bấm
  if (page <= 3) [1, 2, 3, 4].forEach((p) => pages.add(p));
  if (page >= totalPages - 4) [totalPages - 5, totalPages - 4, totalPages - 3, totalPages - 2].forEach((p) => pages.add(p));

  const sorted = [...pages].filter((p) => p >= 0 && p < totalPages).sort((a, b) => a - b);
  const items: (number | "gap")[] = [];
  sorted.forEach((p, i) => {
    if (i > 0 && p - sorted[i - 1] > 1) items.push("gap");
    items.push(p);
  });
  return items;
}

/** Thanh phân trang: "Hiển thị 21–40 / 57" + ‹ 1 2 [3] … 12 ›. page bắt đầu từ 0 (giống Backend). */
export function PaginationBar({
  page,
  size,
  totalElements,
  totalPages,
  onPageChange,
}: {
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  onPageChange: (page: number) => void;
}) {
  if (totalElements === 0) return null;
  const from = page * size + 1;
  const to = Math.min((page + 1) * size, totalElements);

  return (
    <nav aria-label="Phân trang" className="flex flex-col items-center justify-between gap-3 pt-4 text-sm text-muted-foreground sm:flex-row">
      <span>
        Hiển thị {from}–{to} / {totalElements}
      </span>
      {totalPages > 1 && (
        <div className="flex items-center gap-1">
          <Button
            variant="outline"
            size="icon"
            className="size-9 rounded-lg"
            disabled={page <= 0}
            onClick={() => onPageChange(page - 1)}
            aria-label="Trang trước"
          >
            <ChevronLeft />
          </Button>
          {pageItems(page, totalPages).map((item, index) =>
            item === "gap" ? (
              <span key={`gap-${index}`} className="w-6 text-center" aria-hidden>
                …
              </span>
            ) : (
              <Button
                key={item}
                variant={item === page ? "default" : "ghost"}
                size="icon"
                className={cn("size-9 rounded-lg tabular-nums", item !== page && "text-foreground")}
                aria-current={item === page ? "page" : undefined}
                aria-label={`Trang ${item + 1}`}
                onClick={() => item !== page && onPageChange(item)}
              >
                {item + 1}
              </Button>
            ),
          )}
          <Button
            variant="outline"
            size="icon"
            className="size-9 rounded-lg"
            disabled={page + 1 >= totalPages}
            onClick={() => onPageChange(page + 1)}
            aria-label="Trang sau"
          >
            <ChevronRight />
          </Button>
        </div>
      )}
    </nav>
  );
}
