"use client";

import { useState } from "react";
import Link from "next/link";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { ArrowLeft, Banknote, Gavel, MessageSquareWarning } from "lucide-react";
import { SettlementBody } from "@/components/settlement/SettlementBody";
import { SettlementStatusBadge } from "@/components/settlement/SettlementTable";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { errorMessage } from "@/lib/api/errors";
import { getSettlement } from "@/lib/api/settlements";
import { formatDateTime } from "@/lib/format";
import { vietQrUrl } from "@/lib/settlement/labels";
import { formatPrice } from "@/lib/tour/labels";
import type { SettlementDetail } from "@/types/settlement";
import { ADMIN_KEY } from "../../adminQueries";
import { ADMIN_SETTLEMENTS_KEY } from "../AdminSettlementList";
import { PayDialog } from "./PayDialog";
import { ResolveDialog } from "./ResolveDialog";

export function AdminSettlementView({ id }: { id: number }) {
  const key = [...ADMIN_SETTLEMENTS_KEY, id];
  const query = useQuery({ queryKey: key, queryFn: () => getSettlement("admin", id) });
  const queryClient = useQueryClient();
  const [paying, setPaying] = useState(false);
  const [resolving, setResolving] = useState(false);

  if (query.isPending) return <Skeleton className="h-96 rounded-2xl" />;
  if (query.isError) return <p className="text-destructive">{errorMessage(query.error)}</p>;
  const d = query.data;
  const s = d.summary;
  const refresh = (updated: SettlementDetail) => {
    queryClient.setQueryData(key, updated);
    void queryClient.invalidateQueries({ queryKey: ADMIN_SETTLEMENTS_KEY });
    void queryClient.invalidateQueries({ queryKey: [...ADMIN_KEY, "summary"] });
  };
  const openDispute = d.disputes.findLast((x) => x.status === "OPEN");
  // Nội dung chuyển khoản không dấu, ngắn — ngân hàng nào cũng nhận
  const transferContent = `TripConnect doi soat ${s.code}`;

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-col gap-3">
        <Link href="/admin/settlements" className="flex w-fit items-center gap-1.5 text-sm text-muted-foreground hover:text-foreground">
          <ArrowLeft className="size-4" /> Đối soát
        </Link>
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div>
            <h1 className="flex flex-wrap items-center gap-3 text-2xl font-bold tracking-tight">
              Đối soát {s.periodLabel}
              <SettlementStatusBadge status={s.status} />
            </h1>
            <p className="mt-1 text-sm text-muted-foreground">
              {s.agentName} · {s.code} · lập {formatDateTime(s.createdAt)}
            </p>
          </div>
          <div className="flex flex-wrap gap-2">
            {d.canResolve && (
              <Button className="rounded-xl" onClick={() => setResolving(true)}>
                <Gavel /> Xử lý khiếu nại
              </Button>
            )}
            {d.canPay && (
              <Button className="rounded-xl" onClick={() => setPaying(true)}>
                <Banknote /> Đã chuyển khoản
              </Button>
            )}
          </div>
        </div>
      </div>

      {openDispute && (
        <Alert className="rounded-2xl border-red-200 bg-red-50 dark:border-red-900 dark:bg-red-950/40">
          <MessageSquareWarning />
          <AlertTitle>Đơn vị khiếu nại lúc {formatDateTime(openDispute.createdAt)}</AlertTitle>
          <AlertDescription className="whitespace-pre-line">{openDispute.reason}</AlertDescription>
        </Alert>
      )}
      {s.status === "AWAITING_PAYMENT" && !d.bankAccount && (
        <Alert className="rounded-2xl">
          <AlertTitle>Đơn vị chưa có tài khoản ngân hàng trong hồ sơ</AlertTitle>
          <AlertDescription>Liên hệ đơn vị cập nhật hồ sơ kinh doanh trước khi chuyển khoản.</AlertDescription>
        </Alert>
      )}

      {s.status === "AWAITING_PAYMENT" && d.bankAccount && (
        <Card className="rounded-2xl">
          <CardHeader>
            <CardTitle className="text-lg">Chuyển khoản cho đơn vị</CardTitle>
            <CardDescription>
              Quét mã bằng ứng dụng ngân hàng: số tiền và nội dung đã điền sẵn. Chuyển xong, bấm &quot;Đã chuyển khoản&quot; và nhập mã giao
              dịch.
            </CardDescription>
          </CardHeader>
          <CardContent className="grid items-center gap-6 sm:grid-cols-[220px_1fr]">
            {/* eslint-disable-next-line @next/next/no-img-element -- ảnh QR do VietQR tạo theo tham số */}
            <img
              src={vietQrUrl(d.bankAccount, s.payoutAmount, transferContent)}
              alt={`Mã VietQR chuyển ${formatPrice(s.payoutAmount)}`}
              className="w-full max-w-[220px] rounded-xl border bg-white"
            />
            <dl className="grid gap-2 text-sm">
              <Row label="Ngân hàng" value={d.bankAccount.bankName} />
              <Row label="Số tài khoản" value={d.bankAccount.accountNumber} />
              <Row label="Chủ tài khoản" value={d.bankAccount.accountHolder} />
              <Row label="Số tiền" value={formatPrice(s.payoutAmount)} />
              <Row label="Nội dung" value={transferContent} />
            </dl>
          </CardContent>
        </Card>
      )}

      <SettlementBody settlement={d} scope="admin" />

      {paying && <PayDialog settlement={d} onClose={() => setPaying(false)} onDone={refresh} />}
      {resolving && <ResolveDialog settlement={d} onClose={() => setResolving(false)} onDone={refresh} />}
    </div>
  );
}

function Row({ label, value }: { label: string; value: string }) {
  return (
    <div className="grid grid-cols-[110px_1fr] gap-2">
      <dt className="text-muted-foreground">{label}</dt>
      <dd className="font-medium break-all">{value}</dd>
    </div>
  );
}
