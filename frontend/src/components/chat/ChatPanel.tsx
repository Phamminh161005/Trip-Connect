"use client";

import { useEffect, useLayoutEffect, useRef, useState, type KeyboardEvent } from "react";
import { useInfiniteQuery, useQuery, useQueryClient, type InfiniteData } from "@tanstack/react-query";
import { ArrowDown, ImagePlus, Loader2, Lock, MessagesSquare, RotateCcw, Send, ShieldCheck, X } from "lucide-react";
import { toast } from "sonner";
import { ReviewImages } from "@/components/review/ReviewImages";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Skeleton } from "@/components/ui/skeleton";
import { Textarea } from "@/components/ui/textarea";
import {
  chatMessagesKey,
  chatThreadsKey,
  getChatMessages,
  getChatThreads,
  markChatRead,
  sendChatMessage,
  type ChatScope,
} from "@/lib/api/chat";
import { errorMessage } from "@/lib/api/errors";
import { useAuth } from "@/lib/auth/AuthProvider";
import { useRealtime } from "@/lib/realtime/RealtimeProvider";
import { cn } from "@/lib/utils";
import type { ChatMessage, ChatPage, ChatThread } from "@/types/chat";

// Khớp ChatService của Backend
const MAX_LENGTH = 2000;
const MAX_IMAGES = 5;
const MAX_IMAGE_BYTES = 5 * 1024 * 1024;

/**
 * Khung trao đổi trong yêu cầu tour riêng. Tin mới đến qua WebSocket (RealtimeProvider); gửi tin qua API.
 * Khách thấy cả các cuộc với đơn vị trước đây (chỉ xem); Admin chỉ xem.
 */
