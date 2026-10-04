"use client";

import { useState } from "react";
import Link from "next/link";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { Bell, CheckCheck } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover";
import { Skeleton } from "@/components/ui/skeleton";
import { errorMessage } from "@/lib/api/errors";
import {
  getUnreadCount,
  listNotifications,
  markAllNotificationsRead,
  NOTIFICATIONS_KEY,
  UNREAD_COUNT_KEY,
} from "@/lib/api/notifications";
import { useAuth } from "@/lib/auth/AuthProvider";
import { cn } from "@/lib/utils";
import { NotificationRow } from "./NotificationRow";
import { useOpenNotification } from "./useOpenNotification";

/** Hỏi lại số chưa đọc mỗi 30 giây (và mỗi khi quay lại tab). */
const POLL_MS = 30_000;

/** Chuông thông báo trên header. @param inverted icon trắng (header trong suốt trên ảnh) */
export function NotificationBell({ inverted = false }: { inverted?: boolean }) {
  const { user } = useAuth();
  if (!user) return null;
  return <BellPopover inverted={inverted} />;
}

function BellPopover({ inverted }: { inverted: boolean }) {
  const [open, setOpen] = useState(false);
  const queryClient = useQueryClient();
  const unread = useQuery({
    queryKey: UNREAD_COUNT_KEY,
    queryFn: getUnreadCount,
    refetchInterval: POLL_MS,
    refetchOnWindowFocus: true,
  });
  const latest = useQuery({
    queryKey: [...NOTIFICATIONS_KEY, "latest"],
    queryFn: () => listNotifications({ size: 10 }),
    enabled: open,
    staleTime: 0,
  });
  const openNotification = useOpenNotification(() => setOpen(false));
  const count = unread.data?.count ?? 0;

  const readAll = async () => {
    try {
      await markAllNotificationsRead();
      await queryClient.invalidateQueries({ queryKey: NOTIFICATIONS_KEY });
    } catch (error) {
      toast.error(errorMessage(error));
    }
  };

  return (
    <Popover open={open} onOpenChange={setOpen}>
      <PopoverTrigger
        aria-label={count > 0 ? `Thông báo, ${count} chưa đọc` : "Thông báo"}
        className={cn(
          "relative flex size-10 items-center justify-center rounded-full transition-colors outline-none focus-visible:ring-3 focus-visible:ring-ring/50",
          inverted ? "text-white hover:bg-white/15" : "hover:bg-accent",
        )}
      >
        <Bell className="size-5" />
        {count > 0 && (
          <span className="absolute top-1 right-1 flex h-4.5 min-w-4.5 items-center justify-center rounded-full bg-red-600 px-1 text-[10px] leading-none font-bold text-white ring-2 ring-background">
            {count > 9 ? "9+" : count}
          </span>
        )}
      </PopoverTrigger>
      <PopoverContent align="end" sideOffset={8} className="flex w-[min(92vw,380px)] flex-col gap-1 rounded-2xl p-2">
        <div className="flex items-center justify-between gap-2 px-2 py-1">
          <p className="font-semibold">Thông báo</p>
          {count > 0 && (
            <Button variant="ghost" size="sm" className="h-8 rounded-lg text-xs" onClick={readAll}>
              <CheckCheck /> Đánh dấu tất cả đã đọc
            </Button>
          )}
        </div>
        <div className="flex max-h-[min(70vh,440px)] flex-col gap-0.5 overflow-y-auto">
          {latest.isPending ? (
            Array.from({ length: 4 }, (_, i) => <Skeleton key={i} className="h-16 rounded-xl" />)
          ) : latest.isError ? (
            <p className="p-4 text-center text-sm text-destructive">{errorMessage(latest.error)}</p>
          ) : latest.data.items.length === 0 ? (
            <p className="p-8 text-center text-sm text-muted-foreground">Chưa có thông báo nào.</p>
          ) : (
            latest.data.items.map((item) => <NotificationRow key={item.id} item={item} onOpen={openNotification} />)
          )}
        </div>
        <Link
          href="/notifications"
          onClick={() => setOpen(false)}
          className="rounded-xl py-2 text-center text-sm font-semibold text-primary hover:bg-accent"
        >
          Xem tất cả thông báo
        </Link>
      </PopoverContent>
    </Popover>
  );
}
