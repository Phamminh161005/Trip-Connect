// Khớp SettlementResponses / SettlementRequests của Backend

export type SettlementStatus = "PENDING_CONFIRM" | "DISPUTED" | "AWAITING_PAYMENT" | "PAID";
export type SettlementItemKind = "COMPLETED" | "CANCELLED";
export type SettlementDisputeStatus = "OPEN" | "ADJUSTED" | "REJECTED";

export interface SettlementSummary {
  id: number;
  code: string;
  agentId: number;
  agentName: string;
  /** "tháng 10/2026" hoặc "đến 15:30 07/10/2026" */
  periodLabel: string;
  cutoffAt: string;
  status: SettlementStatus;
  itemCount: number;
  retainedAmount: number;
  commissionAmount: number;
  adjustmentAmount: number;
  payoutAmount: number;
  confirmDeadline: string | null;
  paidAt: string | null;
  createdAt: string;
}

export interface SettlementItem {
  id: number;
  bookingId: number;
  bookingCode: string;
  tourTitle: string;
  startDate: string;
  customerName: string;
  kind: SettlementItemKind;
  eventAt: string;
  paidAmount: number;
  refundedAmount: number;
  retainedAmount: number;
  commissionRate: number;
  commissionAmount: number;
  payoutAmount: number;
}

export interface SettlementAdjustment {
  id: number;
  amount: number;
  reason: string;
  createdByName: string;
  createdAt: string;
}

export interface SettlementDispute {
  id: number;
  reason: string;
  status: SettlementDisputeStatus;
  resolution: string | null;
  resolvedByName: string | null;
  resolvedAt: string | null;
  createdAt: string;
}

export interface SettlementBankAccount {
  bin: string;
  bankName: string;
  accountNumber: string;
  accountHolder: string;
}

export interface SettlementDetail {
  summary: SettlementSummary;
  items: SettlementItem[];
  adjustments: SettlementAdjustment[];
  disputes: SettlementDispute[];
  /** Đã trả: tài khoản lúc trả; chưa trả: tài khoản hiện tại trong hồ sơ Agent */
  bankAccount: SettlementBankAccount | null;
  confirmedAt: string | null;
  autoConfirmed: boolean;
  paidByName: string | null;
  transactionRef: string | null;
  hasReceipt: boolean;
  canConfirm: boolean;
  canDispute: boolean;
  canResolve: boolean;
  canPay: boolean;
}

export interface AdjustmentInput {
  amount: number;
  reason: string;
}

export interface ResolveRequest {
  decision: "ADJUST" | "REJECT";
  resolution: string;
  adjustments: AdjustmentInput[] | null;
}
