import type { TourCard } from "./search";

/** Khớp AssistantDtos của Backend. */

export interface AssistantStatus {
  enabled: boolean;
  signedIn: boolean;
  dailyLimit: number;
}

/** Bản nháp yêu cầu tour riêng do trợ lý điền sẵn. */
export interface CustomRequestDraft {
  departureLocationId: number | null;
  departureLocationName: string | null;
  destinationIds: number[];
  destinationNames: string[];
  earliestStart: string | null;
  latestStart: string | null;
  durationDays: number | null;
  adults: number | null;
  children: number | null;
  infants: number | null;
  budgetMax: number | null;
  notes: string | null;
}

export interface AssistantAttachments {
  tours: TourCard[];
  customRequest: CustomRequestDraft | null;
}

export interface AssistantMessage {
  /** id phía client (tin chưa lưu) hoặc id trong CSDL */
  key: string;
  fromUser: boolean;
  content: string;
  attachments: AssistantAttachments | null;
  /** Đang nhận chữ */
  streaming?: boolean;
  /** Trả lời lỗi (không đưa vào ngữ cảnh lần sau) */
  failed?: boolean;
}

export interface ConversationSummary {
  id: number;
  title: string;
  updatedAt: string;
}

export interface ConversationDetail {
  id: number;
  title: string;
  messages: {
    id: number;
    fromUser: boolean;
    content: string;
    attachments: AssistantAttachments | null;
    createdAt: string;
  }[];
}

export interface HistoryMessage {
  fromUser: boolean;
  content: string;
}
