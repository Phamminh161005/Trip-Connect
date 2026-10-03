import { Suspense, type ReactNode } from "react";
import { RequireAuth } from "@/components/auth/RequireAuth";
import { SiteHeader } from "@/components/layout/SiteHeader";
import { AgentNav } from "./AgentNav";

/** Khu vực Quản lý kinh doanh — chỉ dành cho vai trò AGENT (Backend cũng chặn /api/agent/** theo vai trò). */
export default function AgentLayout({ children }: { children: ReactNode }) {
  return (
    <div className="flex min-h-svh flex-col bg-muted/30">
      <SiteHeader />
      <Suspense>
        <RequireAuth roles={["AGENT"]}>
          <div className="mx-auto grid w-full max-w-6xl flex-1 gap-8 px-4 py-10 sm:px-6 md:grid-cols-[220px_1fr]">
            <AgentNav />
            <main className="min-w-0">{children}</main>
          </div>
        </RequireAuth>
      </Suspense>
    </div>
  );
}
