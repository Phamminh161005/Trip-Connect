"use client";

import { useState } from "react";
import Link from "next/link";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { ArrowLeft, CheckCircle2, Hourglass, MessageSquareWarning } from "lucide-react";
import { toast } from "sonner";
import { ConfirmDialog } from "@/components/common/ConfirmDialog";
import { ReasonDialog } from "@/components/common/ReasonDialog";
import { SettlementBody } from "@/components/settlement/SettlementBody";
import { SettlementStatusBadge } from "@/components/settlement/SettlementTable";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { errorMessage } from "@/lib/api/errors";
import { confirmSettlement, disputeSettlement, getSettlement } from "@/lib/api/settlements";
import { timeLeftText } from "@/lib/customRequest/labels";
import { formatDateTime } from "@/lib/format";
import { periodTitle } from "@/lib/settlement/labels";
import { formatPrice } from "@/lib/tour/labels";
import { withErrorToast } from "@/lib/withErrorToast";
import type { SettlementDetail } from "@/types/settlement";
import { AGENT_SETTLEMENTS_KEY } from "../AgentSettlementList";

export function AgentSettlementView({ id }: { id: number }) {
  const key = [...AGENT_SETTLEMENTS_KEY, id];
  const query = useQuery({ queryKey: key, queryFn: () => getSettlement("agent", id) });
  const queryClient = useQueryClient();
  const [confirming, setConfirming] = useState(false);
  const [disputing, setDisputing] = useState(false);

  if (query.isPending) return <Skeleton className="h-96 rounded-2xl" />;
  if (query.isError) return <p className="text-destructive">{errorMessage(query.error)}</p>;
  const d = query.data;
  const s = d.summary;
  const refresh = (updated: SettlementDetail) => {
    queryClient.setQueryData(key, updated);
    void queryClient.invalidateQueries({ queryKey: AGENT_SETTLEMENTS_KEY });
  };

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-col gap-3">
        <Link href="/agent/settlements" className="flex w-fit items-center gap-1.5 text-sm text-muted-foreground hover:text-foreground">
          <ArrowLeft className="size-4" /> Đối soát
        </Link>
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div>
            <h1 className="flex flex-wrap items-center gap-3 text-2xl font-bold tracking-tight">
              Đối soát {s.periodLabel}
              <SettlementStatusBadge status={s.status} agent />
            </h1>
            <p className="mt-1 text-sm text-muted-foreground">
              {s.code} · lập {formatDateTime(s.createdAt)}
            </p>
          </div>
          {(d.canConfirm || d.canDispute) && (
            <div className="flex flex-wrap gap-2">
              {d.canDispute && (
                <Button variant="outline" className="rounded-xl" onClick={() => setDisputing(true)}>
                  <MessageSquareWarning /> Khiếu nại
                </Button>
              )}
              {d.canConfirm && (
                <Button className="rounded-xl" onClick={() => setConfirming(true)}>
                  <CheckCircle2 /> Xác nhận đúng
                </Button>
              )}
            </div>
          )}
        </div>
      </div>

      {s.status === "PENDING_CONFIRM" && s.confirmDeadline && (
        <Alert className="rounded-2xl border-amber-200 bg-amber-50 dark:border-amber-900 dark:bg-amber-950/40">
          <Hourglass />
          <AlertTitle>
            Vui lòng kiểm tra trước {formatDateTime(s.confirmDeadline)} ({timeLeftText(s.confirmDeadline)})
          </AlertTitle>
          <AlertDescription>
            Thấy đúng thì bấm Xác nhận; có đơn sai hoặc bị sót thì bấm Khiếu nại. Quá hạn, bảng đối soát được tự xác nhận.
          </AlertDescription>
        </Alert>
      )}
      {s.status === "DISPUTED" && (
        <Alert className="rounded-2xl">
          <MessageSquareWarning />
          <AlertTitle>TripConnect đang xem xét khiếu nại của bạn</AlertTitle>
          <AlertDescription>Bạn sẽ nhận thông báo khi khiếu nại được xử lý.</AlertDescription>
        </Alert>
      )}
      {s.status === "AWAITING_PAYMENT" && (
        <Alert className="rounded-2xl border-sky-200 bg-sky-50 dark:border-sky-900 dark:bg-sky-950/40">
          <Hourglass />
          <AlertTitle>Chờ TripConnect chuyển {formatPrice(s.payoutAmount)}</AlertTitle>
          <AlertDescription>Tiền được chuyển vào tài khoản ngân hàng trong hồ sơ kinh doanh của bạn.</AlertDescription>
        </Alert>
      )}
      {s.status === "PAID" && (
        <Alert className="rounded-2xl border-emerald-200 bg-emerald-50 dark:border-emerald-900 dark:bg-emerald-950/40">
          <CheckCircle2 />
          <AlertTitle>TripConnect đã chuyển {formatPrice(s.payoutAmount)}</AlertTitle>
          <AlertDescription>
            {s.paidAt && `Lúc ${formatDateTime(s.paidAt)}`}
            {d.transactionRef && ` · mã giao dịch ${d.transactionRef}`}
          </AlertDescription>
        </Alert>
      )}

      <SettlementBody settlement={d} scope="agent" />

      <ConfirmDialog
        open={confirming}
        onOpenChange={setConfirming}
        title={`Xác nhận ${periodTitle(s.periodLabel).toLowerCase()} đúng?`}
        description={`Bạn đồng ý nhận ${formatPrice(s.payoutAmount)} cho ${s.itemCount} đơn. Sau khi xác nhận không khiếu nại được nữa.`}
        confirmLabel="Xác nhận"
        onConfirm={async () => {
          refresh(await withErrorToast(() => confirmSettlement(s.id)));
          toast.success("Đã xác nhận, chờ TripConnect thanh toán");
        }}
      />
      <ReasonDialog
        open={disputing}
        onOpenChange={setDisputing}
        title="Khiếu nại bảng đối soát"
        description="Ghi rõ mã đơn bị sai / bị sót và số tiền bạn cho là đúng. TripConnect sẽ kiểm tra và trả lời."
        label="Nội dung khiếu nại"
        placeholder="Ví dụ: Đơn TC261005123456 khách hủy sát ngày nhưng chưa có trong bảng; đơn TC... tính sai số khách"
        confirmLabel="Gửi khiếu nại"
        hint="Trong lúc xử lý, bảng đối soát chưa được thanh toán."
        emptyMessage="Vui lòng ghi nội dung khiếu nại"
        onConfirm={async (reason) => {
          refresh(await withErrorToast(() => disputeSettlement(s.id, reason)));
          toast.success("Đã gửi khiếu nại");
        }}
      />
    </div>
  );
}
