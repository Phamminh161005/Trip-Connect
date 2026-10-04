import Link from "next/link";
import { cn } from "@/lib/utils";

/** Biểu tượng TripConnect: la bàn cách điệu — gợi ý "khám phá" + "kết nối". */
export function LogoMark({ className }: { className?: string }) {
  return (
    <svg viewBox="0 0 32 32" aria-hidden="true" className={cn("size-8 text-primary", className)}>
      <circle cx="16" cy="16" r="13" fill="none" stroke="currentColor" strokeWidth="2.5" />
      <path d="M16 6.5 19.2 16 16 25.5 12.8 16Z" fill="currentColor" opacity="0.9" />
      <circle cx="16" cy="16" r="2.2" fill="white" />
    </svg>
  );
}

/** @param inverted chữ trắng — dùng khi đặt trên ảnh nền tối */
export function Logo({ className, inverted = false }: { className?: string; inverted?: boolean }) {
  return (
    <Link href="/" className={cn("flex items-center gap-2 rounded-lg outline-none focus-visible:ring-3 focus-visible:ring-ring/50", className)}>
      <LogoMark className={cn("transition-colors", inverted && "text-white")} />
      <span className={cn("text-xl font-bold tracking-tight text-primary transition-colors", inverted && "text-white")}>
        TripConnect
      </span>
    </Link>
  );
}
