// Khớp ChatResponses của Backend

export interface ChatThread {
  agentId: number;
  agentName: string;
  /** Agent xem: tên đã rút gọn */
  customerName: string;
  /** Đơn vị đang phụ trách yêu cầu */
  current: boolean;
  canWrite: boolean;
  unread: number;
  myLastRead: number;
  /** Bên kia đã đọc tới tin nào ("Đã xem") */
  counterpartLastRead: number;
}

export interface ChatImage {
  id: number;
  url: string;
}

export interface ChatMessage {
  id: number;
  requestId: number;
  agentId: number;
  senderId: number;
  fromCustomer: boolean;
  /** Đã che số điện thoại / email (trừ Admin) */
  body: string | null;
  masked: boolean;
  images: ChatImage[];
  createdAt: string;
  /** Mã tạm khi gửi, để thay tin "đang gửi" bằng tin thật */
  clientId: string | null;
}

export interface ChatPage {
  /** Cũ trước, mới sau */
  items: ChatMessage[];
  hasMore: boolean;
}

export interface ChatRead {
  requestId: number;
  agentId: number;
  userId: number;
  lastReadMessageId: number;
}

/** Sự kiện đẩy qua WebSocket (RealtimePublisher.Event) */
export type RealtimeEvent =
  | { type: "CHAT_MESSAGE"; data: ChatMessage }
  | { type: "CHAT_READ"; data: ChatRead }
  | { type: "NOTIFICATION"; data: null };
