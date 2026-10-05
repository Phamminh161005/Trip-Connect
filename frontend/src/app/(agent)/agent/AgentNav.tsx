"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { FileBadge, LayoutDashboard, Map, MessageSquareText, Star, Ticket } from "lucide-react";
import { AGENT_AREA_NAME } from "@/lib/constants";
import { cn } from "@/lib/utils";

// TODO: Doanh thu
const ITEMS = [
  { href: "/agent", label: "Tổng quan", icon: LayoutDashboard, exact: true },
  { href: "/agent/tours", label: "Tour của tôi", icon: Map },
  { href: "/agent/bookings", label: "Đơn đặt tour", icon: Ticket },
  { href: "/agent/requests", label: "Yêu cầu tư vấn", icon: MessageSquareText },
  { href: "/agent/reviews", label: "Đánh giá", icon: Star },
  { href: "/agent/profile", label: "Hồ sơ kinh doanh", icon: FileBadge },
];

export function AgentNav() {
  const pathname = usePathname();
  return (
    <nav aria-label={AGENT_AREA_NAME} className="flex flex-col gap-3">
      <p className="hidden px-3.5 text-xs font-semibold tracking-wide text-muted-foreground uppercase md:block">
        {AGENT_AREA_NAME}
      </p>
      <div className="flex gap-1 overflow-x-auto md:flex-col">
        {ITEMS.map(({ href, label, icon: Icon, exact }) => {
          const active = exact ? pathname === href : pathname === href || pathname.startsWith(`${href}/`);
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
      </div>
    </nav>
  );
}
