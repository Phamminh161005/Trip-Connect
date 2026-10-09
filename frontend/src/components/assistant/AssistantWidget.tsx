"use client";

import { useEffect, useRef, useState, type KeyboardEvent } from "react";
import { usePathname } from "next/navigation";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { ArrowLeft, Bot, History, Loader2, Plus, Send, Sparkles, Square, Trash2, X } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { Textarea } from "@/components/ui/textarea";
import {
  deleteConversation,
  getAssistantStatus,
  getConversation,
  getConversations,
  streamChat,
} from "@/lib/api/assistant";
import { errorMessage } from "@/lib/api/errors";
import { useAuth } from "@/lib/auth/AuthProvider";
import { cn } from "@/lib/utils";
import type { AssistantMessage, HistoryMessage } from "@/types/assistant";
import type { TourCard } from "@/types/search";
import { AssistantTourCard, CustomRequestCta } from "./AssistantAttachments";
import { AssistantText } from "./AssistantText";

// Khớp AssistantDtos của Backend
const MAX_LENGTH = 1000;
const GUEST_HISTORY = 10;

/** Trang không hiện trợ lý: đăng nhập / đăng ký và khu vực của đối tác, quản trị. */
const HIDDEN_PREFIXES = ["/login", "/register", "/forgot-password", "/reset-password", "/unlock-account", "/verify-email", "/agent", "/admin"];

const SUGGESTIONS = [
  "Gợi ý tour biển 3 ngày dưới 5 triệu",
  "Gia đình có trẻ nhỏ nên đi đâu cuối tháng này?",
  "Hủy tour thì được hoàn tiền thế nào?",
  "Mình muốn tour riêng đi Đà Lạt cho 6 người",
];

/** Nút trợ lý AI nổi ở góc màn hình, cho khách vãng lai và khách hàng. */
export function AssistantWidget() {
  const { status: authStatus, user } = useAuth();
  const pathname = usePathname();
  const [open, setOpen] = useState(false);
  const [everOpened, setEverOpened] = useState(false);

  const signedIn = authStatus === "authenticated" && !!user;
  const status = useQuery({
    queryKey: ["assistant-status", signedIn],
    queryFn: getAssistantStatus,
    enabled: authStatus !== "loading",
    staleTime: 5 * 60_000,
  });

  const hidden =
    HIDDEN_PREFIXES.some((p) => pathname === p || pathname.startsWith(`${p}/`)) ||
    (user !== null && user.role !== "CUSTOMER") ||
    !status.data?.enabled;
  if (hidden) return null;

  const toggle = () => {
    setOpen((o) => !o);
    setEverOpened(true);
  };

  return (
    <>
      {everOpened && (
        <AssistantPanel
          // Đổi tài khoản -> bắt đầu lại với lịch sử của tài khoản đó
          key={user ? `user-${user.userId}` : "guest"}
          storageKey={user ? `tc_assistant_${user.userId}` : "tc_assistant_guest"}
          signedIn={signedIn}
          open={open}
          onClose={() => setOpen(false)}
        />
      )}
      <Button
        onClick={toggle}
        size="icon"
        aria-label={open ? "Đóng trợ lý" : "Mở trợ lý du lịch AI"}
        aria-expanded={open}
        className={cn(
          "fixed right-4 bottom-4 z-50 size-14 rounded-full shadow-lg sm:right-6 sm:bottom-6",
          open && "max-sm:hidden",
        )}
      >
        {open ? <X className="size-6" /> : <Sparkles className="size-6" />}
      </Button>
    </>
  );
}

interface SavedState {
  conversationId: number | null;
  messages: AssistantMessage[];
}

function loadState(key: string): SavedState {
  try {
    const raw = window.sessionStorage.getItem(key);
    if (raw) {
      const saved = JSON.parse(raw) as SavedState;
      // Tin đang nhận dở lúc tải lại trang thì coi như lỗi
      return {
        conversationId: saved.conversationId,
        messages: saved.messages.map((m) => (m.streaming ? { ...m, streaming: false, failed: !m.content } : m)),
      };
    }
  } catch {
    // trình duyệt chặn lưu trữ / dữ liệu hỏng
  }
  return { conversationId: null, messages: [] };
}

let keySeq = 0;
const newKey = () => `local-${Date.now()}-${++keySeq}`;

