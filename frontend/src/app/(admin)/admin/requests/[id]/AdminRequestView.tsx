"use client";

import { useState } from "react";
import Link from "next/link";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { ArrowLeft, Lock, Sparkles, Star, UserCheck } from "lucide-react";
import { toast } from "sonner";
import { ConfirmDialog } from "@/components/common/ConfirmDialog";
import { InfoList, InfoRow } from "@/components/common/InfoList";
import { ReasonDialog } from "@/components/common/ReasonDialog";
import { AssignmentStatusBadge, RequestStatusBadge } from "@/components/customRequest/RequestBadges";
import { ProposalSection } from "@/components/customRequest/ProposalSection";
import { RequestInfo } from "@/components/customRequest/RequestInfo";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { errorMessage } from "@/lib/api/errors";
import { withErrorToast } from "@/lib/withErrorToast";
import { assignCustomRequest, closeCustomRequest, getAdminCustomRequest, getRequestCandidates } from "@/lib/api/customRequests";
import { CUSTOM_REQUEST_RULES, timeLeftText } from "@/lib/customRequest/labels";
import { formatDateTime, formatRelative } from "@/lib/format";
import type { AgentCandidate, CustomRequestDetail } from "@/types/customRequest";
import { ADMIN_REQUESTS_KEY } from "../AdminRequestList";
import { ADMIN_KEY } from "../../adminQueries";

export function AdminRequestView({ id }: { id: number }) {
  const key = [...ADMIN_REQUESTS_KEY, id];
  const query = useQuery({ queryKey: key, queryFn: () => getAdminCustomRequest(id) });
  const queryClient = useQueryClient();
  const [closing, setClosing] = useState(false);

  if (query.isPending) return <Skeleton className="h-96 rounded-2xl" />;
  if (query.isError) return <p className="text-destructive">{errorMessage(query.error)}</p>;
  const r = query.data;
  const refresh = (updated: CustomRequestDetail) => {
    queryClient.setQueryData(key, updated);
    void queryClient.invalidateQueries({ queryKey: ADMIN_REQUESTS_KEY });
    void queryClient.invalidateQueries({ queryKey: [...ADMIN_KEY, "summary"] });
  };

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-col gap-3">
        <Link href="/admin/requests" className="flex w-fit items-center gap-1.5 text-sm text-muted-foreground hover:text-foreground">
          <ArrowLeft className="size-4" /> Yêu cầu tour riêng
        </Link>
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div>
            <h1 className="flex flex-wrap items-center gap-3 text-2xl font-bold tracking-tight">
              Yêu cầu {r.code}
              <RequestStatusBadge status={r.status} />
            </h1>
            <p className="mt-1 text-sm text-muted-foreground">Gửi lúc {formatDateTime(r.createdAt)}</p>
          </div>
          {r.canCancel && (
            <Button variant="outline" className="rounded-xl text-destructive hover:text-destructive" onClick={() => setClosing(true)}>
              <Lock /> Đóng yêu cầu
            </Button>
          )}
        </div>
      </div>

      <div className="grid gap-6 lg:grid-cols-[1fr_320px]">
        <RequestInfo request={r} />
        <Card className="rounded-2xl">
          <CardHeader>
            <CardTitle className="text-lg">Khách hàng</CardTitle>
          </CardHeader>
          <CardContent>
            <InfoList>
              <InfoRow label="Họ và tên" value={r.customerName} />
              <InfoRow label="Email" value={r.customerEmail ?? "—"} />
              <InfoRow label="Số điện thoại" value={r.customerPhone ?? "—"} />
              {r.agentName && <InfoRow label="Đơn vị phụ trách" value={r.agentName} />}
              {r.closedReason && <InfoRow label="Lý do đóng" value={r.closedReason} />}
            </InfoList>
          </CardContent>
        </Card>
      </div>

      {r.status === "NEW" && <CandidatesCard request={r} onAssigned={refresh} />}
      <ProposalSection request={r} />
      {r.status === "IN_PROGRESS" && r.proposalDeadline && (
        <p className="text-sm text-muted-foreground">
          {r.stage === "REVISING" ? "Hạn gửi bản chỉnh sửa" : "Hạn gửi đề xuất"}: {formatDateTime(r.proposalDeadline)} (
          {timeLeftText(r.proposalDeadline)})
        </p>
      )}
      {r.assignments.length > 0 && <HistoryCard request={r} />}

      <ReasonDialog
        open={closing}
        onOpenChange={setClosing}
        title={`Đóng yêu cầu ${r.code}?`}
        description="Yêu cầu kết thúc, đơn vị đang được giao (nếu có) dừng xử lý. Khách nhận thông báo và email kèm lý do."
        label="Lý do đóng"
        placeholder="Ví dụ: chưa có đối tác phục vụ điểm đến này, yêu cầu không khả thi với ngân sách"
        confirmLabel="Đóng yêu cầu"
        onConfirm={async (reason) => {
          refresh(await withErrorToast(() => closeCustomRequest(r.id, reason)));
          toast.success("Đã đóng yêu cầu");
        }}
      />
    </div>
  );
}

