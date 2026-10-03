import { Badge } from "@/components/ui/badge";
import { cn } from "@/lib/utils";
import type { ChangeRequestStatus } from "@/types/agent";
import type { AgentStatus, UserRole } from "@/types/auth";

const AGENT_STATUS: Record<AgentStatus, { label: string; className: string }> = {
  DRAFT: { label: "Nháp", className: "bg-sky-100 text-sky-900" },
  PENDING_APPROVAL: { label: "Chờ duyệt", className: "bg-amber-100 text-amber-900" },
  NEEDS_REVISION: { label: "Cần bổ sung", className: "bg-red-100 text-red-900" },
  APPROVED: { label: "Đã duyệt", className: "bg-emerald-100 text-emerald-900" },
};

const CHANGE_REQUEST_STATUS: Record<ChangeRequestStatus, { label: string; className: string }> = {
  PENDING: { label: "Chờ duyệt", className: "bg-amber-100 text-amber-900" },
  APPROVED: { label: "Đã duyệt", className: "bg-emerald-100 text-emerald-900" },
  REJECTED: { label: "Bị từ chối", className: "bg-red-100 text-red-900" },
  CANCELLED: { label: "Đã hủy", className: "bg-muted text-muted-foreground" },
};

export const USER_ROLE_LABELS: Record<UserRole, string> = {
  CUSTOMER: "Khách hàng",
  AGENT: "Đối tác",
  ADMIN: "Quản trị viên",
};

export const AGENT_STATUS_LABELS = Object.fromEntries(
  Object.entries(AGENT_STATUS).map(([key, value]) => [key, value.label]),
) as Record<AgentStatus, string>;

export const CHANGE_REQUEST_STATUS_LABELS = Object.fromEntries(
  Object.entries(CHANGE_REQUEST_STATUS).map(([key, value]) => [key, value.label]),
) as Record<ChangeRequestStatus, string>;

export function AgentStatusBadge({ status }: { status: AgentStatus }) {
  return <Badge className={cn("border-0", AGENT_STATUS[status].className)}>{AGENT_STATUS[status].label}</Badge>;
}

export function ChangeRequestStatusBadge({ status }: { status: ChangeRequestStatus }) {
  return (
    <Badge className={cn("border-0", CHANGE_REQUEST_STATUS[status].className)}>{CHANGE_REQUEST_STATUS[status].label}</Badge>
  );
}

export function ActiveBadge({ active }: { active: boolean }) {
  return (
    <Badge className={cn("border-0", active ? "bg-emerald-100 text-emerald-900" : "bg-red-100 text-red-900")}>
      {active ? "Hoạt động" : "Vô hiệu hóa"}
    </Badge>
  );
}
