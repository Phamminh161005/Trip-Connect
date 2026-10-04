"use client";

import { useSyncExternalStore } from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { Logo } from "@/components/brand/Logo";
import { NotificationBell } from "@/components/notification/NotificationBell";
import { useAuth } from "@/lib/auth/AuthProvider";
import { ADMIN_AREA_NAME, AGENT_AREA_NAME } from "@/lib/constants";
import { cn } from "@/lib/utils";
import { MainNav } from "./MainNav";
import { UserMenu } from "./UserMenu";

/** Link ở góc phải header, đổi theo vai trò: khách -> mời làm đối tác; Agent / Admin -> vào khu vực quản lý. */
function ModeLink({ inverted }: { inverted: boolean }) {
  const { status, user } = useAuth();
  const pathname = usePathname();

  if (status === "loading") return null;

  // Chưa đăng nhập: mời đăng ký làm đối tác
  // (Khách hàng đã đăng nhập không thấy link này: hệ thống chưa hỗ trợ chuyển tài khoản Khách thành Agent)
  if (!user) {
    return (
      <Link
        href="/register?type=agent"
        className={cn(
          "hidden rounded-full border px-4 py-2 text-sm font-semibold transition-colors lg:block",
          inverted ? "border-white/60 text-white hover:bg-white/15" : "border-primary/40 text-primary hover:bg-primary/5",
        )}
      >
        Trở thành đối tác
      </Link>
    );
  }

  const area = user.role === "ADMIN" ? { href: "/admin", label: ADMIN_AREA_NAME } : user.role === "AGENT" ? { href: "/agent", label: AGENT_AREA_NAME } : null;
  if (!area || pathname === area.href || pathname.startsWith(`${area.href}/`)) return null;
  return (
    <Link
      href={area.href}
      className={cn(
        "hidden rounded-full px-4 py-2 text-sm font-semibold transition-colors lg:block",
        inverted ? "text-white hover:bg-white/15" : "hover:bg-accent",
      )}
    >
      {area.label}
    </Link>
  );
}

const subscribeScroll = (onChange: () => void) => {
  window.addEventListener("scroll", onChange, { passive: true });
  return () => window.removeEventListener("scroll", onChange);
};

/** Đã cuộn khỏi đầu trang chưa — header trang chủ đổi từ trong suốt sang nền kính mờ. */
function useScrolled() {
  return useSyncExternalStore(
    subscribeScroll,
    () => window.scrollY > 24,
    () => false,
  );
}

/**
 * Header chung. Ở trang chủ header nằm đè lên ảnh lớn (trang chủ kéo ảnh lên dưới header):
 * trong suốt + chữ trắng, cuộn xuống thì chuyển nền kính mờ như các trang khác.
 */
export function SiteHeader({ className }: { className?: string }) {
  const pathname = usePathname();
  const scrolled = useScrolled();
  const inverted = pathname === "/" && !scrolled;

  return (
    <header
      className={cn(
        "sticky top-0 z-30 transition-[background-color,box-shadow,border-color] duration-300",
        inverted ? "border-b border-transparent bg-transparent" : "border-b bg-background/85 shadow-xs backdrop-blur-md",
        className,
      )}
    >
      <div className="mx-auto flex h-18 max-w-7xl items-center justify-between gap-4 px-4 sm:px-6 lg:px-10">
        <div className="flex items-center gap-6 lg:gap-10">
          <Logo inverted={inverted} />
          <MainNav inverted={inverted} />
        </div>
        <div className="flex items-center gap-2">
          <ModeLink inverted={inverted} />
          <NotificationBell inverted={inverted} />
          <UserMenu />
        </div>
      </div>
    </header>
  );
}
