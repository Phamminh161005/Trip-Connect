"use client";

import { useState } from "react";
import { useInfiniteQuery, useQuery, useQueryClient } from "@tanstack/react-query";
import { Bell, CheckCheck } from "lucide-react";
import { toast } from "sonner";
import { NotificationRow } from "@/components/notification/NotificationRow";
import { useOpenNotification } from "@/components/notification/useOpenNotification";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { Spinner } from "@/components/ui/spinner";
import { errorMessage } from "@/lib/api/errors";
import {
  getUnreadCount,
  listNotifications,
  markAllNotificationsRead,
  NOTIFICATIONS_KEY,
  UNREAD_COUNT_KEY,
} from "@/lib/api/notifications";
import { cn } from "@/lib/utils";

const PAGE_SIZE = 20;

/** Toàn bộ thông báo, mới nhất trước; "Xem thêm" lấy trang kế tiếp theo con trỏ. */
export function NotificationsView() {
  const [unreadOnly, setUnreadOnly] = useState(false);
  const queryClient = useQueryClient();
  const unread = useQuery({ queryKey: UNREAD_COUNT_KEY, queryFn: getUnreadCount });
  const list = useInfiniteQuery({
    queryKey: [...NOTIFICATIONS_KEY, "list", { unreadOnly }],
    queryFn: ({ pageParam }) => listNotifications({ before: pageParam ?? undefined, size: PAGE_SIZE, unread: unreadOnly }),
    initialPageParam: null as number | null,
    getNextPageParam: (last) => last.nextCursor,
  });
  const openNotification = useOpenNotification();
  const items = list.data?.pages.flatMap((p) => p.items) ?? [];
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
    <div className="flex flex-col gap-5">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">Thông báo</h1>
          <p className="text-sm text-muted-foreground">
            {count > 0 ? `${count} thông báo chưa đọc` : "Bạn đã đọc hết thông báo"} · thông báo đã đọc được giữ 60 ngày
          </p>
        </div>
        {count > 0 && (
          <Button variant="outline" className="rounded-xl" onClick={readAll}>
            <CheckCheck /> Đánh dấu tất cả đã đọc
          </Button>
        )}
      </div>

      <div className="flex gap-1" role="tablist" aria-label="Lọc thông báo">
        {[
          { value: false, label: "Tất cả" },
          { value: true, label: "Chưa đọc" },
        ].map((tab) => (
          <button
            key={tab.label}
            type="button"
            role="tab"
            aria-selected={unreadOnly === tab.value}
            onClick={() => setUnreadOnly(tab.value)}
            className={cn(
              "rounded-full px-4 py-1.5 text-sm font-medium transition-colors",
              unreadOnly === tab.value ? "bg-primary text-primary-foreground" : "text-muted-foreground hover:bg-accent",
            )}
          >
            {tab.label}
          </button>
        ))}
      </div>

      <Card className="rounded-2xl py-2">
        <CardContent className="flex flex-col gap-0.5 px-2">
          {list.isPending ? (
            Array.from({ length: 5 }, (_, i) => <Skeleton key={i} className="h-16 rounded-xl" />)
          ) : list.isError ? (
            <p className="p-6 text-center text-sm text-destructive">{errorMessage(list.error)}</p>
          ) : items.length === 0 ? (
            <div className="flex flex-col items-center gap-2 p-12 text-center text-muted-foreground">
              <Bell className="size-8" />
              <p>{unreadOnly ? "Không có thông báo chưa đọc." : "Chưa có thông báo nào."}</p>
            </div>
          ) : (
            items.map((item) => <NotificationRow key={item.id} item={item} onOpen={openNotification} />)
          )}
        </CardContent>
      </Card>

      {list.hasNextPage && (
        <Button
          variant="outline"
          className="self-center rounded-xl"
          disabled={list.isFetchingNextPage}
          onClick={() => list.fetchNextPage()}
        >
          {list.isFetchingNextPage && <Spinner />} Xem thêm
        </Button>
      )}
    </div>
  );
}