function CandidatesCard({ request, onAssigned }: { request: CustomRequestDetail; onAssigned: (r: CustomRequestDetail) => void }) {
  const candidates = useQuery({ queryKey: ["admin", "custom-requests", request.id, "candidates"], queryFn: () => getRequestCandidates(request.id) });
  const [chosen, setChosen] = useState<AgentCandidate | null>(null);

  return (
    <Card className="rounded-2xl">
      <CardHeader>
        <CardTitle className="flex items-center gap-2 text-lg">
          <Sparkles className="size-5 text-primary" /> Đơn vị gợi ý
        </CardTitle>
        <CardDescription>
          Điểm phù hợp = 60% khớp khu vực phục vụ (đúng nơi 100, cùng quốc gia 60) + 40% điểm đánh giá (chưa có đánh giá tính 60). Chỉ
          gồm đơn vị đang nhận yêu cầu, chưa quá số yêu cầu đang mở và chưa từng được giao yêu cầu này.
        </CardDescription>
      </CardHeader>
      <CardContent>
        {candidates.isPending ? (
          <Skeleton className="h-32 rounded-xl" />
        ) : candidates.isError ? (
          <p className="text-sm text-destructive">{errorMessage(candidates.error)}</p>
        ) : candidates.data.length === 0 ? (
          <p className="rounded-xl border border-dashed p-6 text-center text-sm text-muted-foreground">
            Hiện không có đơn vị nào đủ điều kiện nhận yêu cầu. Có thể chờ đơn vị bật lại nhận yêu cầu, hoặc đóng yêu cầu kèm lý do.
          </p>
        ) : (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Đơn vị</TableHead>
                <TableHead className="text-right">Điểm</TableHead>
                <TableHead className="hidden md:table-cell">Khu vực khớp</TableHead>
                <TableHead className="hidden lg:table-cell">Đang xử lý</TableHead>
                <TableHead />
              </TableRow>
            </TableHeader>
            <TableBody>
              {candidates.data.map((c) => (
                <TableRow key={c.agentId} className={c.recommended ? "bg-primary/5" : undefined}>
                  <TableCell>
                    <div className="flex flex-wrap items-center gap-2">
                      <Link href={`/admin/agents/${c.agentProfileId}`} className="font-medium hover:underline">
                        {c.companyName}
                      </Link>
                      {c.recommended && <Badge className="border-0 bg-primary text-primary-foreground">Gợi ý</Badge>}
                    </div>
                    <p className="flex items-center gap-1 text-xs text-muted-foreground">
                      <Star className="size-3 fill-amber-400 text-amber-400" />
                      {c.rating !== null ? `${c.rating.toFixed(1)} (${c.ratingCount})` : "Chưa có đánh giá"}
                      {c.lastLoginAt && ` · đăng nhập ${formatRelative(c.lastLoginAt).toLowerCase()}`}
                    </p>
                  </TableCell>
                  <TableCell className="text-right">
                    <p className="font-semibold tabular-nums">{c.totalScore.toFixed(1)}</p>
                    <p className="text-xs text-muted-foreground tabular-nums">
                      KV {c.locationScore.toFixed(0)} · ĐG {c.ratingScore.toFixed(0)}
                    </p>
                  </TableCell>
                  <TableCell className="hidden max-w-56 text-sm md:table-cell">{c.matchedAreas.join(", ") || "—"}</TableCell>
                  <TableCell className="hidden tabular-nums lg:table-cell">
                    {c.openRequests}/{c.maxOpenRequests}
                  </TableCell>
                  <TableCell className="text-right">
                    <Button size="sm" variant={c.recommended ? "default" : "outline"} className="rounded-lg" onClick={() => setChosen(c)}>
                      <UserCheck /> Giao
                    </Button>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}
      </CardContent>
      <ConfirmDialog
        open={chosen !== null}
        onOpenChange={(open) => !open && setChosen(null)}
        title={`Giao yêu cầu cho ${chosen?.companyName ?? ""}?`}
        description={`Đơn vị nhận thông báo và email, có ${CUSTOM_REQUEST_RULES.acceptHours} giờ để nhận hoặc từ chối.`}
        confirmLabel="Giao yêu cầu"
        onConfirm={async () => {
          if (!chosen) return;
          onAssigned(await withErrorToast(() => assignCustomRequest(request.id, chosen.agentId)));
          toast.success(`Đã giao cho ${chosen.companyName}`);
        }}
      />
    </Card>
  );
}

function HistoryCard({ request }: { request: CustomRequestDetail }) {
  return (
    <Card className="rounded-2xl">
      <CardHeader>
        <CardTitle className="text-lg">Lịch sử giao việc</CardTitle>
      </CardHeader>
      <CardContent>
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Đơn vị</TableHead>
              <TableHead>Kết quả</TableHead>
              <TableHead className="hidden md:table-cell">Giao lúc</TableHead>
              <TableHead className="hidden md:table-cell">Phản hồi</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {request.assignments.map((a) => (
              <TableRow key={a.id}>
                <TableCell>
                  <p className="font-medium">{a.agentName}</p>
                  <p className="text-xs text-muted-foreground">
                    Điểm {a.matchScore?.toFixed(1) ?? "—"} · giao bởi {a.assignedByName}
                  </p>
                </TableCell>
                <TableCell>
                  <AssignmentStatusBadge status={a.status} />
                  {a.status === "PENDING" && <p className="mt-1 text-xs text-amber-700">{timeLeftText(a.deadline)}</p>}
                  {a.declineReason && <p className="mt-1 max-w-64 text-xs whitespace-normal text-muted-foreground">{a.declineReason}</p>}
                </TableCell>
                <TableCell className="hidden md:table-cell">{formatDateTime(a.createdAt)}</TableCell>
                <TableCell className="hidden md:table-cell">{a.respondedAt ? formatDateTime(a.respondedAt) : "—"}</TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </CardContent>
    </Card>
  );
}
