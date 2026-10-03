"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { CircleUserRound, KeyRound, LayoutDashboard, LogOut, Menu, Store, UserRound } from "lucide-react";
import { ADMIN_AREA_NAME, AGENT_AREA_NAME } from "@/lib/constants";
import { toast } from "sonner";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Skeleton } from "@/components/ui/skeleton";
import { useAuth } from "@/lib/auth/AuthProvider";

function initials(fullName: string | null | undefined): string {
  if (!fullName) return "?";
  const words = fullName.trim().split(/\s+/);
  // Tên người Việt: lấy chữ cái đầu của tên (từ cuối cùng)
  return words[words.length - 1].charAt(0).toUpperCase();
}

const triggerClass =
  "flex items-center gap-2 rounded-full border bg-background py-1.5 pr-1.5 pl-3 shadow-xs transition-shadow outline-none hover:shadow-md focus-visible:ring-3 focus-visible:ring-ring/50";

export function UserMenu() {
  const { status, user, signOut } = useAuth();
  const router = useRouter();

  if (status === "loading") {
    return <Skeleton className="h-10 w-20 rounded-full" />;
  }

  if (!user) {
    return (
      <DropdownMenu>
        <DropdownMenuTrigger className={triggerClass} aria-label="Mở menu tài khoản">
          <Menu className="size-4" />
          <CircleUserRound className="size-7 text-muted-foreground" />
        </DropdownMenuTrigger>
        <DropdownMenuContent align="end" className="w-56">
          <DropdownMenuItem asChild className="font-semibold">
            <Link href="/login">Đăng nhập</Link>
          </DropdownMenuItem>
          <DropdownMenuItem asChild>
            <Link href="/register">Đăng ký</Link>
          </DropdownMenuItem>
          {/* Màn hình rộng đã có link "Trở thành đối tác" ngay trên thanh header -> chỉ hiện ở điện thoại */}
          <DropdownMenuSeparator className="sm:hidden" />
          <DropdownMenuItem asChild className="sm:hidden">
            <Link href="/register?type=agent">Trở thành đối tác</Link>
          </DropdownMenuItem>
        </DropdownMenuContent>
      </DropdownMenu>
    );
  }

  const handleSignOut = async () => {
    await signOut();
    toast.success("Đã đăng xuất");
    router.push("/");
  };

  return (
    <DropdownMenu>
      <DropdownMenuTrigger className={triggerClass} aria-label="Mở menu tài khoản">
        <Menu className="size-4" />
        <Avatar className="size-7">
          <AvatarFallback className="bg-primary text-xs font-semibold text-primary-foreground">
            {initials(user.fullName)}
          </AvatarFallback>
        </Avatar>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end" className="w-64">
        <DropdownMenuLabel className="flex flex-col gap-0.5">
          <span className="truncate font-semibold">{user.fullName}</span>
          <span className="truncate text-xs font-normal text-muted-foreground">{user.email}</span>
        </DropdownMenuLabel>
        <DropdownMenuSeparator />
        {user.role === "ADMIN" && (
          <>
            <DropdownMenuItem asChild>
              <Link href="/admin">
                <LayoutDashboard /> {ADMIN_AREA_NAME}
              </Link>
            </DropdownMenuItem>
            <DropdownMenuSeparator />
          </>
        )}
        {user.role === "AGENT" && (
          <>
            {/* Một lối vào duy nhất; các trang con (Hồ sơ kinh doanh...) nằm ở menu bên trái của khu vực này */}
            <DropdownMenuItem asChild>
              <Link href="/agent">
                <Store /> {AGENT_AREA_NAME}
              </Link>
            </DropdownMenuItem>
            <DropdownMenuSeparator />
          </>
        )}
        <DropdownMenuItem asChild>
          <Link href="/account">
            <UserRound /> Thông tin cá nhân
          </Link>
        </DropdownMenuItem>
        <DropdownMenuItem asChild>
          <Link href="/account/change-password">
            <KeyRound /> Đổi mật khẩu
          </Link>
        </DropdownMenuItem>
        <DropdownMenuSeparator />
        <DropdownMenuItem onSelect={handleSignOut}>
          <LogOut /> Đăng xuất
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  );
}
