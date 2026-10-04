"use client";

import { useState } from "react";
import { Star } from "lucide-react";
import { cn } from "@/lib/utils";

const LABELS = ["", "Rất tệ", "Tệ", "Bình thường", "Tốt", "Tuyệt vời"];

/** Hiện số sao (làm tròn); điểm chính xác hiện bằng số bên cạnh. */
export function Stars({ value, className }: { value: number; className?: string }) {
  return (
    <span className={cn("flex items-center gap-0.5", className)} aria-label={`${value} trên 5 sao`}>
      {[1, 2, 3, 4, 5].map((n) => (
        <Star key={n} className={cn("size-4", n <= Math.round(value) ? "fill-amber-400 text-amber-400" : "fill-muted text-muted")} />
      ))}
    </span>
  );
}

/** Chọn số sao 1–5 (rê chuột để xem trước). */
export function StarInput({ value, onChange, invalid }: { value: number; onChange: (value: number) => void; invalid?: boolean }) {
  const [hover, setHover] = useState(0);
  const shown = hover || value;
  return (
    <div className="flex items-center gap-3">
      <div className="flex gap-1" role="radiogroup" aria-label="Số sao" aria-invalid={invalid} onMouseLeave={() => setHover(0)}>
        {[1, 2, 3, 4, 5].map((n) => (
          <button
            key={n}
            type="button"
            role="radio"
            aria-checked={value === n}
            aria-label={`${n} sao — ${LABELS[n]}`}
            onClick={() => onChange(n)}
            onMouseEnter={() => setHover(n)}
            className="rounded-md p-0.5 outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
          >
            <Star className={cn("size-8 transition-colors", n <= shown ? "fill-amber-400 text-amber-400" : "fill-muted text-muted-foreground/40")} />
          </button>
        ))}
      </div>
      <span className="text-sm font-medium text-muted-foreground">{shown ? LABELS[shown] : "Chọn số sao"}</span>
    </div>
  );
}
