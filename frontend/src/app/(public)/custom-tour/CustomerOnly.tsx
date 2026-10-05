"use client";

import type { ReactNode } from "react";
import Link from "next/link";
import { Sparkles } from "lucide-react";
import { Button } from "@/components/ui/button";
import { useAuth } from "@/lib/auth/AuthProvider";
import { ADMIN_AREA_NAME, AGENT_AREA_NAME } from "@/lib/constants";

/** Tour theo yêu cầu chỉ dành cho khách hàng; Agent / Admin thấy lời giải thích thay cho form. */
export function CustomerOnly({ children }: { children: ReactNode }) {
  const { user } = useAuth();
  if (!user || user.role === "CUSTOMER") return <>{children}</>;

  const agent = user.role === "AGENT";
  return (
    <div className="mx-auto flex max-w-lg flex-1 flex-col items-center justify-center gap-4 px-4 py-24 text-center">
      <Sparkles className="size-12 text-primary" />
      <h1 className="text-2xl font-bold">Tour theo yêu cầu dành cho khách hàng</h1>
      <p className="text-muted-foreground">
        {agent
          ? "Tài khoản đối tác không gửi được yêu cầu thiết kế tour. Bạn sẽ nhận các yêu cầu phù hợp từ khách trong mục Yêu cầu tư vấn."
          : "Tài khoản quản trị không gửi được yêu cầu thiết kế tour."}
      </p>
      <Button asChild className="rounded-xl">
        <Link href={agent ? "/agent/requests" : "/admin/requests"}>Về {agent ? AGENT_AREA_NAME : ADMIN_AREA_NAME}</Link>
      </Button>
    </div>
  );
}
