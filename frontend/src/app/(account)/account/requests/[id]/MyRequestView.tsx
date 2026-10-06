"use client";

import { useState } from "react";
import Link from "next/link";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { ArrowLeft, CircleCheck, CreditCard, FilePen, Hourglass, Info, PartyPopper, PenTool, Ticket, XCircle } from "lucide-react";
import { toast } from "sonner";
import { ChatPanel } from "@/components/chat/ChatPanel";
import { ConfirmDialog } from "@/components/common/ConfirmDialog";
import { ReasonDialog } from "@/components/common/ReasonDialog";
import { ProposalSection } from "@/components/customRequest/ProposalSection";
import { RequestStatusBadge } from "@/components/customRequest/RequestBadges";
import { RequestInfo } from "@/components/customRequest/RequestInfo";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { errorMessage } from "@/lib/api/errors";
import { withErrorToast } from "@/lib/withErrorToast";
import { acceptProposal, cancelCustomRequest, getMyCustomRequest, requestProposalRevision } from "@/lib/api/customRequests";
import { CUSTOM_REQUEST_RULES, timeLeftText } from "@/lib/customRequest/labels";
import { formatDateTime } from "@/lib/format";
import { formatPrice } from "@/lib/tour/labels";
import type { CustomRequestDetail } from "@/types/customRequest";
import { MY_REQUESTS_KEY } from "../MyRequestList";

export function MyRequestView({ id }: { id: number }) {
  const key = [...MY_REQUESTS_KEY, id];
  const query = useQuery({ queryKey: key, queryFn: () => getMyCustomRequest(id) });
  const queryClient = useQueryClient();
  const [cancelling, setCancelling] = useState(false);
  const [accepting, setAccepting] = useState(false);
  const [revising, setRevising] = useState(false);

  if (query.isPending) return <Skeleton className="h-96 rounded-2xl" />;
  if (query.isError) return <p className="text-destructive">{errorMessage(query.error)}</p>;
  const r = query.data;
  const latest = r.proposals.at(-1);
  const refresh = (updated: CustomRequestDetail) => {
    queryClient.setQueryData(key, updated);
    void queryClient.invalidateQueries({ queryKey: MY_REQUESTS_KEY });
  };
  // Đề xuất đã đổi (bản mới hơn, hết hạn...) thì tải lại để thấy trạng thái thật
  const reloadOnError = async (action: () => Promise<CustomRequestDetail>) => {
    try {
      refresh(await withErrorToast(action));
    } catch (error) {
      void query.refetch();
      throw error;
    }
  };

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-col gap-3">
        <Link href="/account/requests" className="flex w-fit items-center gap-1.5 text-sm text-muted-foreground hover:text-foreground">
          <ArrowLeft className="size-4" /> Yêu cầu tour riêng
        </Link>
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div>
            <h1 className="flex flex-wrap items-center gap-3 text-2xl font-bold tracking-tight">
              Yêu cầu {r.code}
              <RequestStatusBadge status={r.status} customer />
            </h1>
            <p className="mt-1 text-sm text-muted-foreground">Gửi lúc {formatDateTime(r.createdAt)}</p>
          </div>
          {r.canCancel && (
            <Button variant="outline" className="rounded-xl text-destructive hover:text-destructive" onClick={() => setCancelling(true)}>
              <XCircle /> Hủy yêu cầu
            </Button>
          )}
        </div>
      </div>

      <StatusBanner request={r} />
      <ProposalSection
        request={r}
        actions={
          (r.canAcceptProposal || r.canRequestRevision) && (
            <>
              {r.canRequestRevision && (
                <Button variant="outline" className="rounded-xl" onClick={() => setRevising(true)}>
                  <FilePen /> Yêu cầu chỉnh sửa
                </Button>
              )}
              {r.canAcceptProposal && (
                <Button className="rounded-xl" onClick={() => setAccepting(true)}>
                  <CircleCheck /> Đồng ý đề xuất
                </Button>
              )}
            </>
          )
        }
      />
      <ChatPanel scope="customer" requestId={r.id} />
      <RequestInfo request={r} />

      <ConfirmDialog
        open={cancelling}
        onOpenChange={setCancelling}
        title={`Hủy yêu cầu ${r.code}?`}
        description="Đơn vị tổ chức (nếu đã nhận) sẽ dừng thiết kế tour cho bạn. Bạn có thể gửi yêu cầu mới bất cứ lúc nào."
        confirmLabel="Hủy yêu cầu"
        destructive
        onConfirm={async () => {
          refresh(await withErrorToast(() => cancelCustomRequest(r.id, null)));
          toast.success("Đã hủy yêu cầu");
        }}
      />
      {latest && (
        <>
          <ConfirmDialog
            open={accepting}
            onOpenChange={setAccepting}
            title="Đồng ý với đề xuất này?"
            description={
              <>
                Bạn chốt chuyến đi &quot;{latest.title}&quot; với tổng chi phí <strong>{formatPrice(latest.totalPrice)}</strong>. Tiếp theo,
                đơn vị tổ chức tạo tour riêng để bạn đặt cọc {CUSTOM_REQUEST_RULES.depositPercent}% ({formatPrice(latest.depositAmount)}). Sau
                khi đồng ý, đề xuất không chỉnh sửa thêm được.
              </>
            }
            confirmLabel="Đồng ý"
            onConfirm={async () => {
              await reloadOnError(() => acceptProposal(r.id, latest.id));
              toast.success("Đã đồng ý đề xuất");
            }}
          />
          <ReasonDialog
            open={revising}
            onOpenChange={setRevising}
            title="Yêu cầu chỉnh sửa đề xuất"
            description={`Đơn vị tổ chức sẽ gửi bản mới trong ${CUSTOM_REQUEST_RULES.revisionHours} giờ. Bạn còn ${
              r.maxRevisions - r.revisionCount
            } lượt chỉnh sửa.`}
            label="Bạn muốn chỉnh sửa gì?"
            placeholder="Ví dụ: đổi sang khách sạn 4 sao, bỏ ngày tham quan bảo tàng, giảm giá xuống khoảng 4 triệu/người"
            hint="Càng cụ thể, bản chỉnh sửa càng sát mong muốn của bạn."
            emptyMessage="Vui lòng cho biết bạn muốn chỉnh sửa gì"
            confirmLabel="Gửi yêu cầu"
            onConfirm={async (feedback) => {
              await reloadOnError(() => requestProposalRevision(r.id, latest.id, feedback));
              toast.success("Đã gửi yêu cầu chỉnh sửa");
            }}
          />
        </>
      )}
    </div>
  );
}

