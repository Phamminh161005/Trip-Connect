"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { Logo } from "@/components/brand/Logo";
import { useAuth } from "@/lib/auth/AuthProvider";
import { ADMIN_AREA_NAME, AGENT_AREA_NAME } from "@/lib/constants";
import { cn } from "@/lib/utils";
import { UserMenu } from "./UserMenu";

const linkClass = "hidden rounded-full px-4 py-2.5 text-sm font-semibold transition-colors hover:bg-accent sm:block";

/** Link ở góc phải header, đổi theo vai trò: khách -> mời làm đối tác; Agent -> chuyển qua lại giữa 2 chế độ. */
function ModeLink() {
  const { status, user } = useAuth();
  const pathname = usePathname();

  if (status === "loading") return null;

  // Chưa đăng nhập: mời đăng ký làm đối tác
  // (Khách hàng đã đăng nhập không thấy link này: hệ thống chưa hỗ trợ chuyển tài khoản Khách thành Agent)
  if (!user) {
    return (
      <Link href="/register?type=agent" className={linkClass}>
        Trở thành đối tác
      </Link>
    );
  }

  if (user.role === "ADMIN") {
    return (
      <Link href="/admin" className={linkClass}>
        {ADMIN_AREA_NAME}
      </Link>
    );
  }

  if (user.role === "AGENT") {
    const inAgentArea = pathname === "/agent" || pathname.startsWith("/agent/");
    return inAgentArea ? (
      <Link href="/" className={linkClass}>
        Khám phá tour
      </Link>
    ) : (
      <Link href="/agent" className={linkClass}>
        {AGENT_AREA_NAME}
      </Link>
    );
  }

  return null;
}

export function SiteHeader({ className }: { className?: string }) {
  return (
    <header className={cn("sticky top-0 z-30 border-b bg-background/95 backdrop-blur", className)}>
      <div className="mx-auto flex h-18 max-w-7xl items-center justify-between gap-4 px-4 sm:px-6 lg:px-10">
        <Logo />
        <nav className="flex items-center gap-2">
          <ModeLink />
          <UserMenu />
        </nav>
      </div>
    </header>
  );
}
