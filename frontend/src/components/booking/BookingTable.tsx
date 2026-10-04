"use client";

import Image from "next/image";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { ImageOff } from "lucide-react";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { formatDateTime } from "@/lib/format";
import { travellersText } from "@/lib/booking/labels";
import { formatDay, formatPrice } from "@/lib/tour/labels";
import type { BookingSummary } from "@/types/booking";
import { BookingStatusBadge, RefundStatusBadge } from "./BookingBadges";

/** Bảng đơn đặt tour (khách / Agent / Admin). Bấm dòng để mở chi tiết. */
export function BookingTable({
  bookings,
  basePath,
  showCustomer,
  dimmed,
}: {
  bookings: BookingSummary[];
  basePath: string;
  showCustomer?: boolean;
  dimmed?: boolean;
}) {
  const router = useRouter();
  return (
    <Table className={dimmed ? "opacity-60" : undefined}>
      <TableHeader>
        <TableRow>
          <TableHead>Đơn</TableHead>
          {showCustomer && <TableHead className="hidden lg:table-cell">Khách</TableHead>}
          <TableHead className="hidden md:table-cell">Khởi hành</TableHead>
          <TableHead className="text-right">Tổng tiền</TableHead>
          <TableHead>Trạng thái</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {bookings.map((b) => {
          const href = `${basePath}/${b.id}`;
          return (
            <TableRow key={b.id} className="cursor-pointer" onClick={() => router.push(href)}>
              <TableCell className="max-w-96">
                <div className="flex items-center gap-3">
                  <div className="relative hidden size-12 shrink-0 overflow-hidden rounded-lg bg-muted sm:block">
                    {b.coverImageUrl ? (
                      <Image src={b.coverImageUrl} alt="" fill sizes="48px" className="object-cover" />
                    ) : (
                      <ImageOff className="absolute inset-0 m-auto size-4 text-muted-foreground" />
                    )}
                  </div>
                  <div className="min-w-0">
                    <Link href={href} className="line-clamp-1 font-medium hover:underline" onClick={(e) => e.stopPropagation()}>
                      {b.tourTitle}
                    </Link>
                    <p className="truncate text-xs text-muted-foreground">
                      {b.code} · {travellersText(b.adults, b.children, b.infants)} · đặt {formatDateTime(b.createdAt)}
                    </p>
                  </div>
                </div>
              </TableCell>
              {showCustomer && (
                <TableCell className="hidden max-w-48 lg:table-cell">
                  <p className="truncate">{b.customerName}</p>
                  <p className="truncate text-xs text-muted-foreground">{b.customerEmail}</p>
                </TableCell>
              )}
              <TableCell className="hidden md:table-cell">{formatDay(b.startDate)}</TableCell>
              <TableCell className="text-right font-medium">{formatPrice(b.totalAmount)}</TableCell>
              <TableCell>
                <div className="flex flex-col items-start gap-1">
                  <BookingStatusBadge status={b.status} />
                  <RefundStatusBadge status={b.refundStatus} />
                </div>
              </TableCell>
            </TableRow>
          );
        })}
      </TableBody>
    </Table>
  );
}
