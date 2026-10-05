import { Badge } from "@/components/ui/badge";
import { ASSIGNMENT_STATUS, REQUEST_STATUS } from "@/lib/customRequest/labels";
import { cn } from "@/lib/utils";
import type { AssignmentStatus, CustomRequestStatus } from "@/types/customRequest";

export function RequestStatusBadge({ status, customer = false }: { status: CustomRequestStatus; customer?: boolean }) {
  const s = REQUEST_STATUS[status];
  return <Badge className={cn("border-0", s.className)}>{customer ? s.customerLabel : s.label}</Badge>;
}

export function AssignmentStatusBadge({ status }: { status: AssignmentStatus }) {
  return <Badge className={cn("border-0", ASSIGNMENT_STATUS[status].className)}>{ASSIGNMENT_STATUS[status].label}</Badge>;
}
