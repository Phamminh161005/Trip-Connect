"use client";

import { useEffect, useState } from "react";
import { Timer } from "lucide-react";

/** Đếm ngược thời gian giữ chỗ còn lại ("Giữ chỗ còn 12:34"). Hết giờ thì gọi onExpire một lần. */
export function HoldCountdown({ expiresAt, onExpire }: { expiresAt: string; onExpire?: () => void }) {
  const deadline = new Date(expiresAt).getTime();
  const [now, setNow] = useState(() => Date.now());
  const left = Math.max(0, Math.floor((deadline - now) / 1000));

  useEffect(() => {
    if (left <= 0) {
      onExpire?.();
      return;
    }
    const timer = setTimeout(() => setNow(Date.now()), 1000);
    return () => clearTimeout(timer);
    // onExpire chỉ cần gọi khi hết giờ
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [now, left <= 0]);

  if (left <= 0) return <span className="font-medium text-destructive">Đã hết thời gian giữ chỗ</span>;
  const minutes = String(Math.floor(left / 60)).padStart(2, "0");
  const seconds = String(left % 60).padStart(2, "0");
  return (
    <span className="flex items-center gap-1.5 font-medium text-amber-900">
      <Timer className="size-4" /> Giữ chỗ còn {minutes}:{seconds}
    </span>
  );
}
