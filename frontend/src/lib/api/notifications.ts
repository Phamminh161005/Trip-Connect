import type { NotificationPage } from "@/types/notification";
import { apiRequest } from "./client";
import { query } from "./query";

export const NOTIFICATIONS_KEY = ["notifications"] as const;
export const UNREAD_COUNT_KEY = [...NOTIFICATIONS_KEY, "unread-count"] as const;

/** Mới nhất trước; trang sau: truyền nextCursor của trang trước vào before. */
export const listNotifications = (params: { before?: number; size?: number; unread?: boolean }) =>
  apiRequest<NotificationPage>(`/api/notifications${query({ ...params })}`);

export const getUnreadCount = () => apiRequest<{ count: number }>("/api/notifications/unread-count");

export const markNotificationRead = (id: number) => apiRequest<void>(`/api/notifications/${id}/read`, { method: "POST" });

export const markAllNotificationsRead = () => apiRequest<void>("/api/notifications/read-all", { method: "POST" });
