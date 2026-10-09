import type {
  AssistantStatus,
  ConversationDetail,
  ConversationSummary,
  CustomRequestDraft,
  HistoryMessage,
} from "@/types/assistant";
import type { TourCard } from "@/types/search";
import { getAccessToken } from "@/lib/tokenStore";
import { visitorHeaders } from "@/lib/visitor";
import { API_URL, apiRequest, refreshAccessToken } from "./client";
import { NETWORK_ERROR, toApiError } from "./errors";

export const getAssistantStatus = () =>
  apiRequest<AssistantStatus>("/api/assistant/status");

export const getConversations = () => apiRequest<ConversationSummary[]>("/api/assistant/conversations");

export const getConversation = (id: number) => apiRequest<ConversationDetail>(`/api/assistant/conversations/${id}`);

export const deleteConversation = (id: number) =>
  apiRequest<void>(`/api/assistant/conversations/${id}`, { method: "DELETE" });

export interface ChatHandlers {
  onDelta: (text: string) => void;
  onTours: (tours: TourCard[]) => void;
  onCustomRequest: (draft: CustomRequestDraft) => void;
  onDone: (conversationId: number | null) => void;
  /** Lỗi phía trợ lý (mô hình quá tải...) — gửi trong luồng trả lời. */
  onError: (message: string) => void;
}

interface ChatInput {
  message: string;
  conversationId?: number | null;
  /** Chỉ dùng khi chưa đăng nhập */
  history?: HistoryMessage[];
}

/**
 * Hỏi trợ lý, nhận câu trả lời dạng Server-Sent Events (chữ đến dần).
 * Dùng fetch + đọc luồng thay cho EventSource vì cần POST và header Authorization.
 * Lỗi HTTP (429, 404...) ném ApiError trước khi luồng bắt đầu.
 */
export async function streamChat(input: ChatInput, handlers: ChatHandlers, signal?: AbortSignal): Promise<void> {
  const doFetch = (token: string | null) =>
    fetch(`${API_URL}/api/assistant/chat`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Accept: "text/event-stream",
        ...visitorHeaders(),
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
      body: JSON.stringify(input),
      signal,
    });

  let res: Response;
  try {
    const token = getAccessToken();
    res = await doFetch(token);
    if (res.status === 401 && token) {
      res = await doFetch(await refreshAccessToken());
    }
  } catch (error) {
    if (error instanceof DOMException && error.name === "AbortError") throw error;
    throw NETWORK_ERROR;
  }
  if (!res.ok || !res.body) throw await toApiError(res);

  const reader = res.body.pipeThrough(new TextDecoderStream()).getReader();
  let buffer = "";
  let finished = false;
  try {
    while (true) {
      const { value, done } = await reader.read();
      if (done) break;
      buffer += value;
      // Mỗi sự kiện SSE kết thúc bằng một dòng trống
      let boundary: number;
      while ((boundary = buffer.search(/\r?\n\r?\n/)) >= 0) {
        const block = buffer.slice(0, boundary);
        buffer = buffer.slice(boundary).replace(/^\r?\n\r?\n/, "");
        if (dispatch(block, handlers)) finished = true;
      }
    }
  } catch (error) {
    if (error instanceof DOMException && error.name === "AbortError") throw error;
    throw NETWORK_ERROR;
  }
  if (!finished) handlers.onError("Mất kết nối với trợ lý, bạn thử lại nhé.");
}

/** Xử lý một sự kiện SSE. Trả về true nếu là sự kiện kết thúc (done / error). */
function dispatch(block: string, handlers: ChatHandlers): boolean {
  let event = "message";
  const data: string[] = [];
  for (const line of block.split(/\r?\n/)) {
    if (line.startsWith("event:")) event = line.slice(6).trim();
    else if (line.startsWith("data:")) data.push(line.slice(5).replace(/^ /, ""));
  }
  if (data.length === 0) return false;
  const payload = JSON.parse(data.join("\n"));
  switch (event) {
    case "delta":
      handlers.onDelta(payload.text);
      return false;
    case "tours":
      handlers.onTours(payload);
      return false;
    case "customRequest":
      handlers.onCustomRequest(payload);
      return false;
    case "done":
      handlers.onDone(payload.conversationId ?? null);
      return true;
    case "error":
      handlers.onError(payload.message);
      return true;
    default:
      return false;
  }
}
