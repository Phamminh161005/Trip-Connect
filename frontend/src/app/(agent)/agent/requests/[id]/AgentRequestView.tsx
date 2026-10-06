"use client";

import { useState } from "react";
import Link from "next/link";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { ArrowLeft, Ban, Check, FilePen, Hourglass, Info, PartyPopper, PenTool, Send, Ticket, X } from "lucide-react";
import { toast } from "sonner";
import { ChatPanel } from "@/components/chat/ChatPanel";
import { ConfirmDialog } from "@/components/common/ConfirmDialog";
import { ReasonDialog } from "@/components/common/ReasonDialog";
import { ProposalSection } from "@/components/customRequest/ProposalSection";
import { AssignmentStatusBadge } from "@/components/customRequest/RequestBadges";
import { RequestInfo } from "@/components/customRequest/RequestInfo";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { errorMessage } from "@/lib/api/errors";
import { withErrorToast } from "@/lib/withErrorToast";
import { acceptCustomRequest, cancelPrivateTrip, declineCustomRequest, getAgentCustomRequest } from "@/lib/api/customRequests";
import { BOOKING_STATUS } from "@/lib/booking/labels";
import { CUSTOM_REQUEST_RULES, timeLeftText } from "@/lib/customRequest/labels";
import { formatDateTime } from "@/lib/format";
import { AGENT_REQUESTS_KEY } from "../AgentRequestList";

