import Image from "next/image";
import Link from "next/link";
import type { ReactNode } from "react";
import { SiteHeader } from "@/components/layout/SiteHeader";
import { DESTINATIONS } from "./destinations";

// Lặp danh sách để lưới ảnh phủ kín màn hình rộng
const TILES = [...DESTINATIONS, ...DESTINATIONS, ...DESTINATIONS].slice(0, 18);

/**
 * Khung chung cho các trang đăng nhập / đăng ký / OTP...
 * Nền: lưới thẻ địa danh Việt Nam (làm mờ nhẹ), ở giữa là thẻ trắng chứa form.
 */
export function AuthShell({ children }: { children: ReactNode }) {
  return (
    <div className="flex min-h-svh flex-col">
      <SiteHeader />
      <main className="relative flex flex-1 items-center justify-center overflow-hidden px-4 py-10 sm:py-14">
        {/* Nền trang trí — ẩn với trình đọc màn hình */}
        <div aria-hidden="true" className="pointer-events-none absolute inset-0 hidden sm:block">
          <div className="grid h-full grid-cols-3 gap-5 p-5 md:grid-cols-4 lg:grid-cols-6">
            {TILES.map((place, index) => (
              <div
                key={`${place.slug}-${index}`}
                className={`relative overflow-hidden rounded-3xl shadow-sm ${index % 2 === 0 ? "translate-y-6" : "-translate-y-4"}`}
              >
                <Image
                  src={`/images/destinations/${place.slug}.jpg`}
                  alt=""
                  fill
                  sizes="(min-width: 1024px) 17vw, (min-width: 768px) 25vw, 33vw"
                  className="object-cover"
                  priority={index < 6}
                />
                <div className="absolute inset-0 bg-gradient-to-t from-black/45 via-transparent to-transparent" />
                <span className="absolute bottom-3 left-4 text-lg font-bold tracking-tight text-white drop-shadow">
                  {place.name}
                </span>
              </div>
            ))}
          </div>
          {/* Phủ lớp mờ để thẻ form ở giữa nổi bật */}
          <div className="absolute inset-0 bg-white/55 backdrop-blur-[2px]" />
        </div>
        <div className="absolute inset-0 bg-gradient-to-b from-accent to-background sm:hidden" aria-hidden="true" />

        <div className="relative z-10 w-full">{children}</div>

        <Link
          href="/credits"
          className="absolute right-4 bottom-3 z-10 text-xs text-muted-foreground underline-offset-4 hover:underline"
        >
          Ảnh: Wikimedia Commons (CC BY / CC BY-SA)
        </Link>
      </main>
    </div>
  );
}
