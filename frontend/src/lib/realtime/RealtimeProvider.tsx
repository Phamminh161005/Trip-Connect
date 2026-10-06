"use client";

import { createContext, useContext, useEffect, useRef, useState, type ReactNode } from "react";
import { Client } from "@stomp/stompjs";
import { useQueryClient, type Query } from "@tanstack/react-query";
import { API_URL, refreshAccessToken } from "@/lib/api/client";
import { NOTIFICATIONS_KEY } from "@/lib/api/notifications";
import { useAuth } from "@/lib/auth/AuthProvider";
import { getAccessToken } from "@/lib/tokenStore";
import type { RealtimeEvent } from "@/types/chat";

/** ws://localhost:8080/ws (https -> wss) */
const WS_URL = API_URL ? `${API_URL.replace(/^http/, "ws").replace(/\/+$/, "")}/ws` : null;
const RECONNECT_MS = 3_000;
const HEARTBEAT_MS = 10_000;

type Listener = (event: RealtimeEvent) => void;

interface RealtimeContextValue {
  connected: boolean;
  /** Nhận mọi sự kiện đẩy xuống; trả về hàm hủy đăng ký. */
  subscribe: (listener: Listener) => () => void;
  /** Gọi lại mỗi lần nối lại sau khi bị đứt (để tải bù dữ liệu bị lỡ). */
  onReconnect: (listener: () => void) => () => void;
}

const RealtimeContext = createContext<RealtimeContextValue | null>(null);

/** Thời điểm hết hạn (ms) đọc từ phần payload của JWT; không đọc được thì coi như đã hết hạn. */
function tokenExpiresAt(token: string): number {
  try {
    const payload = JSON.parse(atob(token.split(".")[1].replace(/-/g, "+").replace(/_/g, "/")));
    return typeof payload.exp === "number" ? payload.exp * 1000 : 0;
  } catch {
    return 0;
  }
}

/** Danh sách yêu cầu (không phải trang chi tiết) — cập nhật số tin chưa đọc khi có tin mới. */
const isRequestList = (query: Query) =>
  query.queryKey[1] === "custom-requests" && typeof query.queryKey[2] !== "number";

/**
 * Một kết nối STOMP cho mỗi tab khi đã đăng nhập. Tự nối lại khi đứt; trước mỗi lần kết nối lấy access token
 * còn hạn (sắp hết thì xin token mới). Chuông thông báo và danh sách yêu cầu được làm mới ngay khi có sự kiện.
 */
export function RealtimeProvider({ children }: { children: ReactNode }) {
  const { status, user } = useAuth();
  const queryClient = useQueryClient();
  const listeners = useRef(new Set<Listener>());
  const reconnectListeners = useRef(new Set<() => void>());
  const [connected, setConnected] = useState(false);
  const userId = user?.userId;

  useEffect(() => {
    if (status !== "authenticated" || !userId || !WS_URL) return;
    let hasConnected = false;
    // Server từ chối token (vd khóa ký đổi) -> lần sau bắt buộc xin token mới
    let forceRefresh = false;

    const client = new Client({
      brokerURL: WS_URL,
      reconnectDelay: RECONNECT_MS,
      heartbeatIncoming: HEARTBEAT_MS,
      heartbeatOutgoing: HEARTBEAT_MS,
      beforeConnect: async () => {
        let token = getAccessToken();
        if (forceRefresh || !token || tokenExpiresAt(token) - Date.now() < 30_000) {
          token = await refreshAccessToken();
          forceRefresh = false;
        }
        client.connectHeaders = token ? { Authorization: `Bearer ${token}` } : {};
      },
      onConnect: () => {
        setConnected(true);
        client.subscribe("/user/queue/events", (frame) => {
          const event = JSON.parse(frame.body) as RealtimeEvent;
          if (event.type === "NOTIFICATION") {
            void queryClient.invalidateQueries({ queryKey: NOTIFICATIONS_KEY });
          } else if (event.type === "CHAT_MESSAGE" || event.type === "CHAT_READ") {
            void queryClient.invalidateQueries({ predicate: isRequestList });
          }
          listeners.current.forEach((listener) => listener(event));
        });
        if (hasConnected) {
          // Trong lúc đứt có thể đã lỡ thông báo / tin nhắn
          void queryClient.invalidateQueries({ queryKey: NOTIFICATIONS_KEY });
          reconnectListeners.current.forEach((listener) => listener());
        }
        hasConnected = true;
      },
      onStompError: () => {
        forceRefresh = true;
      },
      onWebSocketClose: () => setConnected(false),
    });
    client.activate();

    return () => {
      setConnected(false);
      void client.deactivate();
    };
  }, [status, userId, queryClient]);

  // Hàm đăng ký giữ nguyên giữa các lần render -> effect của component dùng nó không chạy lại
  const [api] = useState(() => ({
    subscribe: (listener: Listener) => {
      listeners.current.add(listener);
      return () => {
        listeners.current.delete(listener);
      };
    },
    onReconnect: (listener: () => void) => {
      reconnectListeners.current.add(listener);
      return () => {
        reconnectListeners.current.delete(listener);
      };
    },
  }));

  return <RealtimeContext.Provider value={{ connected, ...api }}>{children}</RealtimeContext.Provider>;
}

export function useRealtime(): RealtimeContextValue {
  const context = useContext(RealtimeContext);
  if (!context) throw new Error("useRealtime phải nằm trong RealtimeProvider");
  return context;
}
