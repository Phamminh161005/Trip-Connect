import type { PageResponse } from "@/types/common";
import type {
  AgentCandidate,
  CreateCustomRequest,
  CustomRequestDetail,
  CustomRequestStatus,
  CustomRequestSummary,
  ProposalRequest,
} from "@/types/customRequest";
import { apiRequest } from "./client";
import { query } from "./query";

// ----- Khách (và Agent với tư cách khách) -----

export const createCustomRequest = (data: CreateCustomRequest) =>
  apiRequest<CustomRequestDetail>("/api/custom-requests", { method: "POST", body: data });

export const listMyCustomRequests = (params: { page?: number; size?: number }) =>
  apiRequest<PageResponse<CustomRequestSummary>>(`/api/custom-requests${query({ ...params })}`);

export const getMyCustomRequest = (id: number) => apiRequest<CustomRequestDetail>(`/api/custom-requests/${id}`);

export const cancelCustomRequest = (id: number, reason: string | null) =>
  apiRequest<CustomRequestDetail>(`/api/custom-requests/${id}/cancel`, { method: "POST", body: { reason } });

export const acceptProposal = (id: number, proposalId: number) =>
  apiRequest<CustomRequestDetail>(`/api/custom-requests/${id}/proposals/accept`, { method: "POST", body: { proposalId } });

export const requestProposalRevision = (id: number, proposalId: number, feedback: string) =>
  apiRequest<CustomRequestDetail>(`/api/custom-requests/${id}/proposals/revise`, { method: "POST", body: { proposalId, feedback } });

// ----- Agent -----

export type AgentRequestTab = "PENDING" | "ACCEPTED" | "HISTORY";

export const listAgentCustomRequests = (params: { tab?: AgentRequestTab; page?: number; size?: number }) =>
  apiRequest<PageResponse<CustomRequestSummary>>(`/api/agent/custom-requests${query({ ...params })}`);

export const getAgentCustomRequest = (id: number) => apiRequest<CustomRequestDetail>(`/api/agent/custom-requests/${id}`);

export const acceptCustomRequest = (id: number) =>
  apiRequest<CustomRequestDetail>(`/api/agent/custom-requests/${id}/accept`, { method: "POST" });

export const declineCustomRequest = (id: number, reason: string) =>
  apiRequest<CustomRequestDetail>(`/api/agent/custom-requests/${id}/decline`, { method: "POST", body: { reason } });

export const submitProposal = (id: number, data: ProposalRequest) =>
  apiRequest<CustomRequestDetail>(`/api/agent/custom-requests/${id}/proposals`, { method: "POST", body: data });

// ----- Admin -----

export const listAdminCustomRequests = (params: { status?: CustomRequestStatus; q?: string; page?: number; size?: number }) =>
  apiRequest<PageResponse<CustomRequestSummary>>(`/api/admin/custom-requests${query({ ...params })}`);

export const getAdminCustomRequest = (id: number) => apiRequest<CustomRequestDetail>(`/api/admin/custom-requests/${id}`);

export const getRequestCandidates = (id: number) => apiRequest<AgentCandidate[]>(`/api/admin/custom-requests/${id}/candidates`);

export const assignCustomRequest = (id: number, agentId: number) =>
  apiRequest<CustomRequestDetail>(`/api/admin/custom-requests/${id}/assign`, { method: "POST", body: { agentId } });

export const closeCustomRequest = (id: number, reason: string) =>
  apiRequest<CustomRequestDetail>(`/api/admin/custom-requests/${id}/close`, { method: "POST", body: { reason } });
