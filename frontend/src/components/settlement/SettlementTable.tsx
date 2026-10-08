"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { Badge } from "@/components/ui/badge";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { SETTLEMENT_STATUS, periodTitle } from "@/lib/settlement/labels";
import { formatDateTime } from "@/lib/format";
import { formatPrice } from "@/lib/tour/labels";
import { cn } from "@/lib/utils";
import type { SettlementSummary } from "@/types/settlement";

export function SettlementStatusBadge({ status, agent = false }: { status: SettlementSummary["status"]; agent?: boolean }) {
  const s = SETTLEMENT_STATUS[status];
  return <Badge className={cn("border-0", s.className)}>{agent ? s.agentLabel : s.label}</Badge>;
}

/** Danh sách bảng đối soát (Agent / Admin). Bấm dòng để mở chi tiết. */
export function SettlementTable({ rows, basePath, admin, dimmed }: { rows: SettlementSummary[]; basePath: string; admin: boolean; dimmed?: boolean }) {
  const router = useRouter();
  return (
    <Table className={dimmed ? "opacity-60" : undefined}>
      <TableHeader>
        <TableRow>
          <TableHead>Kỳ đối soát</TableHead>
          {admin && <TableHead className="hidden md:table-cell">Đơn vị</TableHead>}
          <TableHead className="hidden sm:table-cell text-right">Số đơn</TableHead>
          <TableHead className="text-right">{admin ? "Phải trả" : "Bạn nhận"}</TableHead>
          <TableHead>Trạng thái</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {rows.map((s) => {
          const href = `${basePath}/${s.id}`;
          return (
            <TableRow key={s.id} className="cursor-pointer" onClick={() => router.push(href)}>
              <TableCell>
                <Link href={href} className="font-medium hover:underline" onClick={(e) => e.stopPropagation()}>
                  {periodTitle(s.periodLabel)}
                </Link>
                <p className="text-xs text-muted-foreground">
                  {s.code} · lập {formatDateTime(s.createdAt)}
                </p>
              </TableCell>
              {admin && <TableCell className="hidden md:table-cell">{s.agentName}</TableCell>}
              <TableCell className="hidden text-right tabular-nums sm:table-cell">{s.itemCount}</TableCell>
              <TableCell className="text-right font-semibold tabular-nums">{formatPrice(s.payoutAmount)}</TableCell>
              <TableCell>
                <div className="flex flex-col items-start gap-1">
                  <SettlementStatusBadge status={s.status} agent={!admin} />
                  {s.status === "PENDING_CONFIRM" && s.confirmDeadline && (
                    <span className="text-xs text-muted-foreground">hạn {formatDateTime(s.confirmDeadline)}</span>
                  )}
                  {s.paidAt && <span className="text-xs text-muted-foreground">trả {formatDateTime(s.paidAt)}</span>}
                </div>
              </TableCell>
            </TableRow>
          );
        })}
      </TableBody>
    </Table>
  );
}
