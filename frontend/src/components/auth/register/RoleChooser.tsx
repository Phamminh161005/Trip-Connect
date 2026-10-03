"use client";

import { Briefcase, ChevronRight, Luggage } from "lucide-react";

export type RegisterRole = "CUSTOMER" | "AGENT";

const OPTIONS: { role: RegisterRole; title: string; description: string; icon: typeof Luggage }[] = [
  {
    role: "CUSTOMER",
    title: "Khách du lịch",
    description: "Tìm kiếm, đặt tour và gửi yêu cầu thiết kế tour riêng.",
    icon: Luggage,
  },
  {
    role: "AGENT",
    title: "Đối tác kinh doanh tour",
    description: "Đăng bán tour và nhận yêu cầu tư vấn sau khi hồ sơ được duyệt.",
    icon: Briefcase,
  },
];

export function RoleChooser({ onSelect }: { onSelect: (role: RegisterRole) => void }) {
  return (
    <div className="flex flex-col gap-3" role="list">
      {OPTIONS.map(({ role, title, description, icon: Icon }) => (
        <button
          key={role}
          type="button"
          role="listitem"
          onClick={() => onSelect(role)}
          className="group flex items-center gap-4 rounded-2xl border bg-card p-4 text-left transition-all outline-none hover:border-primary/50 hover:bg-accent/50 hover:shadow-sm focus-visible:ring-3 focus-visible:ring-ring/50"
        >
          <span className="flex size-12 shrink-0 items-center justify-center rounded-xl bg-accent text-primary">
            <Icon className="size-6" />
          </span>
          <span className="flex flex-1 flex-col gap-0.5">
            <span className="font-semibold">{title}</span>
            <span className="text-sm text-muted-foreground">{description}</span>
          </span>
          <ChevronRight className="size-5 text-muted-foreground transition-transform group-hover:translate-x-0.5" />
        </button>
      ))}
    </div>
  );
}
