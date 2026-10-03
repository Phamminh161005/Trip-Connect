"use client";

import { useEffect, type ReactNode } from "react";
import Link from "next/link";
import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { ShieldAlert } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Spinner } from "@/components/ui/spinner";
import { useAuth } from "@/lib/auth/AuthProvider";
import { loginUrl } from "@/lib/auth/redirect";
import type { UserRole } from "@/types/auth";

/** Chặn trang cần đăng nhập phía client (proxy.ts chặn trước nếu không có cookie). */
export function RequireAuth({ roles, children }: { roles?: UserRole[]; children: ReactNode }) {
  const { status, user } = useAuth();
  const router = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();

  useEffect(() => {
    if (status === "guest") {
      const query = searchParams.toString();
      router.replace(loginUrl(query ? `${pathname}?${query}` : pathname));
    }
  }, [status, router, pathname, searchParams]);

  if (status !== "authenticated" || !user) {
    return (
      <div className="flex flex-1 items-center justify-center py-24" role="status">
        <Spinner className="size-6 text-primary" />
        <span className="sr-only">Đang tải...</span>
      </div>
    );
  }

  if (roles && !roles.includes(user.role)) {
    return <Forbidden />;
  }

  return <>{children}</>;
}

export function Forbidden() {
  return (
    <div className="mx-auto flex max-w-md flex-1 flex-col items-center justify-center gap-4 px-4 py-24 text-center">
      <ShieldAlert className="size-12 text-muted-foreground" />
      <h1 className="text-2xl font-bold">Bạn không có quyền truy cập</h1>
      <p className="text-muted-foreground">Trang này dành cho vai trò khác. Nếu bạn cho rằng đây là nhầm lẫn, hãy liên hệ quản trị viên.</p>
      <Button asChild className="rounded-xl">
        <Link href="/">Về trang chủ</Link>
      </Button>
    </div>
  );
}
