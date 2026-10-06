"use client";

import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { useQuery } from "@tanstack/react-query";
import { CircleCheck, CircleX } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Spinner } from "@/components/ui/spinner";
import { errorMessage } from "@/lib/api/errors";
import { confirmVnPayReturn } from "@/lib/api/bookings";

/**
 * VNPay đưa khách về đây kèm kết quả trên đường link (?vnp_ResponseCode=...&vnp_SecureHash=...).
 * Trang chuyển nguyên các tham số cho Backend: Backend kiểm chữ ký rồi mới ghi nhận, nên khách sửa link cũng vô ích.
 */
export function VnPayReturnView() {
  const search = useSearchParams().toString();
  const query = useQuery({
    queryKey: ["vnpay-return", search],
    queryFn: () => confirmVnPayReturn(search),
    enabled: search.includes("vnp_"),
    retry: 1,
    staleTime: Infinity,
  });

  return (
    <div className="mx-auto flex w-full max-w-lg flex-1 items-center px-4 py-16">
      <Card className="w-full rounded-2xl">
        <CardContent className="flex flex-col items-center gap-4 py-6 text-center">
          {!search.includes("vnp_") ? (
            <Result ok={false} title="Không có thông tin thanh toán" message="Đường dẫn không hợp lệ." />
          ) : query.isPending ? (
            <>
              <Spinner className="size-8 text-primary" />
              <p className="text-muted-foreground">Đang xác nhận kết quả thanh toán...</p>
            </>
          ) : query.isError ? (
            <Result ok={false} title="Không xác nhận được kết quả" message={errorMessage(query.error)} />
          ) : (
            <Result
              ok={query.data.success}
              title={
                !query.data.success
                  ? "Thanh toán chưa thành công"
                  : query.data.bookingStatus === "DEPOSIT_PAID"
                    ? "Đặt cọc thành công!"
                    : "Đặt tour thành công!"
              }
              message={
                !query.data.success
                  ? `${query.data.message}. Bạn có thể thanh toán lại trước khi hết hạn.`
                  : query.data.bookingStatus === "DEPOSIT_PAID"
                    ? `Mã đơn ${query.data.bookingCode}. Nhớ nhập đủ thông tin người đi và thanh toán phần còn lại trước hạn.`
                    : `Mã đơn ${query.data.bookingCode}. Xác nhận đặt tour đã được gửi tới email của bạn.`
              }
            />
          )}
          <div className="flex flex-wrap justify-center gap-2">
            {query.data?.bookingId && (
              <Button asChild className="rounded-xl">
                <Link href={`/account/bookings/${query.data.bookingId}`}>
                  {query.data.success ? "Xem đơn đặt tour" : "Xem đơn và thanh toán lại"}
                </Link>
              </Button>
            )}
            <Button asChild variant="outline" className="rounded-xl">
              <Link href="/tours">Khám phá tour khác</Link>
            </Button>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}

function Result({ ok, title, message }: { ok: boolean; title: string; message: string }) {
  return (
    <>
      {ok ? <CircleCheck className="size-14 text-emerald-600" /> : <CircleX className="size-14 text-destructive" />}
      <div>
        <h1 className="text-xl font-bold">{title}</h1>
        <p className="mt-1 text-muted-foreground">{message}</p>
      </div>
    </>
  );
}
