"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { travellersText } from "@/lib/booking/labels";
import { STAGE_LABEL, budgetText, destinationsText, placeName, startWindowText, timeLeftText } from "@/lib/customRequest/labels";
import { formatDateTime } from "@/lib/format";
import { cn } from "@/lib/utils";
import type { CustomRequestSummary } from "@/types/customRequest";
import { AssignmentStatusBadge, RequestStatusBadge } from "./RequestBadges";

type Viewer = "customer" | "agent" | "admin";

/** Bảng yêu cầu thiết kế tour (khách / Agent / Admin). Bấm dòng để mở chi tiết. */
export function RequestTable({ rows, basePath, viewer, dimmed }: { rows: CustomRequestSummary[]; basePath: string; viewer: Viewer; dimmed?: boolean }) {
  const router = useRouter();
  return (
    <Table className={dimmed ? "opacity-60" : undefined}>
      <TableHeader>
        <TableRow>
          <TableHead>Yêu cầu</TableHead>
          <TableHead className="hidden md:table-cell">Thời gian · số khách</TableHead>
          {viewer !== "customer" && <TableHead className="hidden lg:table-cell">Khách</TableHead>}
          <TableHead>Trạng thái</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {rows.map((r) => {
          const href = `${basePath}/${r.id}`;
          return (
            <TableRow key={`${r.id}-${r.assignmentDeadline ?? ""}`} className="cursor-pointer" onClick={() => router.push(href)}>
              <TableCell className="max-w-96">
                <Link href={href} className="line-clamp-1 font-medium hover:underline" onClick={(e) => e.stopPropagation()}>
                  {placeName(r.departureLocation)} → {destinationsText(r.destinations)}
                </Link>
                <p className="truncate text-xs text-muted-foreground">
                  {r.code} · {budgetText(r.budgetMin, r.budgetMax)} · gửi {formatDateTime(r.createdAt)}
                </p>
              </TableCell>
              <TableCell className="hidden md:table-cell">
                <p>
                  {r.durationDays} ngày · {startWindowText(r.earliestStart, r.latestStart)}
                </p>
                <p className="text-xs text-muted-foreground">{travellersText(r.adults, r.children, r.infants)}</p>
              </TableCell>
              {viewer !== "customer" && <TableCell className="hidden lg:table-cell">{r.customerName}</TableCell>}
              <TableCell>
                <div className="flex flex-col items-start gap-1">
                  {viewer === "agent" && r.assignmentStatus ? (
                    <AssignmentStatusBadge status={r.assignmentStatus} />
                  ) : (
                    <RequestStatusBadge status={r.status} customer={viewer === "customer"} />
                  )}
                  {viewer === "agent" && r.assignmentStatus === "PENDING" && r.assignmentDeadline && (
                    <span className="text-xs font-medium text-amber-700">{timeLeftText(r.assignmentDeadline)} để phản hồi</span>
                  )}
                  {r.stage && (viewer !== "agent" || r.assignmentStatus === "ACCEPTED") && (
                    <span
                      className={cn(
                        "text-xs",
                        (viewer === "agent" && r.stage !== "WAITING_CUSTOMER") || (viewer === "customer" && r.stage === "WAITING_CUSTOMER")
                          ? "font-medium text-amber-700"
                          : "text-muted-foreground",
                      )}
                    >
                      {viewer === "customer" ? STAGE_LABEL[r.stage].customer : STAGE_LABEL[r.stage].agent}
                      {viewer !== "customer" && r.proposalDeadline && ` · ${timeLeftText(r.proposalDeadline)}`}
                    </span>
                  )}
                  {viewer !== "agent" && r.agentName && <span className="text-xs text-muted-foreground">{r.agentName}</span>}
                </div>
              </TableCell>
            </TableRow>
          );
        })}
      </TableBody>
    </Table>
  );
}
