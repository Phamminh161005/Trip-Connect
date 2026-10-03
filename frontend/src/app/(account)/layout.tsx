import { Suspense, type ReactNode } from "react";
import { RequireAuth } from "@/components/auth/RequireAuth";
import { SiteHeader } from "@/components/layout/SiteHeader";
import { AccountNav } from "./AccountNav";

/** Khu vực tài khoản — bắt buộc đăng nhập. */
export default function AccountLayout({ children }: { children: ReactNode }) {
  return (
    <div className="flex min-h-svh flex-col bg-muted/30">
      <SiteHeader />
      {/* useSearchParams trong RequireAuth cần Suspense khi render tĩnh */}
      <Suspense>
        <RequireAuth>
          <div className="mx-auto grid w-full max-w-5xl flex-1 gap-8 px-4 py-10 sm:px-6 md:grid-cols-[220px_1fr]">
            <AccountNav />
            <main>{children}</main>
          </div>
        </RequireAuth>
      </Suspense>
    </div>
  );
}
