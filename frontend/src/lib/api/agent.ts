import type {
  AgentChangeRequestResponse,
  AgentDocumentResponse,
  AgentDocumentType,
  AgentProfileResponse,
  TemporaryUrlResponse,
  UpdateAgentProfileRequest,
} from "@/types/agent";
import { apiRequest } from "./client";

// API hồ sơ kinh doanh của Agent đang đăng nhập (Backend: AgentProfileController)

export const getMyAgentProfile = () => apiRequest<AgentProfileResponse>("/api/agent/profile");

export const updateMyAgentProfile = (data: UpdateAgentProfileRequest) =>
  apiRequest<AgentProfileResponse>("/api/agent/profile", { method: "PUT", body: data });

export const updateBusinessLicense = (businessLicense: string) =>
  apiRequest<AgentProfileResponse>("/api/agent/profile/business-license", { method: "PATCH", body: { businessLicense } });

export const updateAgentExpertise = (data: { locationIds: number[]; categoryIds: number[] }) =>
  apiRequest<AgentProfileResponse>("/api/agent/profile/expertise", { method: "PUT", body: data });

export const uploadAgentDocument = (type: AgentDocumentType, file: File) => {
  const formData = new FormData();
  formData.append("type", type);
  formData.append("file", file);
  return apiRequest<AgentDocumentResponse>("/api/agent/profile/documents", { method: "POST", body: formData });
};

export const deleteAgentDocument = (documentId: number) =>
  apiRequest<void>(`/api/agent/profile/documents/${documentId}`, { method: "DELETE" });

export const getAgentDocumentUrl = (documentId: number) =>
  apiRequest<TemporaryUrlResponse>(`/api/agent/profile/documents/${documentId}/url`);

export const submitAgentProfile = () => apiRequest<AgentProfileResponse>("/api/agent/profile/submit", { method: "POST" });

export const setAcceptingRequests = (accepting: boolean) =>
  apiRequest<AgentProfileResponse>("/api/agent/profile/accepting-requests", { method: "PATCH", body: { accepting } });

// ----- Yêu cầu cập nhật hồ sơ (sau khi đã được duyệt) -----

export const listMyChangeRequests = () =>
  apiRequest<AgentChangeRequestResponse[]>("/api/agent/profile/change-requests");

/** multipart/form-data: các trường chữ thay đổi + file giấy tờ thay thế, gửi trong 1 request. */
export const createChangeRequest = (formData: FormData) =>
  apiRequest<AgentChangeRequestResponse>("/api/agent/profile/change-requests", { method: "POST", body: formData });

export const cancelChangeRequest = (requestId: number) =>
  apiRequest<void>(`/api/agent/profile/change-requests/${requestId}`, { method: "DELETE" });