/** Giải thích yêu cầu đang ở bước nào và chờ ai. */
function StatusBanner({ request: r }: { request: CustomRequestDetail }) {
  if (r.status === "NEW" || r.status === "WAITING_AGENT") {
    return (
      <Alert className="rounded-2xl border-sky-200 bg-sky-50 dark:border-sky-900 dark:bg-sky-950/40">
        <Hourglass />
        <AlertTitle>TripConnect đang tìm đơn vị tổ chức phù hợp</AlertTitle>
        <AlertDescription>
          Chúng tôi chọn đối tác am hiểu điểm đến và được khách đánh giá tốt. Bạn sẽ nhận thông báo ngay khi có đơn vị nhận yêu cầu.
        </AlertDescription>
      </Alert>
    );
  }
  if (r.status === "IN_PROGRESS") {
    const latest = r.proposals.at(-1);
    if (r.stage === "WAITING_CUSTOMER" && latest) {
      return latest.status === "SENT" ? (
        <Alert className="rounded-2xl border-amber-200 bg-amber-50 dark:border-amber-900 dark:bg-amber-950/40">
          <Hourglass />
          <AlertTitle>
            {r.agentName} đã gửi đề xuất — vui lòng phản hồi trước {formatDateTime(latest.expiresAt)} ({timeLeftText(latest.expiresAt)})
          </AlertTitle>
          <AlertDescription>
            Xem lịch trình và báo giá bên dưới, rồi Đồng ý hoặc Yêu cầu chỉnh sửa
            {r.canRequestRevision ? ` (còn ${r.maxRevisions - r.revisionCount} lượt)` : " (đã hết lượt chỉnh sửa)"}.
          </AlertDescription>
        </Alert>
      ) : (
        <Alert className="rounded-2xl">
          <Info />
          <AlertTitle>Đề xuất đã hết hạn phản hồi</AlertTitle>
          <AlertDescription>
            {r.canRequestRevision ? "Bạn vẫn có thể yêu cầu đơn vị chỉnh sửa và gửi lại. " : "Bạn đã dùng hết lượt chỉnh sửa. "}
            Yêu cầu sẽ tự đóng nếu không có trao đổi thêm trong {CUSTOM_REQUEST_RULES.inactiveCloseDays} ngày kể từ lần gần nhất.
          </AlertDescription>
        </Alert>
      );
    }
    const revising = r.stage === "REVISING";
    return (
      <Alert className="rounded-2xl border-emerald-200 bg-emerald-50 dark:border-emerald-900 dark:bg-emerald-950/40">
        <PenTool />
        <AlertTitle>
          {revising ? `${r.agentName} đang chỉnh sửa đề xuất theo góp ý của bạn` : `${r.agentName} đang thiết kế tour cho bạn`}
        </AlertTitle>
        <AlertDescription>
          {r.proposalDeadline
            ? `${revising ? "Bản chỉnh sửa" : "Đề xuất lịch trình và báo giá"} sẽ được gửi trước ${formatDateTime(r.proposalDeadline)}.`
            : `Nhận yêu cầu lúc ${formatDateTime(r.acceptedAt)}.`}
        </AlertDescription>
      </Alert>
    );
  }
  if (r.status === "AGREED") {
    const awaitingPayment = r.bookingStatus === "PENDING_PAYMENT" || r.bookingStatus === "DEPOSIT_PAID";
    return (
      <Alert className="rounded-2xl border-primary/30 bg-primary/5">
        <PartyPopper />
        <AlertTitle>
          Bạn đã chốt lịch trình với {r.agentName} lúc {formatDateTime(r.agreedAt)}
        </AlertTitle>
        <AlertDescription className="flex flex-col items-start gap-3">
          <span>
            {r.bookingStatus === "PENDING_PAYMENT"
              ? `Đơn đặt tour ${r.bookingCode} đã được tạo. Hãy đặt cọc trong 48 giờ để giữ chuyến đi.`
              : r.bookingStatus === "DEPOSIT_PAID"
                ? `Đã đặt cọc đơn ${r.bookingCode}. Nhớ nhập đủ thông tin người đi và thanh toán phần còn lại trước hạn.`
                : `Đơn đặt tour ${r.bookingCode ?? ""} — theo dõi trong mục Đơn đặt của tôi.`}
          </span>
          {r.bookingId && (
            <Button asChild size="sm" className="rounded-lg">
              <Link href={`/account/bookings/${r.bookingId}`}>
                <CreditCard /> {r.bookingStatus === "PENDING_PAYMENT" ? "Đặt cọc ngay" : awaitingPayment ? "Thanh toán / nhập người đi" : "Xem đơn đặt tour"}
              </Link>
            </Button>
          )}
        </AlertDescription>
      </Alert>
    );
  }
  return (
    <Alert className="rounded-2xl">
      <Info />
      <AlertTitle>
        Yêu cầu {r.status === "CANCELLED" ? "đã hủy" : "đã đóng"} {r.closedAt && `lúc ${formatDateTime(r.closedAt)}`}
      </AlertTitle>
      {(r.closedReason || r.bookingId) && (
        <AlertDescription className="flex flex-col items-start gap-2">
          {r.closedReason && <span>{r.closedReason}</span>}
          {r.bookingId && (
            <Link href={`/account/bookings/${r.bookingId}`} className="flex items-center gap-1 font-medium text-primary hover:underline">
              <Ticket className="size-4" /> Xem đơn {r.bookingCode} (hoàn tiền nếu có)
            </Link>
          )}
        </AlertDescription>
      )}
    </Alert>
  );
}
