import type { SettlementBankAccount, SettlementDisputeStatus, SettlementItemKind, SettlementStatus } from "@/types/settlement";

// Khớp SettlementRules của Backend
export const SETTLEMENT_RULES = { confirmDays: 5 } as const;

export const SETTLEMENT_STATUS: Record<SettlementStatus, { label: string; agentLabel: string; className: string }> = {
  PENDING_CONFIRM: { label: "Chờ Agent xác nhận", agentLabel: "Chờ bạn xác nhận", className: "bg-amber-100 text-amber-900" },
  DISPUTED: { label: "Có khiếu nại", agentLabel: "Đang xử lý khiếu nại", className: "bg-red-100 text-red-900" },
  AWAITING_PAYMENT: { label: "Chờ thanh toán", agentLabel: "Chờ TripConnect thanh toán", className: "bg-sky-100 text-sky-900" },
  PAID: { label: "Đã thanh toán", agentLabel: "Đã nhận tiền", className: "bg-emerald-100 text-emerald-900" },
};

export const SETTLEMENT_ITEM_KIND: Record<SettlementItemKind, string> = {
  COMPLETED: "Hoàn thành",
  CANCELLED: "Hủy, vẫn giữ tiền",
};

export const DISPUTE_STATUS: Record<SettlementDisputeStatus, string> = {
  OPEN: "Chờ xử lý",
  ADJUSTED: "Đã điều chỉnh",
  REJECTED: "Không chấp nhận",
};

/** "Tháng 10/2026" */
export const periodTitle = (label: string) => label.charAt(0).toUpperCase() + label.slice(1);

/** Ảnh mã VietQR chuyển khoản đúng tài khoản, số tiền và nội dung (dịch vụ ảnh công khai của VietQR). */
export function vietQrUrl(bank: SettlementBankAccount, amount: number, content: string): string {
  const params = new URLSearchParams({ amount: String(amount), addInfo: content, accountName: bank.accountHolder });
  return `https://img.vietqr.io/image/${bank.bin}-${bank.accountNumber}-compact2.png?${params}`;
}
