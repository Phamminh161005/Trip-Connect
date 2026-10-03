"use client";

import Link from "next/link";
import { Logo } from "@/components/brand/Logo";
import { UserMenu } from "@/components/layout/UserMenu";
import { ADMIN_AREA_NAME } from "@/lib/constants";
import { cn } from "@/lib/utils";
import { useAdminNavItems } from "./AdminSidebar";

/** Thanh trên cùng. Màn hình hẹp: menu chuyển thành hàng tab cuộn ngang (thay cho menu bên trái). */
export function AdminTopBar() {
  const items = useAdminNavItems();
  return (
    <header className="sticky top-0 z-30 border-b bg-background/95 backdrop-blur">
      <div className="flex h-16 items-center justify-between gap-4 px-4 sm:px-6">
        <div className="lg:hidden">
          <Logo />
        </div>
        <p className="hidden font-semibold lg:block">{ADMIN_AREA_NAME}</p>
        <UserMenu />
      </div>
      <nav aria-label={ADMIN_AREA_NAME} className="flex gap-1 overflow-x-auto px-4 pb-2 lg:hidden">
        {items.map(({ href, label, active, badge }) => (
          <Link
            key={href}
            href={href}
            aria-current={active ? "page" : undefined}
            className={cn(
              "shrink-0 rounded-full px-3 py-1.5 text-sm font-medium",
              active ? "bg-accent text-accent-foreground" : "text-muted-foreground hover:bg-accent/60",
            )}
          >
            {label}
            {badge > 0 && <span className="ml-1.5 text-xs font-semibold text-primary">({badge})</span>}
          </Link>
        ))}
      </nav>
    </header>
  );
}
