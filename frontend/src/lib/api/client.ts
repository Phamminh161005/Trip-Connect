import { getAccessToken, setAccessToken } from "@/lib/tokenStore";
import { ApiError, NETWORK_ERROR, toApiError } from "./errors";

export const API_URL = process.env.NEXT_PUBLIC_API_URL;

// ===================== Refresh access token =====================

// Refresh token chỉ dùng được 1 lần (Backend coi lần dùng thứ 2 là bị đánh cắp và hủy mọi phiên),
// nên khi nhiều request cùng nhận 401 thì chỉ gọi refresh 1 lần, các request khác chờ chung kết quả.
let refreshPromise: Promise<string | null> | null = null;

/** Xin access token mới qua route BFF (refresh token nằm trong cookie HttpOnly). null = hết phiên. */
export function refreshAccessToken(): Promise<string | null> {
  if (!refreshPromise) {
    refreshPromise = (async () => {
      try {
        const res = await fetch("/api/auth/refresh", { method: "POST" });
        if (!res.ok) {
          setAccessToken(null);
          return null;
        }
        const data: { accessToken: string } = await res.json();
        setAccessToken(data.accessToken);
        return data.accessToken;
      } catch {
        return null;
      }
    })().finally(() => {
      refreshPromise = null;
    });
  }
  return refreshPromise;
}

// ===================== Thông báo hết phiên =====================

type SessionExpiredListener = () => void;
const sessionExpiredListeners = new Set<SessionExpiredListener>();

/** AuthProvider đăng ký để biết khi nào phiên hết hạn hẳn (refresh cũng thất bại). */
export function onSessionExpired(listener: SessionExpiredListener): () => void {
  sessionExpiredListeners.add(listener);
  return () => sessionExpiredListeners.delete(listener);
}

// ===================== Gọi API =====================

interface RequestOptions {
  method?: "GET" | "POST" | "PUT" | "PATCH" | "DELETE";
  /** Object sẽ được gửi dạng JSON; FormData được gửi dạng multipart. */
  body?: unknown;
  /** Gắn access token và tự refresh khi gặp 401. Mặc định: true. */
  auth?: boolean;
  /** Header bổ sung (vd X-Visitor-Id cho lịch sử tìm kiếm). */
  headers?: Record<string, string>;
  signal?: AbortSignal;
}

/** Gọi trực tiếp Backend (Spring Boot). Trả về dữ liệu đã parse, hoặc ném ApiError. */
export async function apiRequest<T>(path: string, options: RequestOptions = {}): Promise<T> {
  return send<T>(`${API_URL}${path}`, options);
}

/** Gọi route BFF nội bộ của Next.js (/api/auth/...) — nơi xử lý cookie refresh token. */
export async function bffRequest<T>(path: string, options: Omit<RequestOptions, "auth"> = {}): Promise<T> {
  return send<T>(path, { ...options, auth: false });
}

async function send<T>(url: string, options: RequestOptions): Promise<T> {
  const { method = "GET", body, auth = true, signal, headers: extraHeaders } = options;

  const doFetch = (token: string | null) => {
    const headers: Record<string, string> = { ...extraHeaders };
    let payload: BodyInit | undefined;
    if (body instanceof FormData) {
      payload = body; // trình duyệt tự đặt Content-Type multipart kèm boundary
    } else if (body !== undefined) {
      headers["Content-Type"] = "application/json";
      payload = JSON.stringify(body);
    }
    if (auth && token) headers["Authorization"] = `Bearer ${token}`;
    return fetch(url, { method, headers, body: payload, signal });
  };

  let res: Response;
  try {
    res = await doFetch(auth ? getAccessToken() : null);

    if (res.status === 401 && auth) {
      const newToken = await refreshAccessToken();
      if (!newToken) {
        sessionExpiredListeners.forEach((listener) => listener());
        throw await toApiError(res);
      }
      res = await doFetch(newToken);
    }
  } catch (error) {
    if (error instanceof ApiError) throw error;
    if (error instanceof DOMException && error.name === "AbortError") throw error;
    throw NETWORK_ERROR;
  }

  if (!res.ok) throw await toApiError(res);
  if (res.status === 204) return undefined as T;

  const text = await res.text();
  return (text ? JSON.parse(text) : undefined) as T;
}
