"use client";

import type { ReactNode } from "react";
import Link from "next/link";
import { ShieldAlert } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { errorMessage } from "@/lib/api/errors";
import { useAgentProfile } from "./profile/useAgentProfile";

/** Chức năng chỉ dành cho Agent đã được duyệt hồ sơ (đăng tour...). Backend cũng chặn (AgentAccessGuard). */
export function ApprovedAgentGate({ children }: { children: ReactNode }) {
  const { data: profile, isPending, error } = useAgentProfile();

  if (isPending) return <Skeleton className="h-64 rounded-2xl" />;
  if (error) return <p className="text-destructive">{errorMessage(error)}</p>;
  if (profile.status !== "APPROVED") {
    return (
      <div className="flex flex-col items-center gap-4 rounded-2xl border bg-card p-10 text-center">
        <ShieldAlert className="size-10 text-amber-500" />
        <div>
          <p className="text-lg font-semibold">Hồ sơ kinh doanh chưa được duyệt</p>
          <p className="text-muted-foreground">Bạn cần hoàn thiện và được duyệt hồ sơ trước khi đăng bán tour.</p>
        </div>
        <Button asChild className="rounded-xl">
          <Link href="/agent/profile">Đến Hồ sơ kinh doanh</Link>
        </Button>
      </div>
    );
  }
  return children;
}