export function AgentRequestView({ id }: { id: number }) {
  const key = [...AGENT_REQUESTS_KEY, id];
  const query = useQuery({ queryKey: key, queryFn: () => getAgentCustomRequest(id) });
  const queryClient = useQueryClient();
  const [accepting, setAccepting] = useState(false);
  const [declining, setDeclining] = useState(false);
  const [cancellingTrip, setCancellingTrip] = useState(false);

  if (query.isPending) return <Skeleton className="h-96 rounded-2xl" />;
  if (query.isError) return <p className="text-destructive">{errorMessage(query.error)}</p>;
  const r = query.data;
  const refresh = (updated: typeof r) => {
    queryClient.setQueryData(key, updated);
    void queryClient.invalidateQueries({ queryKey: AGENT_REQUESTS_KEY });
  };

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-col gap-3">
        <Link href="/agent/requests" className="flex w-fit items-center gap-1.5 text-sm text-muted-foreground hover:text-foreground">
          <ArrowLeft className="size-4" /> Yêu cầu tư vấn
        </Link>
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div>
            <h1 className="flex flex-wrap items-center gap-3 text-2xl font-bold tracking-tight">
              Yêu cầu {r.code}
              {r.myAssignmentStatus && <AssignmentStatusBadge status={r.myAssignmentStatus} />}
            </h1>
            <p className="mt-1 text-sm text-muted-foreground">
              Khách {r.customerName} · gửi {formatDateTime(r.createdAt)}
            </p>
          </div>
          {r.canPropose && (
            <Button asChild className="rounded-xl">
              <Link href={`/agent/requests/${r.id}/propose`}>
                {r.stage === "REVISING" ? <FilePen /> : <Send />}
                {r.stage === "REVISING" ? "Gửi bản chỉnh sửa" : "Gửi đề xuất"}
              </Link>
            </Button>
          )}
          {r.canRespond && (
            <div className="flex flex-wrap gap-2">
              <Button variant="outline" className="rounded-xl text-destructive hover:text-destructive" onClick={() => setDeclining(true)}>
                <X /> Từ chối
              </Button>
              <Button className="rounded-xl" onClick={() => setAccepting(true)}>
                <Check /> Nhận yêu cầu
              </Button>
            </div>
          )}
        </div>
      </div>

      {r.canRespond && r.myAssignmentDeadline && (
        <Alert className="rounded-2xl border-amber-200 bg-amber-50 dark:border-amber-900 dark:bg-amber-950/40">
          <Hourglass />
          <AlertTitle>Phản hồi trước {formatDateTime(r.myAssignmentDeadline)} ({timeLeftText(r.myAssignmentDeadline)})</AlertTitle>
          <AlertDescription>
            Nhận nếu bạn tổ chức được chuyến đi này. Sau khi nhận, bạn có {CUSTOM_REQUEST_RULES.proposalHours} giờ để gửi đề xuất lịch
            trình và báo giá cho khách.
          </AlertDescription>
        </Alert>
      )}
      {r.canPropose && r.proposalDeadline && (
        <Alert className="rounded-2xl border-amber-200 bg-amber-50 dark:border-amber-900 dark:bg-amber-950/40">
          <PenTool />
          <AlertTitle>
            {r.stage === "REVISING" ? "Khách yêu cầu chỉnh sửa — gửi bản mới" : "Gửi đề xuất lịch trình và báo giá"} trước{" "}
            {formatDateTime(r.proposalDeadline)} ({timeLeftText(r.proposalDeadline)})
          </AlertTitle>
          <AlertDescription>
            {r.stage === "REVISING"
              ? `Lần chỉnh sửa ${r.revisionCount}/${r.maxRevisions}. Xem góp ý của khách ở đề xuất bên dưới.`
              : `Bạn đã nhận yêu cầu lúc ${formatDateTime(r.acceptedAt)}.`}{" "}
            Quá hạn, yêu cầu sẽ được chuyển cho đơn vị khác.
          </AlertDescription>
        </Alert>
      )}
      {r.myAssignmentStatus === "ACCEPTED" && r.stage === "WAITING_CUSTOMER" && (
        <Alert className="rounded-2xl border-emerald-200 bg-emerald-50 dark:border-emerald-900 dark:bg-emerald-950/40">
          <Hourglass />
          <AlertTitle>Đang chờ khách phản hồi đề xuất</AlertTitle>
          <AlertDescription>
            Khách có {CUSTOM_REQUEST_RULES.responseDays} ngày để đồng ý hoặc yêu cầu chỉnh sửa. Bạn sẽ nhận thông báo khi khách phản hồi.
          </AlertDescription>
        </Alert>
      )}
      {r.myAssignmentStatus === "ACCEPTED" && r.status === "AGREED" && (
        <Alert className="rounded-2xl border-primary/30 bg-primary/5">
          <PartyPopper />
          <AlertTitle>Khách đã đồng ý đề xuất lúc {formatDateTime(r.agreedAt)}</AlertTitle>
          <AlertDescription className="flex flex-col items-start gap-3">
            <span>
              Hệ thống đã tạo tour riêng và đơn {r.bookingCode}
              {r.bookingStatus && ` (${BOOKING_STATUS[r.bookingStatus].label.toLowerCase()})`}. Khách đặt cọc{" "}
              {CUSTOM_REQUEST_RULES.depositPercent}% rồi trả phần còn lại trước ngày đi; bạn nhận thông báo mỗi khi khách thanh toán.
            </span>
            <div className="flex flex-wrap gap-2">
              {r.bookingId && (
                <Button asChild size="sm" className="rounded-lg">
                  <Link href={`/agent/bookings/${r.bookingId}`}>
                    <Ticket /> Xem đơn đặt tour
                  </Link>
                </Button>
              )}
              <Button
                size="sm"
                variant="outline"
                className="rounded-lg text-destructive hover:text-destructive"
                onClick={() => setCancellingTrip(true)}
              >
                <Ban /> Hủy chuyến
              </Button>
            </div>
          </AlertDescription>
        </Alert>
      )}
      {(r.myAssignmentStatus === "EXPIRED" ||
        r.myAssignmentStatus === "REVOKED" ||
        r.myAssignmentStatus === "OVERDUE" ||
        r.status === "CANCELLED" ||
        r.status === "CLOSED") && (
        <Alert className="rounded-2xl">
          <Info />
          <AlertTitle>
            {r.myAssignmentStatus === "EXPIRED"
              ? "Bạn đã không phản hồi kịp — yêu cầu được chuyển cho đơn vị khác"
              : r.myAssignmentStatus === "OVERDUE"
                ? "Bạn đã quá hạn gửi đề xuất — yêu cầu được chuyển cho đơn vị khác"
                : "Yêu cầu đã hủy hoặc đóng"}
          </AlertTitle>
          {r.myAssignmentStatus === "ACCEPTED" && r.closedReason && <AlertDescription>{r.closedReason}</AlertDescription>}
        </Alert>
      )}

      {r.myAssignmentStatus !== "PENDING" && (
        <ProposalSection
          request={r}
          emptyText={r.canPropose ? "Bạn chưa gửi đề xuất nào cho yêu cầu này." : undefined}
        />
      )}
      {r.myAssignmentStatus === "ACCEPTED" && <ChatPanel scope="agent" requestId={r.id} />}
      <RequestInfo request={r} />

      <ConfirmDialog
        open={accepting}
        onOpenChange={setAccepting}
        title="Nhận yêu cầu này?"
        description={`Bạn cam kết gửi đề xuất lịch trình và báo giá cho khách trong ${CUSTOM_REQUEST_RULES.proposalHours} giờ.`}
        confirmLabel="Nhận yêu cầu"
        onConfirm={async () => {
          refresh(await withErrorToast(() => acceptCustomRequest(r.id)));
          toast.success("Đã nhận yêu cầu");
        }}
      />
      <ReasonDialog
        open={declining}
        onOpenChange={setDeclining}
        title="Từ chối yêu cầu?"
        description="Yêu cầu sẽ được chuyển lại để TripConnect giao cho đơn vị khác."
        label="Lý do từ chối"
        placeholder="Ví dụ: chưa có đối tác tại điểm đến, lịch đã kín vào thời gian này"
        confirmLabel="Từ chối"
        hint="Lý do giúp TripConnect giao yêu cầu phù hợp hơn; khách không thấy nội dung này."
        onConfirm={async (reason) => {
          refresh(await withErrorToast(() => declineCustomRequest(r.id, reason)));
          toast.success("Đã từ chối yêu cầu");
        }}
      />
      <ReasonDialog
        open={cancellingTrip}
        onOpenChange={setCancellingTrip}
        title="Hủy chuyến đi đã chốt?"
        description="Đơn của khách bị hủy, khách được hoàn 100% số tiền đã trả và nhận email thông báo. Yêu cầu sẽ đóng lại."
        label="Lý do hủy chuyến"
        placeholder="Ví dụ: đường lên Hà Giang bị sạt lở, không đảm bảo an toàn cho đoàn"
        confirmLabel="Hủy chuyến"
        hint="Lý do được gửi kèm trong email tới khách."
        onConfirm={async (reason) => {
          refresh(await withErrorToast(() => cancelPrivateTrip(r.id, reason)));
          toast.success("Đã hủy chuyến, khách sẽ được hoàn tiền");
        }}
      />
    </div>
  );
}
