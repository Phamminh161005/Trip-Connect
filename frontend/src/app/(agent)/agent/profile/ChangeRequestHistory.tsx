"use client";

import { useState } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { ChevronDown, Eye, FileText } from "lucide-react";
import { toast } from "sonner";
import { ConfirmDialog } from "@/components/common/ConfirmDialog";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { openAgentDocument } from "@/lib/agent/documents";
import { cancelChangeRequest, listMyChangeRequests } from "@/lib/api/agent";
import { errorMessage } from "@/lib/api/errors";
import { formatDateTime } from "@/lib/format";
import { cn } from "@/lib/utils";
import type { AgentChangeRequestResponse, ChangeRequestStatus } from "@/types/agent";
import { AGENT_PROFILE_KEY } from "./useAgentProfile";

export const CHANGE_REQUESTS_KEY = ["agent-change-requests"] as const;

const STATUS: Record<ChangeRequestStatus, { label: string; className: string }> = {
  PENDING: { label: "Chờ duyệt", className: "bg-amber-100 text-amber-900" },
  APPROVED: { label: "Đã duyệt", className: "bg-emerald-100 text-emerald-900" },
  REJECTED: { label: "Bị từ chối", className: "bg-red-100 text-red-900" },
  CANCELLED: { label: "Đã hủy", className: "bg-muted text-muted-foreground" },
};

/** Danh sách những gì yêu cầu thay đổi (trường = null nghĩa là không đổi). */
function describeChanges(request: AgentChangeRequestResponse): { label: string; value: string }[] {
  const changes: { label: string; value: string }[] = [];
  if (request.companyName) changes.push({ label: "Tên công ty", value: request.companyName });
  if (request.taxCode) changes.push({ label: "Mã số thuế", value: request.taxCode });
  if (request.businessLicense) changes.push({ label: "Số giấy phép lữ hành", value: request.businessLicense });
  if (request.addressProvince || request.address) {
    changes.push({
      label: "Địa chỉ trụ sở",
      value: [request.address, request.addressProvince?.province].filter(Boolean).join(", "),
    });
  }
  if (request.bank) {
    changes.push({
      label: "Tài khoản ngân hàng",
      value: `${request.bank.shortName} · ${request.bankAccountNumber} · ${request.bankAccountHolder}`,
    });
  }
  return changes;
}

export function ChangeRequestHistory() {
  const query = useQuery({ queryKey: CHANGE_REQUESTS_KEY, queryFn: listMyChangeRequests });

  return (
    <Card id="change-requests" className="scroll-mt-24 rounded-2xl">
      <CardHeader>
        <CardTitle className="text-lg">Lịch sử yêu cầu cập nhật hồ sơ</CardTitle>
        <CardDescription>Thay đổi thông tin pháp lý chỉ được áp dụng sau khi quản trị viên duyệt.</CardDescription>
      </CardHeader>
      <CardContent>
        {query.isPending ? (
          <Skeleton className="h-20 rounded-xl" />
        ) : query.isError ? (
          <p className="text-sm text-destructive">{errorMessage(query.error)}</p>
        ) : query.data.length === 0 ? (
          <p className="text-sm text-muted-foreground">Bạn chưa gửi yêu cầu cập nhật nào.</p>
        ) : (
          <ul className="flex flex-col gap-3">
            {query.data.map((request) => (
              <ChangeRequestItem key={request.id} request={request} />
            ))}
          </ul>
        )}
      </CardContent>
    </Card>
  );
}

