"use client";

import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { ArrowLeft, Printer } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { errorMessage } from "@/lib/api/errors";
import { getManifest, type BookingScope } from "@/lib/api/bookings";
import { PASSENGER_TYPE, travellersText } from "@/lib/booking/labels";
import { formatDay } from "@/lib/tour/labels";

/** Danh sách đoàn của một lịch khởi hành — để đơn vị tổ chức chuẩn bị xe, phòng, bảo hiểm. In được. */
export function ManifestView({ scope, departureId, backHref }: { scope: BookingScope; departureId: number; backHref: string }) {
  const query = useQuery({ queryKey: [scope, "manifest", departureId], queryFn: () => getManifest(scope, departureId) });

  if (query.isPending) return <Skeleton className="h-96 rounded-2xl" />;
  if (query.isError) return <p className="text-destructive">{errorMessage(query.error)}</p>;
  const m = query.data;
  const passengers = m.bookings.flatMap((b) => b.passengers.map((p) => ({ ...p, booking: b })));
  const international = passengers.some((p) => p.passportNumber);
  const travellers = m.bookings.reduce((sum, b) => sum + b.adults + b.children + b.infants, 0);
  const incomplete = m.bookings
    .map((b) => ({ booking: b, missing: b.adults + b.children + b.infants - b.passengers.length }))
    .filter((x) => x.missing > 0);

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-col gap-3 print:hidden">
        <Link href={backHref} className="flex w-fit items-center gap-1.5 text-sm text-muted-foreground hover:text-foreground">
          <ArrowLeft className="size-4" /> Quay lại quản lý tour
        </Link>
      </div>
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">Danh sách đoàn</h1>
          <p className="text-muted-foreground">
            {m.tourTitle} · {formatDay(m.startDate)} – {formatDay(m.endDate)} · {travellers} khách ({m.seatsBooked}/{m.capacity} chỗ)
          </p>
        </div>
        <Button variant="outline" className="rounded-xl print:hidden" onClick={() => window.print()}>
          <Printer /> In danh sách
        </Button>
      </div>

      {m.bookings.length === 0 ? (
        <p className="rounded-2xl border border-dashed p-10 text-center text-muted-foreground">Chưa có khách nào thanh toán cho lịch này.</p>
      ) : (
        <>
          <Card className="rounded-2xl">
            <CardHeader>
              <CardTitle className="text-lg">Hành khách</CardTitle>
            </CardHeader>
            <CardContent>
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead className="w-10">#</TableHead>
                    <TableHead>Họ và tên</TableHead>
                    <TableHead>Ngày sinh</TableHead>
                    <TableHead>Loại</TableHead>
                    {international && <TableHead>Hộ chiếu</TableHead>}
                    <TableHead>Mã đơn</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {passengers.map((p, index) => (
                    <TableRow key={p.id}>
                      <TableCell>{index + 1}</TableCell>
                      <TableCell className="font-medium">{p.fullName}</TableCell>
                      <TableCell>{formatDay(p.dateOfBirth)}</TableCell>
                      <TableCell>
                        <Badge variant="secondary">{PASSENGER_TYPE[p.type]}</Badge>
                      </TableCell>
                      {international && <TableCell>{p.passportNumber ?? "—"}</TableCell>}
                      <TableCell className="font-mono text-xs">{p.booking.code}</TableCell>
                    </TableRow>
                  ))}
                  {incomplete.map(({ booking, missing }) => (
                    <TableRow key={`missing-${booking.bookingId}`} className="text-muted-foreground">
                      <TableCell>—</TableCell>
                      <TableCell colSpan={international ? 4 : 3} className="italic">
                        {missing} khách chưa có thông tin (liên hệ {booking.contactName} · {booking.contactPhone})
                      </TableCell>
                      <TableCell className="font-mono text-xs">{booking.code}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </CardContent>
          </Card>

          <Card className="rounded-2xl">
            <CardHeader>
              <CardTitle className="text-lg">Liên hệ & ghi chú theo đơn</CardTitle>
            </CardHeader>
            <CardContent>
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Mã đơn</TableHead>
                    <TableHead>Người liên hệ</TableHead>
                    <TableHead>Điện thoại</TableHead>
                    <TableHead>Số khách</TableHead>
                    <TableHead>Ghi chú</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {m.bookings.map((b) => (
                    <TableRow key={b.bookingId}>
                      <TableCell className="font-mono text-xs">{b.code}</TableCell>
                      <TableCell>
                        <p>{b.contactName}</p>
                        <p className="text-xs text-muted-foreground">{b.contactEmail}</p>
                      </TableCell>
                      <TableCell>{b.contactPhone}</TableCell>
                      <TableCell>
                        <p>{travellersText(b.adults, b.children, b.infants)}</p>
                        {b.passengers.length < b.adults + b.children + b.infants && (
                          <p className="text-xs text-amber-700">
                            Đã nhập {b.passengers.length}/{b.adults + b.children + b.infants} khách
                          </p>
                        )}
                      </TableCell>
                      <TableCell className="max-w-64 whitespace-normal">{b.note ?? "—"}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </CardContent>
          </Card>
        </>
      )}
    </div>
  );
}
