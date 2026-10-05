"use client";

import type { ReactNode } from "react";
import { BedDouble, CalendarDays, Check, Clock, MessageSquareQuote, UtensilsCrossed, X } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { travellersText } from "@/lib/booking/labels";
import { PROPOSAL_STATUS, proposalChanges } from "@/lib/customRequest/labels";
import { formatDateTime } from "@/lib/format";
import { ACCOMMODATION_LABELS, TRANSPORT_LABELS, formatDay, formatDuration, formatPrice, mealsText } from "@/lib/tour/labels";
import { cn } from "@/lib/utils";
import type { CustomRequestDetail, ProposalView } from "@/types/customRequest";

/**
 * Các phiên bản đề xuất của một yêu cầu: bản mới nhất mở sẵn (kèm nút thao tác), bản cũ thu gọn.
 * Mỗi bản có nhãn những gì đã đổi so với bản trước.
 */
export function ProposalSection({
  request,
  actions,
  emptyText,
}: {
  request: CustomRequestDetail;
  /** Nút thao tác cho bản mới nhất (khách: đồng ý / yêu cầu chỉnh sửa) */
  actions?: ReactNode;
  emptyText?: string;
}) {
  const proposals = request.proposals;
  if (proposals.length === 0) {
    return emptyText ? (
      <Card className="rounded-2xl">
        <CardHeader>
          <CardTitle className="text-lg">Đề xuất lịch trình & báo giá</CardTitle>
          <CardDescription>{emptyText}</CardDescription>
        </CardHeader>
      </Card>
    ) : null;
  }
  const latest = proposals[proposals.length - 1];
  const older = proposals.slice(0, -1).reverse();

  return (
    <section className="flex flex-col gap-4">
      <div className="flex flex-wrap items-end justify-between gap-2">
        <h2 className="text-lg font-semibold">Đề xuất lịch trình & báo giá</h2>
        {request.status === "IN_PROGRESS" && (
          <p className="text-sm text-muted-foreground">
            Đã chỉnh sửa {request.revisionCount}/{request.maxRevisions} lần
          </p>
        )}
      </div>
      <ProposalCard proposal={latest} previous={proposals[proposals.length - 2]} request={request} actions={actions} />
      {older.length > 0 && (
        <div className="flex flex-col gap-2">
          <p className="text-sm font-medium text-muted-foreground">Phiên bản trước</p>
          {older.map((p) => {
            const index = proposals.indexOf(p);
            return (
              <details key={p.id} className="group rounded-2xl border bg-card">
                <summary className="flex cursor-pointer list-none flex-wrap items-center gap-x-3 gap-y-1 p-4">
                  <span className="font-medium">Bản {p.versionNo}</span>
                  <ProposalStatusBadge proposal={p} />
                  <span className="text-sm text-muted-foreground">
                    {formatPrice(p.totalPrice)} · gửi {formatDateTime(p.createdAt)}
                    {p.agentName && ` · ${p.agentName}`}
                  </span>
                </summary>
                <div className="border-t p-4">
                  <ProposalBody proposal={p} previous={proposals[index - 1]} request={request} />
                </div>
              </details>
            );
          })}
        </div>
      )}
    </section>
  );
}

function ProposalStatusBadge({ proposal }: { proposal: ProposalView }) {
  const s = PROPOSAL_STATUS[proposal.status];
  return <Badge className={cn("border-0", s.className)}>{s.label}</Badge>;
}

function ProposalCard({
  proposal: p,
  previous,
  request,
  actions,
}: {
  proposal: ProposalView;
  previous?: ProposalView;
  request: CustomRequestDetail;
  actions?: ReactNode;
}) {
  return (
    <Card className="rounded-2xl">
      <CardHeader className="gap-2">
        <div className="flex flex-wrap items-center gap-2">
          <Badge variant="outline">Bản {p.versionNo}</Badge>
          <ProposalStatusBadge proposal={p} />
          {p.agentName && <span className="text-sm text-muted-foreground">{p.agentName}</span>}
        </div>
        <CardTitle className="text-xl">{p.title}</CardTitle>
        <CardDescription>
          Gửi lúc {formatDateTime(p.createdAt)}
          {p.status === "SENT" && ` · phản hồi trước ${formatDateTime(p.expiresAt)}`}
        </CardDescription>
      </CardHeader>
      <CardContent className="flex flex-col gap-6">
        <ProposalBody proposal={p} previous={previous} request={request} />
        {actions && <div className="flex flex-wrap justify-end gap-2 border-t pt-4">{actions}</div>}
      </CardContent>
    </Card>
  );
}