function AssistantPanel({
  storageKey,
  signedIn,
  open,
  onClose,
}: {
  storageKey: string;
  signedIn: boolean;
  open: boolean;
  onClose: () => void;
}) {
  const queryClient = useQueryClient();
  const [initial] = useState(() => loadState(storageKey));
  const [messages, setMessages] = useState<AssistantMessage[]>(initial.messages);
  const [conversationId, setConversationId] = useState<number | null>(initial.conversationId);
  const [input, setInput] = useState("");
  const [busy, setBusy] = useState(false);
  const [view, setView] = useState<"chat" | "history">("chat");
  const abortRef = useRef<AbortController | null>(null);
  const bottomRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLTextAreaElement>(null);

  // Giữ cuộc trò chuyện khi chuyển trang / tải lại (trong phiên trình duyệt)
  useEffect(() => {
    try {
      window.sessionStorage.setItem(storageKey, JSON.stringify({ conversationId, messages }));
    } catch {
      // bỏ qua
    }
  }, [storageKey, conversationId, messages]);

  useEffect(() => {
    if (open && view === "chat") bottomRef.current?.scrollIntoView({ block: "end" });
  }, [messages, open, view]);

  useEffect(() => {
    if (open && view === "chat") inputRef.current?.focus();
  }, [open, view]);

  // Đóng component (đổi tài khoản) thì dừng câu trả lời đang nhận
  useEffect(() => () => abortRef.current?.abort(), []);

  const update = (key: string, patch: (m: AssistantMessage) => Partial<AssistantMessage>) =>
    setMessages((list) => list.map((m) => (m.key === key ? { ...m, ...patch(m) } : m)));

  const send = async (raw: string) => {
    const text = raw.trim();
    if (!text || busy) return;
    if (text.length > MAX_LENGTH) {
      toast.error(`Câu hỏi tối đa ${MAX_LENGTH} ký tự`);
      return;
    }

    const history: HistoryMessage[] = messages
      .filter((m) => !m.failed && m.content)
      .slice(-GUEST_HISTORY)
      .map((m) => ({ fromUser: m.fromUser, content: m.content }));
    const botKey = newKey();
    setMessages((list) => [
      ...list,
      { key: newKey(), fromUser: true, content: text, attachments: null },
      { key: botKey, fromUser: false, content: "", attachments: null, streaming: true },
    ]);
    setInput("");
    setBusy(true);

    const controller = new AbortController();
    abortRef.current = controller;
    try {
      await streamChat(
        signedIn ? { message: text, conversationId } : { message: text, history },
        {
          onDelta: (delta) => update(botKey, (m) => ({ content: m.content + delta })),
          onTours: (tours) =>
            update(botKey, (m) => ({ attachments: { tours, customRequest: m.attachments?.customRequest ?? null } })),
          onCustomRequest: (draft) =>
            update(botKey, (m) => ({ attachments: { tours: m.attachments?.tours ?? [], customRequest: draft } })),
          onDone: (id) => {
            update(botKey, () => ({ streaming: false }));
            if (id !== null) {
              setConversationId(id);
              queryClient.invalidateQueries({ queryKey: ["assistant-conversations"] });
            }
          },
          onError: (message) =>
            update(botKey, (m) => ({ streaming: false, failed: true, content: m.content || message })),
        },
        controller.signal,
      );
    } catch (error) {
      if (error instanceof DOMException && error.name === "AbortError") {
        update(botKey, (m) => ({ streaming: false, failed: !m.content, content: m.content || "Đã dừng." }));
      } else {
        update(botKey, () => ({ streaming: false, failed: true, content: errorMessage(error) }));
      }
    } finally {
      abortRef.current = null;
      setBusy(false);
    }
  };

  const stop = () => abortRef.current?.abort();

  const startNew = () => {
    stop();
    setMessages([]);
    setConversationId(null);
    setView("chat");
  };

  const openConversation = async (id: number) => {
    try {
      const detail = await queryClient.fetchQuery({
        queryKey: ["assistant-conversation", id],
        queryFn: () => getConversation(id),
        staleTime: 0,
      });
      stop();
      setMessages(
        detail.messages.map((m) => ({
          key: `db-${m.id}`,
          fromUser: m.fromUser,
          content: m.content,
          attachments: m.attachments,
        })),
      );
      setConversationId(detail.id);
      setView("chat");
    } catch (error) {
      toast.error(errorMessage(error));
    }
  };

  const onKeyDown = (e: KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === "Enter" && !e.shiftKey && !e.nativeEvent.isComposing) {
      e.preventDefault();
      void send(input);
    }
  };

  return (
    <section
      aria-label="Trợ lý du lịch AI"
      className={cn(
        "fixed z-50 flex flex-col overflow-hidden border bg-background shadow-2xl",
        "inset-0 sm:inset-auto sm:right-6 sm:bottom-24 sm:h-[min(640px,calc(100svh-8rem))] sm:w-[400px] sm:rounded-2xl",
        !open && "hidden",
      )}
    >
      <header className="flex items-center gap-2 border-b bg-primary px-3 py-2.5 text-primary-foreground">
        {view === "history" ? (
          <Button variant="ghost" size="icon-sm" onClick={() => setView("chat")} aria-label="Quay lại" className="hover:bg-white/15 hover:text-primary-foreground">
            <ArrowLeft />
          </Button>
        ) : (
          <span className="flex size-8 items-center justify-center rounded-full bg-white/15">
            <Bot className="size-4.5" />
          </span>
        )}
        <div className="min-w-0 flex-1">
          <p className="text-sm font-semibold">{view === "history" ? "Cuộc trò chuyện trước" : "Trợ lý TripConnect"}</p>
          {view === "chat" && <p className="text-xs opacity-80">Tìm tour, giải đáp quy định</p>}
        </div>
        {signedIn && view === "chat" && (
          <Button variant="ghost" size="icon-sm" onClick={() => setView("history")} aria-label="Lịch sử trò chuyện" title="Lịch sử trò chuyện" className="hover:bg-white/15 hover:text-primary-foreground">
            <History />
          </Button>
        )}
        <Button variant="ghost" size="icon-sm" onClick={startNew} aria-label="Cuộc trò chuyện mới" title="Cuộc trò chuyện mới" className="hover:bg-white/15 hover:text-primary-foreground">
          <Plus />
        </Button>
        <Button variant="ghost" size="icon-sm" onClick={onClose} aria-label="Đóng trợ lý" className="hover:bg-white/15 hover:text-primary-foreground">
          <X />
        </Button>
      </header>

      {view === "history" ? (
        <ConversationList activeId={conversationId} onOpen={openConversation} onDeleted={(id) => id === conversationId && startNew()} />
      ) : (
        <>
          <div className="flex-1 overflow-y-auto px-3 py-4">
            {messages.length === 0 ? (
              <Welcome onPick={(q) => void send(q)} />
            ) : (
              <div className="flex flex-col gap-4">
                {messages.map((m) => (
                  <MessageBubble key={m.key} message={m} onNavigate={() => window.innerWidth < 640 && onClose()} />
                ))}
              </div>
            )}
            <div ref={bottomRef} />
          </div>

          <div className="border-t p-3">
            <div className="flex items-end gap-2">
              <Textarea
                ref={inputRef}
                value={input}
                onChange={(e) => setInput(e.target.value)}
                onKeyDown={onKeyDown}
                rows={1}
                maxLength={MAX_LENGTH}
                placeholder="Bạn muốn đi đâu, khi nào?"
                aria-label="Câu hỏi cho trợ lý"
                className="max-h-32 min-h-10 resize-none rounded-xl"
              />
              {busy ? (
                <Button size="icon" variant="outline" onClick={stop} aria-label="Dừng trả lời" className="shrink-0 rounded-xl">
                  <Square className="size-4" />
                </Button>
              ) : (
                <Button size="icon" onClick={() => void send(input)} disabled={!input.trim()} aria-label="Gửi" className="shrink-0 rounded-xl">
                  <Send className="size-4" />
                </Button>
              )}
            </div>
            <p className="mt-2 text-center text-[11px] text-muted-foreground">
              Trợ lý AI có thể nhầm lẫn — hãy kiểm tra lại giá và lịch trên trang tour trước khi đặt.
            </p>
          </div>
        </>
      )}
    </section>
  );
}

