import type {
  AdminChangeRequestDetailResponse,
  AdminChangeRequestSummaryResponse,
  AdminSummaryResponse,
  AdminUserResponse,
  AgentProfileSummaryResponse,
} from "@/types/admin";
import type { AgentProfileResponse, ChangeRequestStatus, TemporaryUrlResponse } from "@/types/agent";
import type { AgentStatus, UserRole } from "@/types/auth";
import type { PageResponse } from "@/types/common";
import { apiRequest } from "./client";
import { query } from "./query";

export const getAdminSummary = () => apiRequest<AdminSummaryResponse>("/api/admin/summary");

// ----- Hồ sơ đối tác -----

export interface AgentProfileListParams {
  status?: AgentStatus;
  q?: string;
  page?: number;
  size?: number;
  sort?: string;
}

export const listAgentProfiles = (params: AgentProfileListParams) =>
  apiRequest<PageResponse<AgentProfileSummaryResponse>>(`/api/admin/agent-profiles${query({ ...params })}`);

export const getAgentProfileForAdmin = (id: number) =>
  apiRequest<AgentProfileResponse>(`/api/admin/agent-profiles/${id}`);

export const getAgentDocumentUrlForAdmin = (profileId: number, documentId: number) =>
  apiRequest<TemporaryUrlResponse>(`/api/admin/agent-profiles/${profileId}/documents/${documentId}/url`);

export const approveAgentProfile = (id: number) =>
  apiRequest<void>(`/api/admin/agent-profiles/${id}/approve`, { method: "POST" });

export const rejectAgentProfile = (id: number, reason: string) =>
  apiRequest<void>(`/api/admin/agent-profiles/${id}/reject`, { method: "POST", body: { reason } });

// ----- Yêu cầu cập nhật hồ sơ -----

export const listChangeRequests = (params: { status?: ChangeRequestStatus; page?: number; size?: number; sort?: string }) =>
  apiRequest<PageResponse<AdminChangeRequestSummaryResponse>>(`/api/admin/agent-change-requests${query({ ...params })}`);

export const getChangeRequestForAdmin = (id: number) =>
  apiRequest<AdminChangeRequestDetailResponse>(`/api/admin/agent-change-requests/${id}`);

export const approveChangeRequest = (id: number) =>
  apiRequest<void>(`/api/admin/agent-change-requests/${id}/approve`, { method: "POST" });

export const rejectChangeRequest = (id: number, reason: string) =>
  apiRequest<void>(`/api/admin/agent-change-requests/${id}/reject`, { method: "POST", body: { reason } });

// ----- Người dùng -----

export interface UserListParams {
  q?: string;
  role?: UserRole;
  active?: boolean;
  page?: number;
  size?: number;
}

export const listUsers = (params: UserListParams) =>
  apiRequest<PageResponse<AdminUserResponse>>(`/api/admin/users${query({ ...params })}`);

export const deactivateUser = (id: number, reason: string) =>
  apiRequest<AdminUserResponse>(`/api/admin/users/${id}/deactivate`, { method: "PATCH", body: { reason } });

export const activateUser = (id: number) =>
  apiRequest<AdminUserResponse>(`/api/admin/users/${id}/activate`, { method: "PATCH" });
