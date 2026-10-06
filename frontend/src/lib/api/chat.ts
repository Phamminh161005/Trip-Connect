import type { ChatMessage, ChatPage, ChatThread } from "@/types/chat";
import { apiRequest } from "./client";
import { query } from "./query";

/** Khách, Agent (đơn vị đang phụ trách), Admin (chỉ xem) */
export type ChatScope = "customer" | "agent" | "admin";

const base = (scope: ChatScope, requestId: number) =>
  `${scope === "customer" ? "/api" : `/api/${scope}`}/custom-requests/${requestId}/chat`;

export const chatThreadsKey = (scope: ChatScope, requestId: number) => ["chat", scope, requestId, "threads"] as const;
export const chatMessagesKey = (scope: ChatScope, requestId: number, agentId: number) =>
  ["chat", scope, requestId, "messages", agentId] as const;

export const getChatThreads = (scope: ChatScope, requestId: number) =>
  apiRequest<ChatThread[]>(`${base(scope, requestId)}/threads`);

export const getChatMessages = (
  scope: ChatScope,
  requestId: number,
  params: { agentId?: number; before?: number; after?: number; size?: number },
) => apiRequest<ChatPage>(`${base(scope, requestId)}/messages${query({ ...params, agentId: scope === "agent" ? undefined : params.agentId })}`);

export function sendChatMessage(scope: Exclude<ChatScope, "admin">, requestId: number, body: string, images: File[], clientId: string) {
  const form = new FormData();
  if (body) form.append("body", body);
  images.forEach((file) => form.append("images", file));
  form.append("clientId", clientId);
  return apiRequest<ChatMessage>(`${base(scope, requestId)}/messages`, { method: "POST", body: form });
}

export const markChatRead = (scope: Exclude<ChatScope, "admin">, requestId: number, agentId: number, lastMessageId: number) =>
  apiRequest<void>(`${base(scope, requestId)}/read`, {
    method: "POST",
    body: { agentId: scope === "customer" ? agentId : null, lastMessageId },
  });