function Welcome({ onPick }: { onPick: (question: string) => void }) {
  return (
    <div className="flex flex-col gap-4 py-4 text-center">
      <span className="mx-auto flex size-12 items-center justify-center rounded-full bg-primary/10 text-primary">
        <Sparkles className="size-6" />
      </span>
      <div className="flex flex-col gap-1">
        <p className="font-semibold">Xin chào! Mình có thể giúp gì cho chuyến đi của bạn?</p>
        <p className="text-sm text-muted-foreground">
          Hãy kể điểm đến, thời gian, số người và ngân sách — mình sẽ tìm tour phù hợp trên TripConnect.
        </p>
      </div>
      <div className="flex flex-col gap-2">
        {SUGGESTIONS.map((s) => (
          <button
            key={s}
            type="button"
            onClick={() => onPick(s)}
            className="rounded-xl border px-3 py-2 text-left text-sm transition-colors hover:bg-muted"
          >
            {s}
          </button>
        ))}
      </div>
    </div>
  );
}

function MessageBubble({ message, onNavigate }: { message: AssistantMessage; onNavigate: () => void }) {
  if (message.fromUser) {
    return (
      <div className="ml-10 self-end rounded-2xl rounded-br-md bg-primary px-3.5 py-2 text-sm whitespace-pre-wrap text-primary-foreground">
        {message.content}
      </div>
    );
  }

  const tours = new Map<number, TourCard>((message.attachments?.tours ?? []).map((t) => [t.id, t]));
  return (
    <div className="mr-6 flex flex-col gap-2">
      <div
        className={cn(
          "rounded-2xl rounded-bl-md px-3.5 py-2.5",
          message.failed ? "bg-destructive/10 text-destructive" : "bg-muted",
        )}
      >
        {message.content ? (
          <AssistantText text={message.content} tours={tours} />
        ) : (
          <span className="flex items-center gap-2 text-sm text-muted-foreground">
            <Loader2 className="size-4 animate-spin" /> Đang tìm thông tin...
          </span>
        )}
      </div>
      {tours.size > 0 && (
        <div className="flex flex-col gap-2">
          {[...tours.values()].map((t) => (
            <AssistantTourCard key={t.id} tour={t} onNavigate={onNavigate} />
          ))}
        </div>
      )}
      {message.attachments?.customRequest && (
        <CustomRequestCta draft={message.attachments.customRequest} onNavigate={onNavigate} />
      )}
    </div>
  );
}

