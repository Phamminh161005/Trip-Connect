import { Suspense, type ReactNode } from "react";
import { RequireAuth } from "@/components/auth/RequireAuth";
import { AdminSidebar } from "./AdminSidebar";
import { AdminTopBar } from "./AdminTopBar";

/** Trang quản trị — chỉ dành cho ADMIN (Backend cũng chặn /api/admin/** theo vai trò). Bố cục riêng, không dùng header trang khách. */
export default function AdminLayout({ children }: { children: ReactNode }) {
  return (
    <Suspense>
      <RequireAuth roles={["ADMIN"]}>
        <div className="flex min-h-svh bg-muted/30">
          <AdminSidebar />
          <div className="flex min-w-0 flex-1 flex-col">
            <AdminTopBar />
            <main className="mx-auto w-full max-w-6xl flex-1 px-4 py-8 sm:px-6">{children}</main>
          </div>
        </div>
      </RequireAuth>
    </Suspense>
  );
}
