"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { Bell, KeyRound, Ticket, UserRound } from "lucide-react";
import { cn } from "@/lib/utils";

const ITEMS = [
  { href: "/notifications", label: "Thông báo", icon: Bell },
  { href: "/account/bookings", label: "Đơn đặt của tôi", icon: Ticket },
  { href: "/account", label: "Thông tin cá nhân", icon: UserRound },
  { href: "/account/change-password", label: "Đổi mật khẩu", icon: KeyRound },
];

export function AccountNav() {
  const pathname = usePathname();
  return (
    <nav aria-label="Tài khoản" className="flex gap-1 overflow-x-auto md:flex-col">
      {ITEMS.map(({ href, label, icon: Icon }) => {
        const active = href === "/account" ? pathname === href : pathname === href || pathname.startsWith(`${href}/`);
        return (
          <Link
            key={href}
            href={href}
            aria-current={active ? "page" : undefined}
            className={cn(
              "flex shrink-0 items-center gap-2.5 rounded-xl px-3.5 py-2.5 text-sm font-medium transition-colors",
              active ? "bg-accent text-accent-foreground" : "text-muted-foreground hover:bg-accent/60 hover:text-foreground",
            )}
          >
            <Icon className="size-4" />
            {label}
          </Link>
        );
      })}
    </nav>
  );
}
