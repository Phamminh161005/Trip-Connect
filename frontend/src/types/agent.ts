// Khớp các DTO hồ sơ Agent của Backend (AgentProfileResponse, AgentDocumentResponse...)
import type { AgentStatus } from "./auth";
import type { BankResponse, LocationResponse, TourCategoryResponse } from "./catalog";

export type AgentDocumentType =
  | "TRAVEL_LICENSE"
  | "BUSINESS_REGISTRATION"
  | "REPRESENTATIVE_ID_FRONT"
  | "REPRESENTATIVE_ID_BACK"
  | "OTHER";

export type AgentDocumentStatus = "ACTIVE" | "PENDING" | "ARCHIVED";

export interface AgentDocumentResponse {
  id: number;
  type: AgentDocumentType;
  typeLabel: string;
  status: AgentDocumentStatus;
  originalFilename: string | null;
  mimeType: string;
  sizeBytes: number;
  uploadedAt: string;
}

export interface AgentProfileResponse {
  id: number;
  userId: number;
  fullName: string;
  email: string;
  phone: string | null;
  companyName: string | null;
  taxCode: string | null;
  businessLicense: string | null;
  addressProvince: LocationResponse | null;
  address: string | null;
  bank: BankResponse | null;
  bankAccountNumber: string | null;
  bankAccountHolder: string | null;
  status: AgentStatus;
  rejectionReason: string | null;
  acceptingRequests: boolean;
  maxOpenRequests: number;
  rating: number | null;
  ratingCount: number;
  submittedAt: string | null;
  reviewedAt: string | null;
  createdAt: string;
  serviceAreas: LocationResponse[];
  specialties: TourCategoryResponse[];
  documents: AgentDocumentResponse[];
  pendingChangeRequestId: number | null;
  /** Mục còn thiếu để được nộp hồ sơ (rỗng = đủ điều kiện). Chỉ có khi hồ sơ Nháp / Cần bổ sung. */
  missingItems: string[];
}

export interface UpdateAgentProfileRequest {
  companyName: string;
  taxCode: string;
  businessLicense?: string | null;
  addressProvinceId: number;
  address: string;
  bankBin: string;
  bankAccountNumber: string;
  bankAccountHolder: string;
}

export type ChangeRequestStatus = "PENDING" | "APPROVED" | "REJECTED" | "CANCELLED";

/** Yêu cầu cập nhật hồ sơ. Trường = null nghĩa là yêu cầu KHÔNG đổi trường đó. */
export interface AgentChangeRequestResponse {
  id: number;
  agentProfileId: number;
  status: ChangeRequestStatus;
  companyName: string | null;
  taxCode: string | null;
  businessLicense: string | null;
  addressProvince: LocationResponse | null;
  address: string | null;
  bank: BankResponse | null;
  bankAccountNumber: string | null;
  bankAccountHolder: string | null;
  note: string | null;
  rejectionReason: string | null;
  createdAt: string;
  reviewedAt: string | null;
  documents: AgentDocumentResponse[];
}

export interface TemporaryUrlResponse {
  url: string;
  expiresAt: string;
}