export function ChatPanel({ scope, requestId }: { scope: ChatScope; requestId: number }) {
  const threads = useQuery({
    queryKey: chatThreadsKey(scope, requestId),
    queryFn: () => getChatThreads(scope, requestId),
    // Mở trang là lấy mới (vd Agent vừa nhận yêu cầu thì khung chat phải hiện ngay)
    staleTime: 0,
  });
  const [selected, setSelected] = useState<number | null>(null);
  const hasThreads = (threads.data?.length ?? 0) > 0;

  // Mở từ thông báo "tin nhắn mới" (link ...#chat): khung chat chỉ có sau khi tải xong nên tự cuộn tới
  useEffect(() => {
    if (hasThreads && window.location.hash === "#chat") {
      document.getElementById("chat")?.scrollIntoView({ behavior: "smooth", block: "start" });
    }
  }, [hasThreads]);

  if (threads.isPending) return <Skeleton className="h-[560px] rounded-2xl" />;
  if (threads.isError) return <p className="text-sm text-destructive">{errorMessage(threads.error)}</p>;
  if (threads.data.length === 0) return null;
  const thread = threads.data.find((t) => t.agentId === selected) ?? threads.data[0];

  const title =
    scope === "customer"
      ? `Trao đổi với ${thread.agentName}`
      : scope === "agent"
        ? `Trao đổi với khách ${thread.customerName}`
        : `Trao đổi giữa ${thread.customerName} và ${thread.agentName}`;

  return (
    <Card id="chat" className="scroll-mt-24 gap-0 overflow-hidden rounded-2xl py-0">
      <CardHeader className="flex flex-row flex-wrap items-center justify-between gap-2 border-b py-4">
        <CardTitle className="flex items-center gap-2 text-lg">
          <MessagesSquare className="size-5 text-primary" /> {title}
        </CardTitle>
        {threads.data.length > 1 && (
          <Select value={String(thread.agentId)} onValueChange={(v) => setSelected(Number(v))}>
            <SelectTrigger className="h-9 w-auto rounded-lg" aria-label="Chọn cuộc trò chuyện">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              {threads.data.map((t) => (
                <SelectItem key={t.agentId} value={String(t.agentId)}>
                  {t.agentName} {t.current ? "(đang phụ trách)" : "(trước đây)"}
                  {t.unread > 0 && ` · ${t.unread} tin mới`}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        )}
      </CardHeader>
      <CardContent className="p-0">
        <Conversation key={thread.agentId} scope={scope} requestId={requestId} thread={thread} />
      </CardContent>
    </Card>
  );
}

interface Pending {
  clientId: string;
  body: string;
  files: File[];
  previews: string[];
  failed: boolean;
}

/** Tin "của mình" nằm bên phải. Admin: tin của đơn vị bên phải. */
const isMine = (scope: ChatScope, m: ChatMessage) => (scope === "customer" ? m.fromCustomer : !m.fromCustomer);

function Conversation({ scope, requestId, thread }: { scope: ChatScope; requestId: number; thread: ChatThread }) {
  const { user } = useAuth();
  const queryClient = useQueryClient();
  const { subscribe, onReconnect } = useRealtime();
  const messagesKey = chatMessagesKey(scope, requestId, thread.agentId);
  const threadsKey = chatThreadsKey(scope, requestId);

  const history = useInfiniteQuery({
    queryKey: messagesKey,
    queryFn: ({ pageParam }) => getChatMessages(scope, requestId, { agentId: thread.agentId, before: pageParam }),
    initialPageParam: undefined as number | undefined,
    // Trang sau = các tin cũ hơn tin cũ nhất của trang vừa tải
    getNextPageParam: (last) => (last.hasMore && last.items.length > 0 ? last.items[0].id : undefined),
    staleTime: Infinity,
  });

  // pages[0] là trang mới nhất -> đảo lại để tin cũ ở trên
  const messages = dedupe([...(history.data?.pages ?? [])].reverse().flatMap((p) => p.items));
  const lastId = messages.at(-1)?.id ?? 0;
  const latestTheirs = messages.findLast((m) => !isMine(scope, m))?.id ?? 0;
  const lastMine = messages.findLast((m) => isMine(scope, m));

  const [pending, setPending] = useState<Pending[]>([]);
  const [text, setText] = useState("");
  const [files, setFiles] = useState<{ file: File; preview: string }[]>([]);
  const [atBottom, setAtBottom] = useState(true);
  const [seenLastId, setSeenLastId] = useState(0);
  const [visible, setVisible] = useState(() => typeof document === "undefined" || document.visibilityState === "visible");
  const scrollRef = useRef<HTMLDivElement>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const keepOffsetRef = useRef<number | null>(null);
  const sentReadRef = useRef(0);

  // ----- Cập nhật bộ nhớ đệm khi có tin / sự kiện mới -----

  const appendMessage = (message: ChatMessage) => {
    queryClient.setQueryData<InfiniteData<ChatPage, number | undefined>>(messagesKey, (data) => {
      if (!data || data.pages.length === 0) return data;
      if (data.pages.some((p) => p.items.some((m) => m.id === message.id))) return data;
      const [newest, ...older] = data.pages;
      const items = [...newest.items, message].sort((a, b) => a.id - b.id);
      return { ...data, pages: [{ ...newest, items }, ...older] };
    });
  };

  const updateThread = (patch: (t: ChatThread) => ChatThread) =>
    queryClient.setQueryData<ChatThread[]>(threadsKey, (list) =>
      list?.map((t) => (t.agentId === thread.agentId ? patch(t) : t)),
    );

  useEffect(
    () =>
      subscribe((event) => {
        if (event.type === "NOTIFICATION" || event.data.requestId !== requestId) return;
        if (event.data.agentId !== thread.agentId) {
          void queryClient.invalidateQueries({ queryKey: threadsKey });
          return;
        }
        if (event.type === "CHAT_MESSAGE") {
          appendMessage(event.data);
          const clientId = event.data.clientId;
          if (clientId) setPending((list) => list.filter((p) => p.clientId !== clientId));
        } else if (event.type === "CHAT_READ") {
          const { userId, lastReadMessageId } = event.data;
          updateThread((t) =>
            userId === user?.userId
              ? { ...t, myLastRead: Math.max(t.myLastRead, lastReadMessageId), unread: 0 }
              : { ...t, counterpartLastRead: Math.max(t.counterpartLastRead, lastReadMessageId) },
          );
        }
      }),
    // eslint-disable-next-line react-hooks/exhaustive-deps -- đăng ký một lần cho mỗi cuộc trò chuyện
    [subscribe, requestId, thread.agentId],
  );

  // Nối lại sau khi mất mạng: tải bù các tin bị lỡ
  useEffect(
    () =>
      onReconnect(() => {
        const data = queryClient.getQueryData<InfiniteData<ChatPage>>(messagesKey);
        const newestId = data?.pages[0]?.items.at(-1)?.id;
        if (newestId === undefined) return;
        getChatMessages(scope, requestId, { agentId: thread.agentId, after: newestId, size: 100 })
          .then((page) => page.items.forEach(appendMessage))
          .catch(() => {});
        void queryClient.invalidateQueries({ queryKey: threadsKey });
      }),
    // eslint-disable-next-line react-hooks/exhaustive-deps -- như trên
    [onReconnect, requestId, thread.agentId],
  );

  useEffect(() => {
    const onChange = () => setVisible(document.visibilityState === "visible");
    document.addEventListener("visibilitychange", onChange);
    return () => document.removeEventListener("visibilitychange", onChange);
  }, []);

  // Đang mở khung chat và tab đang hiện -> đánh dấu đã đọc tới tin mới nhất của bên kia
  useEffect(() => {
    if (scope === "admin" || !visible || latestTheirs <= thread.myLastRead || latestTheirs <= sentReadRef.current) return;
    sentReadRef.current = latestTheirs;
    markChatRead(scope, requestId, thread.agentId, latestTheirs).catch(() => {
      sentReadRef.current = 0;
    });
  }, [scope, visible, latestTheirs, thread.myLastRead, requestId, thread.agentId]);

  // ----- Cuộn -----

  useLayoutEffect(() => {
    const el = scrollRef.current;
    if (!el) return;
    if (keepOffsetRef.current !== null) {
      // Vừa tải tin cũ ở trên: giữ nguyên chỗ đang đọc
      el.scrollTop = el.scrollHeight - keepOffsetRef.current;
      keepOffsetRef.current = null;
    } else if (atBottom || lastMine?.id === lastId) {
      el.scrollTop = el.scrollHeight;
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps -- chỉ khi danh sách tin thay đổi
  }, [lastId, pending.length, messages.length]);

  const onScroll = () => {
    const el = scrollRef.current;
    if (!el) return;
    const bottom = el.scrollHeight - el.scrollTop - el.clientHeight < 80;
    setAtBottom(bottom);
    if (bottom) setSeenLastId(lastId);
  };

  const loadOlder = () => {
    const el = scrollRef.current;
    if (el) keepOffsetRef.current = el.scrollHeight - el.scrollTop;
    void history.fetchNextPage();
  };

  const scrollToBottom = () => {
    const el = scrollRef.current;
    if (el) el.scrollTo({ top: el.scrollHeight, behavior: "smooth" });
  };

  // ----- Gửi -----

  const pickFiles = (list: FileList | null) => {
    if (!list) return;
    const chosen = Array.from(list);
    const tooBig = chosen.find((f) => f.size > MAX_IMAGE_BYTES);
    if (tooBig) toast.error(`Ảnh "${tooBig.name}" lớn hơn 5MB`);
    const ok = chosen.filter((f) => f.size <= MAX_IMAGE_BYTES && f.type.startsWith("image/"));
    const room = MAX_IMAGES - files.length;
    if (ok.length > room) toast.error(`Mỗi tin tối đa ${MAX_IMAGES} ảnh`);
    setFiles((current) => [...current, ...ok.slice(0, room).map((file) => ({ file, preview: URL.createObjectURL(file) }))]);
    if (fileInputRef.current) fileInputRef.current.value = "";
  };

  const removeFile = (index: number) =>
    setFiles((current) => {
      URL.revokeObjectURL(current[index].preview);
      return current.filter((_, i) => i !== index);
    });

  const deliver = async (item: Pending) => {
    if (scope === "admin") return;
    try {
      const message = await sendChatMessage(scope, requestId, item.body, item.files, item.clientId);
      appendMessage(message);
      setPending((list) => list.filter((p) => p.clientId !== item.clientId));
      item.previews.forEach((url) => URL.revokeObjectURL(url));
    } catch (error) {
      toast.error(errorMessage(error));
      setPending((list) => list.map((p) => (p.clientId === item.clientId ? { ...p, failed: true } : p)));
    }
  };

  const send = () => {
    const body = text.trim();
    if (!body && files.length === 0) return;
    if (body.length > MAX_LENGTH) {
      toast.error(`Tin nhắn tối đa ${MAX_LENGTH} ký tự`);
      return;
    }
    const item: Pending = {
      clientId: crypto.randomUUID(),
      body,
      files: files.map((f) => f.file),
      previews: files.map((f) => f.preview),
      failed: false,
    };
    setPending((list) => [...list, item]);
    setText("");
    setFiles([]);
    setAtBottom(true);
    void deliver(item);
  };

  const retry = (item: Pending) => {
    setPending((list) => list.map((p) => (p.clientId === item.clientId ? { ...p, failed: false } : p)));
    void deliver({ ...item, failed: false });
  };

  const onKeyDown = (e: KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === "Enter" && !e.shiftKey && !e.nativeEvent.isComposing) {
      e.preventDefault();
      send();
    }
  };

  const hasNewBelow = !atBottom && lastId > seenLastId && seenLastId > 0;
  const canWrite = scope !== "admin" && thread.canWrite;

  return (
    <div className="flex h-[560px] flex-col">
      <div className="relative min-h-0 flex-1">
        <div ref={scrollRef} onScroll={onScroll} className="h-full overflow-y-auto px-4 py-4" aria-live="polite">
          {history.isPending ? (
            <div className="flex flex-col gap-3">
              <Skeleton className="h-12 w-2/3 rounded-2xl" />
              <Skeleton className="ml-auto h-12 w-1/2 rounded-2xl" />
              <Skeleton className="h-12 w-3/5 rounded-2xl" />
            </div>
          ) : history.isError ? (
            <p className="text-sm text-destructive">{errorMessage(history.error)}</p>
          ) : (
            <div className="flex flex-col gap-1">
              {history.hasNextPage && (
                <Button variant="ghost" size="sm" className="mx-auto mb-2 rounded-lg" onClick={loadOlder} disabled={history.isFetchingNextPage}>
                  {history.isFetchingNextPage && <Loader2 className="animate-spin" />} Xem tin cũ hơn
                </Button>
              )}
              {messages.length === 0 && pending.length === 0 && (
                <p className="py-16 text-center text-sm text-muted-foreground">
                  {canWrite ? "Chưa có tin nhắn. Hãy gửi lời chào để bắt đầu trao đổi." : "Chưa có tin nhắn nào."}
                </p>
              )}
              {messages.map((m, i) => (
                <MessageRow
                  key={m.id}
                  message={m}
                  mine={isMine(scope, m)}
                  scope={scope}
                  thread={thread}
                  showDay={i === 0 || dayKey(messages[i - 1].createdAt) !== dayKey(m.createdAt)}
                  status={
                    scope !== "admin" && m.id === lastMine?.id
                      ? thread.counterpartLastRead >= m.id
                        ? "Đã xem"
                        : "Đã gửi"
                      : null
                  }
                />
              ))}
              {pending.map((p) => (
                <PendingRow key={p.clientId} item={p} onRetry={() => retry(p)} />
              ))}
            </div>
          )}
        </div>
        {hasNewBelow && (
          <Button size="sm" className="absolute bottom-3 left-1/2 -translate-x-1/2 rounded-full shadow-md" onClick={scrollToBottom}>
            <ArrowDown /> Tin mới
          </Button>
        )}
      </div>

      <div className="border-t bg-muted/30 px-4 py-3">
        {canWrite ? (
          <>
            {files.length > 0 && (
              <div className="mb-2 flex flex-wrap gap-2">
                {files.map((f, i) => (
                  <div key={f.preview} className="relative size-16 overflow-hidden rounded-lg border bg-muted">
                    {/* eslint-disable-next-line @next/next/no-img-element -- ảnh xem trước từ máy (blob:) */}
                    <img src={f.preview} alt="" className="size-full object-cover" />
                    <button
                      type="button"
                      onClick={() => removeFile(i)}
                      className="absolute top-0.5 right-0.5 rounded-full bg-black/60 p-0.5 text-white"
                      aria-label="Bỏ ảnh"
                    >
                      <X className="size-3" />
                    </button>
                  </div>
                ))}
              </div>
            )}
            <div className="flex items-end gap-2">
              <input
                ref={fileInputRef}
                type="file"
                accept="image/jpeg,image/png,image/webp"
                multiple
                hidden
                onChange={(e) => pickFiles(e.target.files)}
              />
              <Button
                type="button"
                variant="ghost"
                size="icon"
                className="shrink-0 rounded-xl"
                onClick={() => fileInputRef.current?.click()}
                disabled={files.length >= MAX_IMAGES}
                aria-label="Đính kèm ảnh"
              >
                <ImagePlus />
              </Button>
              <Textarea
                value={text}
                onChange={(e) => setText(e.target.value)}
                onKeyDown={onKeyDown}
                rows={1}
                maxLength={MAX_LENGTH}
                placeholder="Nhập tin nhắn… (Enter để gửi, Shift+Enter xuống dòng)"
                className="max-h-32 min-h-10 resize-none rounded-xl bg-background text-base"
                aria-label="Tin nhắn"
              />
              <Button type="button" size="icon" className="shrink-0 rounded-xl" onClick={send} disabled={!text.trim() && files.length === 0} aria-label="Gửi">
                <Send />
              </Button>
            </div>
            <p className="mt-2 flex items-center gap-1.5 text-xs text-muted-foreground">
              <ShieldCheck className="size-3.5 shrink-0" />
              Số điện thoại và email trong tin nhắn được ẩn. TripConnect có thể xem lại cuộc trò chuyện khi cần hỗ trợ.
            </p>
          </>
        ) : (
          <p className="flex items-center gap-2 text-sm text-muted-foreground">
            <Lock className="size-4 shrink-0" />
            {scope === "admin"
              ? "Quản trị viên chỉ xem cuộc trò chuyện (nội dung gốc, không che)."
              : thread.current
                ? "Cuộc trò chuyện đã đóng — bạn chỉ xem lại được tin cũ."
                : "Đây là cuộc trò chuyện với đơn vị trước đây — chỉ xem lại."}
          </p>
        )}
      </div>
    </div>
  );
}

function dedupe(messages: ChatMessage[]): ChatMessage[] {
  const seen = new Set<number>();
  return messages.filter((m) => (seen.has(m.id) ? false : (seen.add(m.id), true)));
}

const dayKey = (iso: string) => iso.slice(0, 10);

function dayLabel(iso: string): string {
  const date = new Date(iso);
  const today = new Date();
  const yesterday = new Date();
  yesterday.setDate(today.getDate() - 1);
  if (date.toDateString() === today.toDateString()) return "Hôm nay";
  if (date.toDateString() === yesterday.toDateString()) return "Hôm qua";
  return date.toLocaleDateString("vi-VN", { day: "2-digit", month: "2-digit", year: "numeric" });
}

const timeText = (iso: string) => new Date(iso).toLocaleTimeString("vi-VN", { hour: "2-digit", minute: "2-digit" });

function MessageRow({
  message: m,
  mine,
  scope,
  thread,
  showDay,
  status,
}: {
  message: ChatMessage;
  mine: boolean;
  scope: ChatScope;
  thread: ChatThread;
  showDay: boolean;
  status: string | null;
}) {
  return (
    <>
      {showDay && (
        <div className="my-3 flex items-center gap-3 text-xs text-muted-foreground">
          <span className="h-px flex-1 bg-border" />
          {dayLabel(m.createdAt)}
          <span className="h-px flex-1 bg-border" />
        </div>
      )}
      <div className={cn("flex flex-col gap-1", mine ? "items-end" : "items-start")}>
        {scope === "admin" && (
          <span className="px-1 text-xs text-muted-foreground">{m.fromCustomer ? thread.customerName : thread.agentName}</span>
        )}
        <div
          className={cn(
            "flex max-w-[80%] flex-col gap-2 rounded-2xl px-3.5 py-2 text-sm",
            mine ? "rounded-br-md bg-primary text-primary-foreground" : "rounded-bl-md bg-muted",
          )}
        >
          {m.images.length > 0 && <ReviewImages images={m.images} />}
          {m.body && <p className="leading-relaxed wrap-break-word whitespace-pre-line">{m.body}</p>}
        </div>
        <span className="px-1 text-[11px] text-muted-foreground">
          {timeText(m.createdAt)}
          {m.masked && " · đã ẩn thông tin liên hệ"}
          {status && ` · ${status}`}
        </span>
      </div>
    </>
  );
}

function PendingRow({ item, onRetry }: { item: Pending; onRetry: () => void }) {
  return (
    <div className="flex flex-col items-end gap-1 opacity-70">
      <div className="flex max-w-[80%] flex-col gap-2 rounded-2xl rounded-br-md bg-primary px-3.5 py-2 text-sm text-primary-foreground">
        {item.previews.length > 0 && (
          <div className="flex flex-wrap gap-2">
            {item.previews.map((url) => (
              // eslint-disable-next-line @next/next/no-img-element -- ảnh đang gửi (blob:)
              <img key={url} src={url} alt="" className="size-20 rounded-lg object-cover" />
            ))}
          </div>
        )}
        {item.body && <p className="leading-relaxed wrap-break-word whitespace-pre-line">{item.body}</p>}
      </div>
      {item.failed ? (
        <button type="button" onClick={onRetry} className="flex items-center gap-1 px-1 text-[11px] font-medium text-destructive">
          <RotateCcw className="size-3" /> Gửi không được · Thử lại
        </button>
      ) : (
        <span className="flex items-center gap-1 px-1 text-[11px] text-muted-foreground">
          <Loader2 className="size-3 animate-spin" /> Đang gửi…
        </span>
      )}
    </div>
  );
}
