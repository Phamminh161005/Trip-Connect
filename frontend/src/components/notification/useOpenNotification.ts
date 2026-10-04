"use client";

import { useRouter } from "next/navigation";
import { useQueryClient } from "@tanstack/react-query";
import { markNotificationRead, NOTIFICATIONS_KEY } from "@/lib/api/notifications";
import type { NotificationItem } from "@/types/notification";

/** Bấm một thông báo: đánh dấu đã đọc (không chờ) rồi mở trang liên quan. */
export function useOpenNotification(afterOpen?: () => void) {
  const router = useRouter();
  const queryClient = useQueryClient();

  return (item: NotificationItem) => {
    if (!item.read) {
      markNotificationRead(item.id)
        .catch(() => {})
        .finally(() => queryClient.invalidateQueries({ queryKey: NOTIFICATIONS_KEY }));
    }
    afterOpen?.();
    if (item.link) router.push(item.link);
  };
}