function ProposalBody({ proposal: p, previous, request }: { proposal: ProposalView; previous?: ProposalView; request: CustomRequestDetail }) {
  const changes = proposalChanges(p, previous);
  return (
    <div className="flex flex-col gap-6">
      {changes.length > 0 && (
        <div className="flex flex-wrap items-center gap-2 text-sm">
          <span className="text-muted-foreground">Thay đổi so với bản {previous?.versionNo}:</span>
          {changes.map((c) => (
            <Badge key={c} className="border-0 bg-sky-100 text-sky-900">
              {c}
            </Badge>
          ))}
        </div>
      )}

      {p.agentMessage && <Quote label="Lời nhắn của đơn vị tổ chức">{p.agentMessage}</Quote>}

      <div className="grid gap-4 md:grid-cols-[1fr_280px]">
        <div className="grid gap-3 text-sm sm:grid-cols-2">
          <Fact label="Thời gian" value={`${formatDay(p.startDate)} – ${formatDay(p.endDate)}`} />
          <Fact label="Thời lượng" value={formatDuration(p.durationDays, p.durationNights)} />
          <Fact label="Lưu trú" value={ACCOMMODATION_LABELS[p.accommodationType]} />
          <Fact label="Phương tiện" value={p.transportModes.map((m) => TRANSPORT_LABELS[m]).join(", ")} />
          <Fact label="Tập trung" value={`${p.meetingTime} tại ${p.meetingPoint}`} />
          <Fact label="Số khách" value={travellersText(request.adults, request.children, request.infants)} />
        </div>
        <div className="flex flex-col gap-1.5 rounded-xl bg-muted/60 p-4 text-sm">
          <Row label={`Người lớn × ${request.adults}`} value={formatPrice(p.adultPrice)} />
          {request.children > 0 && <Row label={`Trẻ em × ${request.children}`} value={formatPrice(p.childPrice)} />}
          {request.infants > 0 && <Row label={`Trẻ sơ sinh × ${request.infants}`} value="Miễn phí" />}
          <div className="mt-1 flex items-baseline justify-between border-t pt-2">
            <span className="font-medium">Tổng chi phí</span>
            <span className="text-lg font-bold text-primary">{formatPrice(p.totalPrice)}</span>
          </div>
          <Row label="Đặt cọc khi xác nhận" value={formatPrice(p.depositAmount)} />
        </div>
      </div>

      <div className="flex flex-col gap-3">
        <h3 className="flex items-center gap-2 font-semibold">
          <CalendarDays className="size-5 text-primary" /> Lịch trình
        </h3>
        <ol className="flex flex-col gap-2">
          {p.itinerary.map((day) => (
            <li key={day.dayNumber}>
              <details className="rounded-xl border bg-card" open={day.dayNumber === 1}>
                <summary className="flex cursor-pointer list-none items-center gap-3 p-3">
                  <span className="flex size-8 shrink-0 items-center justify-center rounded-full bg-primary/10 text-sm font-semibold text-primary">
                    {day.dayNumber}
                  </span>
                  <span className="font-medium">{day.title}</span>
                </summary>
                <div className="flex flex-col gap-2 px-3 pb-3 pl-14">
                  <p className="text-sm leading-relaxed whitespace-pre-line">{day.description}</p>
                  <div className="flex flex-wrap gap-x-5 gap-y-1 text-sm text-muted-foreground">
                    {mealsText(day) && (
                      <span className="flex items-center gap-1.5">
                        <UtensilsCrossed className="size-4" /> {mealsText(day)}
                      </span>
                    )}
                    {day.accommodation && (
                      <span className="flex items-center gap-1.5">
                        <BedDouble className="size-4" /> Nghỉ đêm: {day.accommodation}
                      </span>
                    )}
                  </div>
                </div>
              </details>
            </li>
          ))}
        </ol>
      </div>

      <div className="grid gap-4 md:grid-cols-2">
        <ServiceList title="Bao gồm" items={p.includedServices} included />
        {p.excludedServices.length > 0 && <ServiceList title="Không bao gồm" items={p.excludedServices} />}
      </div>

      {p.notes && (
        <div className="flex flex-col gap-1 text-sm">
          <h3 className="flex items-center gap-2 font-semibold">
            <Clock className="size-4 text-primary" /> Lưu ý
          </h3>
          <p className="leading-relaxed whitespace-pre-line">{p.notes}</p>
        </div>
      )}

      {p.customerFeedback && (
        <Quote label={`Khách yêu cầu chỉnh sửa${p.respondedAt ? ` (${formatDateTime(p.respondedAt)})` : ""}`} tone="sky">
          {p.customerFeedback}
        </Quote>
      )}
    </div>
  );
}

function Fact({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <p className="text-xs text-muted-foreground">{label}</p>
      <p className="font-medium">{value}</p>
    </div>
  );
}

function Row({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex items-baseline justify-between gap-3">
      <span className="text-muted-foreground">{label}</span>
      <span className="font-medium tabular-nums">{value}</span>
    </div>
  );
}

function Quote({ label, children, tone }: { label: string; children: ReactNode; tone?: "sky" }) {
  return (
    <div
      className={cn(
        "flex gap-3 rounded-xl border-l-4 p-3 text-sm",
        tone === "sky" ? "border-sky-400 bg-sky-50 dark:bg-sky-950/40" : "border-primary/60 bg-primary/5",
      )}
    >
      <MessageSquareQuote className="mt-0.5 size-4 shrink-0 text-muted-foreground" />
      <div>
        <p className="text-xs font-medium text-muted-foreground">{label}</p>
        <p className="leading-relaxed whitespace-pre-line">{children}</p>
      </div>
    </div>
  );
}

function ServiceList({ title, items, included }: { title: string; items: string[]; included?: boolean }) {
  return (
    <div className="flex flex-col gap-2 rounded-xl border p-4">
      <p className="text-sm font-semibold">{title}</p>
      <ul className="flex flex-col gap-1.5 text-sm">
        {items.map((item, index) => (
          <li key={index} className="flex items-start gap-2">
            {included ? <Check className="mt-0.5 size-4 shrink-0 text-emerald-600" /> : <X className="mt-0.5 size-4 shrink-0 text-red-500" />}
            {item}
          </li>
        ))}
      </ul>
    </div>
  );
}