function ChangeRequestItem({ request }: { request: AgentChangeRequestResponse }) {
  const queryClient = useQueryClient();
  const [expanded, setExpanded] = useState(request.status === "PENDING");
  const [confirming, setConfirming] = useState(false);

  const changes = describeChanges(request);
  const summary = [
    ...changes.map((c) => c.label),
    ...(request.documents.length ? [`${request.documents.length} giấy tờ`] : []),
  ].join(", ");

  return (
    <li className="rounded-2xl border">
      <button
        type="button"
        onClick={() => setExpanded((v) => !v)}
        aria-expanded={expanded}
        className="flex w-full items-center justify-between gap-3 rounded-2xl p-4 text-left outline-none hover:bg-muted/40 focus-visible:ring-3 focus-visible:ring-ring/50"
      >
        <div className="flex min-w-0 flex-col gap-1">
          <div className="flex flex-wrap items-center gap-2">
            <Badge className={cn("border-0", STATUS[request.status].className)}>{STATUS[request.status].label}</Badge>
            <span className="text-sm text-muted-foreground">Gửi lúc {formatDateTime(request.createdAt)}</span>
          </div>
          <p className="truncate text-sm font-medium">{summary || "Không có thay đổi"}</p>
        </div>
        <ChevronDown className={cn("size-4 shrink-0 transition-transform", expanded && "rotate-180")} />
      </button>

      {expanded && (
        <div className="flex flex-col gap-3 border-t px-4 py-4 text-sm">
          {changes.length > 0 && (
            <dl className="flex flex-col gap-1.5">
              {changes.map((c) => (
                <div key={c.label} className="grid gap-0.5 sm:grid-cols-[180px_1fr]">
                  <dt className="text-muted-foreground">{c.label}</dt>
                  <dd className="font-medium wrap-break-word">{c.value}</dd>
                </div>
              ))}
            </dl>
          )}
          {request.documents.length > 0 && (
            <ul className="flex flex-col gap-1.5">
              {request.documents.map((document) => (
                <li key={document.id} className="flex items-center justify-between gap-2 rounded-xl bg-muted/50 px-3 py-2">
                  <span className="flex min-w-0 items-center gap-1.5">
                    <FileText className="size-3.5 shrink-0" />
                    <span className="truncate">{document.typeLabel}: {document.originalFilename ?? "Tệp đính kèm"}</span>
                  </span>
                  <Button variant="ghost" size="sm" className="rounded-lg" onClick={() => openAgentDocument(document.id)}>
                    <Eye /> Xem
                  </Button>
                </li>
              ))}
            </ul>
          )}
          {request.note && (
            <p>
              <span className="text-muted-foreground">Ghi chú của bạn: </span>
              {request.note}
            </p>
          )}
          {request.status === "REJECTED" && request.rejectionReason && (
            <p className="rounded-xl bg-red-50 px-3 py-2 text-red-900">
              <span className="font-semibold">Lý do từ chối: </span>
              {request.rejectionReason}
            </p>
          )}
          {request.reviewedAt && <p className="text-muted-foreground">Xử lý lúc {formatDateTime(request.reviewedAt)}</p>}

          {request.status === "PENDING" && (
            <>
              <Button variant="outline" size="sm" className="self-start rounded-lg text-destructive" onClick={() => setConfirming(true)}>
                Hủy yêu cầu
              </Button>
              <ConfirmDialog
                open={confirming}
                onOpenChange={setConfirming}
                title="Hủy yêu cầu cập nhật?"
                description="Các thay đổi và giấy tờ đính kèm trong yêu cầu này sẽ bị bỏ. Thông tin hồ sơ hiện tại giữ nguyên."
                confirmLabel="Hủy yêu cầu"
                destructive
                onConfirm={async () => {
                  try {
                    await cancelChangeRequest(request.id);
                    await Promise.all([
                      queryClient.invalidateQueries({ queryKey: CHANGE_REQUESTS_KEY }),
                      queryClient.invalidateQueries({ queryKey: AGENT_PROFILE_KEY }),
                    ]);
                    toast.success("Đã hủy yêu cầu cập nhật");
                  } catch (error) {
                    toast.error(errorMessage(error));
                    throw error;
                  }
                }}
              />
            </>
          )}
        </div>
      )}
    </li>
  );
}
