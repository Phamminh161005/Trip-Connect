"use client";

import Link from "next/link";
import { FileImage, MessageSquareWarning, Receipt } from "lucide-react";
import { InfoList, InfoRow } from "@/components/common/InfoList";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { openTemporaryDocument } from "@/lib/agent/documents";
import { getSettlementReceiptUrl, type SettlementScope } from "@/lib/api/settlements";
import { formatDateTime } from "@/lib/format";
import { DISPUTE_STATUS, SETTLEMENT_ITEM_KIND } from "@/lib/settlement/labels";
import { formatDay, formatPrice } from "@/lib/tour/labels";
import { cn } from "@/lib/utils";
import type { SettlementDetail } from "@/types/settlement";

/** Phần thân bảng đối soát: tổng tiền, từng đơn, điều chỉnh, khiếu nại, thông tin thanh toán. */
export function SettlementBody({ settlement: d, scope }: { settlement: SettlementDetail; scope: SettlementScope }) {
  const s = d.summary;
  const bookingBase = scope === "admin" ? "/admin/bookings" : "/agent/bookings";
  return (
    <div className="flex flex-col gap-6">
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <Stat label={`Khách đã thanh toán (${s.itemCount} đơn)`} value={formatPrice(s.retainedAmount)} />
        <Stat label="Phí nền tảng TripConnect" value={`− ${formatPrice(s.commissionAmount)}`} />
        <Stat
          label="Điều chỉnh"
          value={s.adjustmentAmount === 0 ? "0 VNĐ" : `${s.adjustmentAmount > 0 ? "+" : "−"} ${formatPrice(Math.abs(s.adjustmentAmount))}`}
        />
        <Stat label={scope === "admin" ? "Phải trả đơn vị" : "Bạn nhận"} value={formatPrice(s.payoutAmount)} highlight />
      </div>

      <Card className="rounded-2xl">
        <CardHeader>
          <CardTitle className="text-lg">Các đơn trong kỳ</CardTitle>
          <CardDescription>
            Đơn hoàn thành, và đơn bị hủy mà TripConnect vẫn giữ tiền (khách hủy sát ngày, mất cọc). Phí nền tảng tính theo tỷ lệ
            chụp lại lúc khách đặt.
          </CardDescription>
        </CardHeader>
        <CardContent>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Đơn</TableHead>
                <TableHead className="hidden md:table-cell">Loại</TableHead>
                <TableHead className="hidden lg:table-cell text-right">Đã thu / đã hoàn</TableHead>
                <TableHead className="text-right">Giữ lại</TableHead>
                <TableHead className="hidden sm:table-cell text-right">Phí</TableHead>
                <TableHead className="text-right">Đơn vị nhận</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {d.items.map((i) => (
                <TableRow key={i.id}>
                  <TableCell className="max-w-64">
                    <Link href={`${bookingBase}/${i.bookingId}`} className="font-medium hover:underline">
                      {i.bookingCode}
                    </Link>
                    <p className="truncate text-xs text-muted-foreground">
                      {i.tourTitle} · đi {formatDay(i.startDate)} · {i.customerName}
                    </p>
                  </TableCell>
                  <TableCell className="hidden md:table-cell">
                    <Badge variant={i.kind === "COMPLETED" ? "secondary" : "outline"}>{SETTLEMENT_ITEM_KIND[i.kind]}</Badge>
                    <p className="mt-1 text-xs text-muted-foreground">{formatDateTime(i.eventAt)}</p>
                  </TableCell>
                  <TableCell className="hidden text-right text-xs tabular-nums lg:table-cell">
                    {formatPrice(i.paidAmount)}
                    {i.refundedAmount > 0 && <span className="block text-muted-foreground">− {formatPrice(i.refundedAmount)}</span>}
                  </TableCell>
                  <TableCell className="text-right tabular-nums">{formatPrice(i.retainedAmount)}</TableCell>
                  <TableCell className="hidden text-right tabular-nums text-muted-foreground sm:table-cell">
                    {formatPrice(i.commissionAmount)} ({Math.round(i.commissionRate * 100)}%)
                  </TableCell>
                  <TableCell className="text-right font-semibold tabular-nums">{formatPrice(i.payoutAmount)}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </CardContent>
      </Card>

      {d.adjustments.length > 0 && (
        <Card className="rounded-2xl">
          <CardHeader>
            <CardTitle className="text-lg">Khoản điều chỉnh</CardTitle>
          </CardHeader>
          <CardContent className="flex flex-col gap-2">
            {d.adjustments.map((a) => (
              <div key={a.id} className="flex flex-wrap items-start justify-between gap-3 rounded-xl border p-3 text-sm">
                <div>
                  <p>{a.reason}</p>
                  <p className="text-xs text-muted-foreground">
                    {a.createdByName} · {formatDateTime(a.createdAt)}
                  </p>
                </div>
                <span className={cn("font-semibold tabular-nums", a.amount > 0 ? "text-emerald-700" : "text-red-700")}>
                  {a.amount > 0 ? "+" : "−"} {formatPrice(Math.abs(a.amount))}
                </span>
              </div>
            ))}
          </CardContent>
        </Card>
      )}

      {d.disputes.length > 0 && (
        <Card className="rounded-2xl">
          <CardHeader>
            <CardTitle className="flex items-center gap-2 text-lg">
              <MessageSquareWarning className="size-5 text-primary" /> Khiếu nại
            </CardTitle>
          </CardHeader>
          <CardContent className="flex flex-col gap-3">
            {d.disputes.map((x) => (
              <div key={x.id} className="flex flex-col gap-2 rounded-xl border p-3 text-sm">
                <div className="flex flex-wrap items-center gap-2">
                  <Badge variant="outline">{DISPUTE_STATUS[x.status]}</Badge>
                  <span className="text-xs text-muted-foreground">gửi {formatDateTime(x.createdAt)}</span>
                </div>
                <p className="whitespace-pre-line">{x.reason}</p>
                {x.resolution && (
                  <div className="rounded-lg bg-muted/60 p-2.5">
                    <p className="text-xs font-medium text-muted-foreground">
                      TripConnect trả lời{x.resolvedByName && ` (${x.resolvedByName})`}
                      {x.resolvedAt && ` · ${formatDateTime(x.resolvedAt)}`}
                    </p>
                    <p className="whitespace-pre-line">{x.resolution}</p>
                  </div>
                )}
              </div>
            ))}
          </CardContent>
        </Card>
      )}

      <Card className="rounded-2xl">
        <CardHeader>
          <CardTitle className="flex items-center gap-2 text-lg">
            <Receipt className="size-5 text-primary" /> Thanh toán
          </CardTitle>
        </CardHeader>
        <CardContent>
          <InfoList>
            <InfoRow
              label={s.status === "PAID" ? "Tài khoản đã nhận" : "Tài khoản nhận tiền"}
              value={
                d.bankAccount
                  ? `${d.bankAccount.bankName} · ${d.bankAccount.accountNumber} · ${d.bankAccount.accountHolder}`
                  : "Chưa có tài khoản ngân hàng trong hồ sơ"
              }
            />
            {d.confirmedAt && (
              <InfoRow label="Xác nhận" value={`${formatDateTime(d.confirmedAt)}${d.autoConfirmed ? " (tự xác nhận vì quá hạn)" : ""}`} />
            )}
            {s.paidAt && <InfoRow label="Thanh toán lúc" value={`${formatDateTime(s.paidAt)}${d.paidByName ? ` · ${d.paidByName}` : ""}`} />}
            {d.transactionRef && <InfoRow label="Mã giao dịch" value={d.transactionRef} />}
            {d.hasReceipt && (
              <InfoRow
                label="Biên lai"
                value={
                  <Button
                    variant="outline"
                    size="sm"
                    className="rounded-lg"
                    onClick={() => openTemporaryDocument(() => getSettlementReceiptUrl(scope, s.id))}
                  >
                    <FileImage /> Xem biên lai
                  </Button>
                }
              />
            )}
          </InfoList>
        </CardContent>
      </Card>
    </div>
  );
}

function Stat({ label, value, highlight }: { label: string; value: string; highlight?: boolean }) {
  return (
    <div className={cn("rounded-2xl border bg-card p-4", highlight && "border-primary/40 bg-primary/5")}>
      <p className="text-xs text-muted-foreground">{label}</p>
      <p className={cn("mt-1 text-lg font-bold tabular-nums", highlight && "text-primary")}>{value}</p>
    </div>
  );
}
