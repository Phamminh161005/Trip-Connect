"use client";

import type { ReactNode } from "react";
import Image from "next/image";
import Link from "next/link";
import { ArrowLeft, CalendarDays, Hourglass, MapPin, Receipt, Sparkles, UserPen, Users } from "lucide-react";
import { InfoList, InfoRow } from "@/components/common/InfoList";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { formatDate, formatDateTime } from "@/lib/format";
import {
  CANCELLED_BY,
  PASSENGER_TYPE,
  PAYMENT_PURPOSE,
  PAYMENT_STATUS,
  REFUND_RECORD_STATUS,
  REFUND_STATUS,
  travellersText,
} from "@/lib/booking/labels";
import { timeLeftText } from "@/lib/customRequest/labels";
import { formatDay, formatPrice } from "@/lib/tour/labels";
import type { BookingDetail } from "@/types/booking";
import { BookingStatusBadge, RefundStatusBadge } from "./BookingBadges";
import { HoldCountdown } from "./HoldCountdown";

/**
 * Chi tiết một đơn — dùng chung cho khách, Agent, Admin.
 * staff = Agent / Admin: thấy thêm hoa hồng, lịch sử giao dịch VNPay, ghi chú xử lý hoàn tiền.
 */
export function BookingDetailView({
  booking,
  backHref,
  backLabel,
  actions,
  refundActions,
  passengerAction,
  children,
  onHoldExpire,
  staff = false,
  requestHref,
}: {
  booking: BookingDetail;
  backHref: string;
  backLabel: string;
  /** Các nút thao tác cạnh tiêu đề (thanh toán, hủy...). */
  actions?: ReactNode;
  /** Nút xử lý từng khoản hoàn (Admin). */
  refundActions?: (refundId: number) => ReactNode;
  /** Nút cập nhật danh sách hành khách (khách). */
  passengerAction?: ReactNode;
  /** Khối riêng hiện ngay dưới các thông báo trạng thái (vd đánh giá chuyến đi của khách). */
  children?: ReactNode;
  onHoldExpire?: () => void;
  staff?: boolean;
  /** Tour riêng: trang yêu cầu thiết kế tour (thay cho trang tour công khai) */
  requestHref?: string;
}) {
  const b = booking;
  const travellers = b.adults + b.children + b.infants;
  const missing = travellers - b.passengers.length;
  const plan = b.paymentPlan;
  const privateTour = b.customRequestId !== null;
  // Tour riêng nhập danh sách dần: nhắc phải đủ trước khi trả phần còn lại / trước ngày đi
  const passengerDeadline = plan ? plan.balanceDueDate : b.passengerListDeadline;
  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-col gap-3">
        <Link href={backHref} className="flex w-fit items-center gap-1.5 text-sm text-muted-foreground hover:text-foreground">
          <ArrowLeft className="size-4" /> {backLabel}
        </Link>
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div>
            <h1 className="flex flex-wrap items-center gap-3 text-2xl font-bold tracking-tight">
              Đơn {b.code}
              <BookingStatusBadge status={b.status} />
              {privateTour && (
                <Badge variant="outline" className="gap-1 border-primary/40 text-primary">
                  <Sparkles className="size-3" /> Tour riêng
                </Badge>
              )}
              <RefundStatusBadge status={b.refundStatus} />
            </h1>
            <p className="mt-1 text-sm text-muted-foreground">Đặt lúc {formatDateTime(b.createdAt)}</p>
          </div>
          {actions && <div className="flex flex-wrap gap-2">{actions}</div>}
        </div>
      </div>

      {b.status === "PENDING_PAYMENT" && b.canPay && plan && (
        <Alert className="rounded-2xl border-amber-200 bg-amber-50 dark:border-amber-900 dark:bg-amber-950/40">
          <Hourglass />
          <AlertTitle>
            Đặt cọc {formatPrice(plan.depositAmount)} trước {formatDateTime(b.holdExpiresAt)} ({timeLeftText(b.holdExpiresAt)})
          </AlertTitle>
          <AlertDescription>
            Quá hạn đơn sẽ tự hủy. Phần còn lại {formatPrice(plan.balanceAmount)} thanh toán trước hết ngày{" "}
            {formatDay(plan.balanceDueDate)}.
          </AlertDescription>
        </Alert>
      )}
      {b.status === "PENDING_PAYMENT" && b.canPay && !plan && privateTour && (
        <Alert className="rounded-2xl border-amber-200 bg-amber-50 dark:border-amber-900 dark:bg-amber-950/40">
          <Hourglass />
          <AlertTitle>
            Thanh toán {formatPrice(b.totalAmount)} trước {formatDateTime(b.holdExpiresAt)} ({timeLeftText(b.holdExpiresAt)})
          </AlertTitle>
          <AlertDescription>Ngày đi đã gần nên chuyến đi được thanh toán một lần. Quá hạn đơn sẽ tự hủy.</AlertDescription>
        </Alert>
      )}
      {b.status === "DEPOSIT_PAID" && plan && (
        <Alert className="rounded-2xl border-violet-200 bg-violet-50 dark:border-violet-900 dark:bg-violet-950/40">
          <Receipt />
          <AlertTitle>
            Đã đặt cọc {formatPrice(plan.depositAmount)} · còn lại {formatPrice(plan.balanceAmount)}, hạn hết ngày{" "}
            {formatDay(plan.balanceDueDate)}
            {plan.balanceExtended && " (đã gia hạn)"}
          </AlertTitle>
          <AlertDescription>
            Quá hạn mà chưa thanh toán, đơn sẽ bị hủy và tiền cọc không được hoàn lại
            {plan.canExtend && ". Nếu cần thêm thời gian, bạn có thể gia hạn 1 lần thêm 3 ngày"}.
          </AlertDescription>
        </Alert>
      )}
      {privateTour && missing > 0 && (b.status === "PENDING_PAYMENT" || b.status === "DEPOSIT_PAID" || b.status === "PAID") && (
        <Alert className="rounded-2xl">
          <UserPen />
          <AlertTitle>
            {staff ? "Khách" : "Bạn"} cần nhập đầy đủ thông tin {travellers} người đi trước hết ngày {formatDay(passengerDeadline)}
          </AlertTitle>
          <AlertDescription>
            Hiện có {b.passengers.length}/{travellers} người.{" "}
            {plan && b.status !== "PAID"
              ? "Chưa đủ danh sách thì chưa thanh toán được phần còn lại."
              : "Đơn vị tổ chức cần danh sách để đặt dịch vụ cho đoàn."}
          </AlertDescription>
        </Alert>
      )}
      {b.status === "PENDING_PAYMENT" && b.canPay && !privateTour && (
        <Alert className="rounded-2xl border-amber-200 bg-amber-50">
          <AlertTitle>
            <HoldCountdown expiresAt={b.holdExpiresAt} onExpire={onHoldExpire} />
          </AlertTitle>
          <AlertDescription>Đơn chưa thanh toán. Hết thời gian giữ chỗ, đơn sẽ tự hủy và chỗ được nhả cho khách khác.</AlertDescription>
        </Alert>
      )}
      {b.status === "CANCELLED" && (
        <Alert className="rounded-2xl">
          <AlertTitle>
            Đơn đã bị hủy {b.cancelledAt && `lúc ${formatDateTime(b.cancelledAt)}`}
            {b.cancelledBy && ` · bởi ${CANCELLED_BY[b.cancelledBy]}`}
          </AlertTitle>
          <AlertDescription>
            {b.cancelReason && <p>{b.cancelReason}</p>}
            {b.refundStatus !== "NONE" && (
              <p className="font-medium text-foreground">
                Hoàn tiền {formatPrice(b.refundAmount)} — {REFUND_STATUS[b.refundStatus].label.toLowerCase()}
              </p>
            )}
          </AlertDescription>
        </Alert>
      )}

      {children}

      <Card className="overflow-hidden rounded-2xl py-0">
        <div className="grid sm:grid-cols-[220px_1fr]">
          <div className="relative aspect-[16/9] bg-muted sm:aspect-auto">
            {b.coverImageUrl && <Image src={b.coverImageUrl} alt={b.tourTitle} fill sizes="220px" className="object-cover" />}
          </div>
          <div className="flex flex-col gap-2 p-5">
            <Link href={privateTour && requestHref ? requestHref : `/tours/${b.tourId}`} className="font-semibold hover:underline">
              {b.tourTitle}
            </Link>
            <p className="text-sm text-muted-foreground">Đơn vị tổ chức: {b.providerName}</p>
            <div className="flex flex-wrap gap-x-5 gap-y-1 text-sm">
              <span className="flex items-center gap-1.5">
                <CalendarDays className="size-4 text-primary" /> {formatDay(b.startDate)} – {formatDay(b.endDate)}
              </span>
              <span className="flex items-center gap-1.5">
                <MapPin className="size-4 text-primary" /> Tập trung {b.meetingTime.slice(0, 5)} tại {b.meetingPoint}
              </span>
              <span className="flex items-center gap-1.5">
                <Users className="size-4 text-primary" /> {travellersText(b.adults, b.children, b.infants)}
              </span>
            </div>
          </div>
        </div>
      </Card>

      <div className="grid gap-6 lg:grid-cols-2">
        <Card className="rounded-2xl">
          <CardHeader>
            <CardTitle className="flex items-center gap-2 text-lg">
              <Receipt className="size-5 text-primary" /> Thanh toán
            </CardTitle>
          </CardHeader>
          <CardContent>
            <InfoList>
              <InfoRow label={`Người lớn × ${b.adults}`} value={formatPrice(b.adults * b.adultPrice)} />
              {b.children > 0 && <InfoRow label={`Trẻ em × ${b.children}`} value={formatPrice(b.children * b.childPrice)} />}
              {b.infants > 0 && <InfoRow label={`Trẻ sơ sinh × ${b.infants}`} value="Miễn phí" />}
              <InfoRow label="Tổng cộng" value={<span className="text-lg text-primary">{formatPrice(b.totalAmount)}</span>} />
              {plan && (
                <>
                  <InfoRow
                    label="Đặt cọc (30%)"
                    value={`${formatPrice(plan.depositAmount)} · ${plan.depositPaidAt ? `đã trả ${formatDateTime(plan.depositPaidAt)}` : "chưa trả"}`}
                  />
                  <InfoRow
                    label="Phần còn lại"
                    value={`${formatPrice(plan.balanceAmount)} · ${
                      b.paidAt ? `đã trả ${formatDateTime(b.paidAt)}` : `hạn hết ngày ${formatDay(plan.balanceDueDate)}`
                    }`}
                  />
                </>
              )}
              {b.paidAt && !plan && <InfoRow label="Thanh toán lúc" value={formatDateTime(b.paidAt)} />}
              {staff && b.commissionRate !== null && (
                <InfoRow
                  label={`Phí nền tảng (${Math.round(b.commissionRate * 100)}%)`}
                  value={`${formatPrice(b.commissionAmount ?? 0)} · đơn vị tổ chức nhận ${formatPrice(b.totalAmount - (b.commissionAmount ?? 0))}`}
                />
              )}
            </InfoList>
            <p className="mt-3 text-xs text-muted-foreground">
              Chính sách hủy của đơn: hủy trước {b.refundPolicy.fullRefundDays} ngày hoàn 100%, trước {b.refundPolicy.partialRefundDays}{" "}
              ngày hoàn {b.refundPolicy.partialRefundPercent}%, sát hơn không hoàn
              {plan ? ". Tiền cọc không được hoàn khi khách hủy hoặc quá hạn thanh toán." : "."}
            </p>
          </CardContent>
        </Card>

        <Card className="rounded-2xl">
          <CardHeader>
            <CardTitle className="text-lg">Người liên hệ</CardTitle>
          </CardHeader>
          <CardContent>
            <InfoList>
              <InfoRow label="Họ và tên" value={b.contactName} />
              <InfoRow label="Số điện thoại" value={b.contactPhone} />
              <InfoRow label="Email" value={b.contactEmail} />
              {b.note && <InfoRow label="Ghi chú" value={<span className="whitespace-pre-line">{b.note}</span>} />}
            </InfoList>
          </CardContent>
        </Card>
      </div>

      <Card className="rounded-2xl">
        <CardHeader className="flex flex-wrap items-start justify-between gap-3">
          <div className="flex flex-col gap-1.5">
            <CardTitle className="flex flex-wrap items-center gap-2 text-lg">
              Danh sách hành khách
              <Badge variant={missing > 0 ? "outline" : "secondary"} className={missing > 0 ? "border-amber-300 text-amber-900 dark:text-amber-200" : ""}>
                {missing > 0 ? `Đã nhập ${b.passengers.length}/${travellers}` : `Đủ ${travellers} khách`}
              </Badge>
            </CardTitle>
            {missing > 0 && b.status !== "CANCELLED" && (
              <p className="text-sm text-muted-foreground">
                Còn {missing} khách chưa có thông tin{privateTour && ` · cần đủ trước hết ngày ${formatDay(passengerDeadline)}`}.
              </p>
            )}
          </div>
          {passengerAction}
        </CardHeader>
        <CardContent>
          {b.passengers.length === 0 ? (
            <p className="rounded-xl border border-dashed p-6 text-center text-sm text-muted-foreground">
              Chưa có thông tin hành khách — đơn đặt {travellersText(b.adults, b.children, b.infants)}.
            </p>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Họ và tên</TableHead>
                  <TableHead>Ngày sinh</TableHead>
                  <TableHead>Loại</TableHead>
                  {b.international && <TableHead>Hộ chiếu</TableHead>}
                </TableRow>
              </TableHeader>
              <TableBody>
                {b.passengers.map((p) => (
                  <TableRow key={p.id}>
                    <TableCell className="font-medium">{p.fullName}</TableCell>
                    <TableCell>{formatDay(p.dateOfBirth)}</TableCell>
                    <TableCell>
                      <Badge variant="secondary">{PASSENGER_TYPE[p.type]}</Badge>
                    </TableCell>
                    {b.international && <TableCell>{p.passportNumber ?? "—"}</TableCell>}
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>

      {b.refunds.length > 0 && (
        <Card className="rounded-2xl">
          <CardHeader>
            <CardTitle className="text-lg">Hoàn tiền</CardTitle>
          </CardHeader>
          <CardContent className="flex flex-col gap-3">
            {b.refunds.map((r) => (
              <div key={r.id} className="flex flex-wrap items-start justify-between gap-3 rounded-xl border p-3 text-sm">
                <div>
                  <p className="font-semibold">
                    {formatPrice(r.amount)} · {REFUND_RECORD_STATUS[r.status]}
                  </p>
                  <p className="text-muted-foreground">{r.reason}</p>
                  {staff && r.message && <p className="text-xs text-muted-foreground">VNPay / ghi chú: {r.message}</p>}
                  <p className="text-xs text-muted-foreground">
                    Tạo {formatDateTime(r.createdAt)}
                    {r.processedAt && ` · xử lý ${formatDateTime(r.processedAt)}`}
                  </p>
                </div>
                {refundActions?.(r.id)}
              </div>
            ))}
          </CardContent>
        </Card>
      )}

      {staff && b.payments.length > 0 && (
        <Card className="rounded-2xl">
          <CardHeader>
            <CardTitle className="text-lg">Giao dịch VNPay</CardTitle>
          </CardHeader>
          <CardContent>
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Mã giao dịch</TableHead>
                  <TableHead>Khoản</TableHead>
                  <TableHead>Số tiền</TableHead>
                  <TableHead>Trạng thái</TableHead>
                  <TableHead className="hidden sm:table-cell">Ngân hàng / mã VNPay</TableHead>
                  <TableHead className="hidden md:table-cell">Tạo lúc</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {b.payments.map((p) => (
                  <TableRow key={p.id}>
                    <TableCell className="font-mono text-xs">{p.txnRef}</TableCell>
                    <TableCell>{PAYMENT_PURPOSE[p.purpose]}</TableCell>
                    <TableCell>{formatPrice(p.amount)}</TableCell>
                    <TableCell>{PAYMENT_STATUS[p.status]}</TableCell>
                    <TableCell className="hidden sm:table-cell">
                      {[p.bankCode, p.transactionNo, p.responseCode && `mã ${p.responseCode}`].filter(Boolean).join(" · ") || "—"}
                    </TableCell>
                    <TableCell className="hidden md:table-cell">{formatDate(p.createdAt)}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </CardContent>
        </Card>
      )}
    </div>
  );
}