function ConversationList({
  activeId,
  onOpen,
  onDeleted,
}: {
  activeId: number | null;
  onOpen: (id: number) => void;
  onDeleted: (id: number) => void;
}) {
  const queryClient = useQueryClient();
  const list = useQuery({ queryKey: ["assistant-conversations"], queryFn: getConversations });

  const remove = async (id: number) => {
    try {
      await deleteConversation(id);
      queryClient.invalidateQueries({ queryKey: ["assistant-conversations"] });
      onDeleted(id);
    } catch (error) {
      toast.error(errorMessage(error));
    }
  };

  if (list.isPending) {
    return (
      <div className="flex flex-col gap-2 p-3">
        {Array.from({ length: 4 }, (_, i) => (
          <Skeleton key={i} className="h-12 rounded-xl" />
        ))}
      </div>
    );
  }
  if (list.isError) {
    return <p className="p-6 text-center text-sm text-destructive">{errorMessage(list.error)}</p>;
  }
  if (list.data.length === 0) {
    return <p className="p-6 text-center text-sm text-muted-foreground">Chưa có cuộc trò chuyện nào.</p>;
  }
  return (
    <ul className="flex-1 overflow-y-auto p-2">
      {list.data.map((c) => (
        <li key={c.id} className="group flex items-center gap-1">
          <button
            type="button"
            onClick={() => onOpen(c.id)}
            className={cn(
              "min-w-0 flex-1 rounded-xl px-3 py-2.5 text-left transition-colors hover:bg-muted",
              c.id === activeId && "bg-muted",
            )}
          >
            <p className="truncate text-sm font-medium">{c.title}</p>
            <p className="text-xs text-muted-foreground">{new Date(c.updatedAt).toLocaleString("vi-VN")}</p>
          </button>
          <Button
            variant="ghost"
            size="icon-sm"
            onClick={() => void remove(c.id)}
            aria-label="Xóa cuộc trò chuyện"
            className="text-muted-foreground opacity-0 group-hover:opacity-100 focus-visible:opacity-100 max-sm:opacity-100"
          >
            <Trash2 />
          </Button>
        </li>
      ))}
    </ul>
  );
}
