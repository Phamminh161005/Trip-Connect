// Khớp các DTO trang quản trị của Backend
import type { AgentChangeRequestResponse, AgentProfileResponse } from "./agent";
import type { AgentStatus, UserRole } from "./auth";

export interface AdminSummaryResponse {
  pendingAgentProfiles: number;
  pendingChangeRequests: number;
  totalUsers: number;
  approvedAgents: number;
  pendingTours: number;
  manualRefunds: number;
  pendingCustomRequests: number;
}

/** Một dòng trong danh sách hồ sơ đối tác. */
export interface AgentProfileSummaryResponse {
  id: number;
  userId: number;
  fullName: string;
  email: string;
  companyName: string | null;
  taxCode: string | null;
  status: AgentStatus;
  submittedAt: string | null;
  createdAt: string;
}

export interface AdminChangeRequestSummaryResponse {
  id: number;
  agentProfileId: number;
  currentCompanyName: string | null;
  agentEmail: string;
  status: AgentChangeRequestResponse["status"];
  createdAt: string;
}

export interface AdminChangeRequestDetailResponse {
  changeRequest: AgentChangeRequestResponse;
  currentProfile: AgentProfileResponse;
}

export interface AdminUserResponse {
  id: number;
  fullName: string | null;
  email: string;
  phone: string | null;
  role: UserRole;
  active: boolean;
  emailVerified: boolean;
  googleLinked: boolean;
  deactivatedReason: string | null;
  deactivatedAt: string | null;
  lastLoginAt: string | null;
  createdAt: string;
  agentProfileId: number | null;
}
