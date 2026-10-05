"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { ArrowLeft, FilePen, LayoutDashboard, Map, Sparkles, Star, Store, Ticket, Users } from "lucide-react";
import { Logo } from "@/components/brand/Logo";
import { ADMIN_AREA_NAME } from "@/lib/constants";
import { cn } from "@/lib/utils";
import { useAdminSummary } from "./adminQueries";

type CountKey = "pendingAgentProfiles" | "pendingChangeRequests" | "pendingTours" | "manualRefunds" | "pendingCustomRequests";

const ITEMS: { href: string; label: string; icon: typeof Users; exact?: boolean; count?: CountKey }[] = [
  { href: "/admin", label: "Tổng quan", icon: LayoutDashboard, exact: true },
  { href: "/admin/agents", label: "Hồ sơ đối tác", icon: Store, count: "pendingAgentProfiles" },
  { href: "/admin/change-requests", label: "Yêu cầu cập nhật", icon: FilePen, count: "pendingChangeRequests" },
  { href: "/admin/tours", label: "Tour", icon: Map, count: "pendingTours" },
  { href: "/admin/bookings", label: "Đơn đặt tour", icon: Ticket, count: "manualRefunds" },
  { href: "/admin/requests", label: "Yêu cầu tour riêng", icon: Sparkles, count: "pendingCustomRequests" },
  { href: "/admin/reviews", label: "Đánh giá", icon: Star },
  { href: "/admin/users", label: "Người dùng", icon: Users },
];

export function useAdminNavItems() {
  const pathname = usePathname();
  const { data: summary } = useAdminSummary();
  return ITEMS.map((item) => ({
    ...item,
    active: item.exact ? pathname === item.href : pathname === item.href || pathname.startsWith(`${item.href}/`),
    badge: item.count && summary ? summary[item.count] : 0,
  }));
}

/** Menu bên trái (màn hình rộng). Số trong ô tròn = việc đang chờ xử lý. */
export function AdminSidebar() {
  const items = useAdminNavItems();
  return (
    <aside className="sticky top-0 hidden h-svh w-64 shrink-0 flex-col border-r bg-background lg:flex">
      <div className="flex h-16 items-center px-5">
        <Logo />
      </div>
      <p className="px-5 pt-4 pb-2 text-xs font-semibold tracking-wide text-muted-foreground uppercase">{ADMIN_AREA_NAME}</p>
      <nav aria-label={ADMIN_AREA_NAME} className="flex flex-1 flex-col gap-1 px-3">
        {items.map(({ href, label, icon: Icon, active, badge }) => (
          <Link
            key={href}
            href={href}
            aria-current={active ? "page" : undefined}
            className={cn(
              "flex items-center gap-2.5 rounded-xl px-3 py-2.5 text-sm font-medium transition-colors",
              active ? "bg-accent text-accent-foreground" : "text-muted-foreground hover:bg-accent/60 hover:text-foreground",
            )}
          >
            <Icon className="size-4" />
            <span className="flex-1">{label}</span>
            {badge > 0 && (
              <span className="min-w-6 rounded-full bg-primary px-2 py-0.5 text-center text-xs font-semibold text-primary-foreground">
                {badge}
              </span>
            )}
          </Link>
        ))}
      </nav>
      <Link href="/" className="m-3 flex items-center gap-2 rounded-xl px-3 py-2.5 text-sm text-muted-foreground hover:bg-accent/60 hover:text-foreground">
        <ArrowLeft className="size-4" /> Về trang chủ
      </Link>
    </aside>
  );
}
