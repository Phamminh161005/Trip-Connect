import type { TemporaryUrlResponse } from "@/types/agent";
import type { PageResponse } from "@/types/common";
import type { ResolveRequest, SettlementDetail, SettlementStatus, SettlementSummary } from "@/types/settlement";
import { apiRequest } from "./client";
import { query } from "./query";

export type SettlementScope = "agent" | "admin";

export const listSettlements = (scope: SettlementScope, params: { status?: SettlementStatus; page?: number; size?: number }) =>
  apiRequest<PageResponse<SettlementSummary>>(`/api/${scope}/settlements${query({ ...params })}`);

export const getSettlement = (scope: SettlementScope, id: number) => apiRequest<SettlementDetail>(`/api/${scope}/settlements/${id}`);

export const getSettlementReceiptUrl = (scope: SettlementScope, id: number) =>
  apiRequest<TemporaryUrlResponse>(`/api/${scope}/settlements/${id}/receipt-url`);

// ----- Agent -----

export const confirmSettlement = (id: number) =>
  apiRequest<SettlementDetail>(`/api/agent/settlements/${id}/confirm`, { method: "POST" });

export const disputeSettlement = (id: number, reason: string) =>
  apiRequest<SettlementDetail>(`/api/agent/settlements/${id}/dispute`, { method: "POST", body: { reason } });

// ----- Admin -----

export const generateSettlements = () => apiRequest<{ created: number }>("/api/admin/settlements/generate", { method: "POST" });

export const resolveSettlementDispute = (id: number, body: ResolveRequest) =>
  apiRequest<SettlementDetail>(`/api/admin/settlements/${id}/resolve`, { method: "POST", body });

export function paySettlement(id: number, transactionRef: string, receipt: File | null) {
  const form = new FormData();
  form.append("transactionRef", transactionRef);
  if (receipt) form.append("receipt", receipt);
  return apiRequest<SettlementDetail>(`/api/admin/settlements/${id}/pay`, { method: "POST", body: form });
}
