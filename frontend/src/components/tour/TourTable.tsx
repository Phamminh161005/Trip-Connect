"use client";

import Image from "next/image";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { Globe, ImageOff } from "lucide-react";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { formatDateTime } from "@/lib/format";
import { formatDuration, formatPrice } from "@/lib/tour/labels";
import type { TourSummary } from "@/types/tour";
import { TourStatusBadge } from "./TourStatusBadge";

/** Bảng danh sách tour (trang quản lý của Agent và Admin). Bấm dòng để mở chi tiết. */
export function TourTable({
  tours,
  basePath,
  showProvider,
  dimmed,
  timeColumn = "updatedAt",
}: {
  tours: TourSummary[];
  basePath: string;
  showProvider?: boolean;
  dimmed?: boolean;
  timeColumn?: "updatedAt" | "submittedAt";
}) {
  const router = useRouter();
  return (
    <Table className={dimmed ? "opacity-60" : undefined}>
      <TableHeader>
        <TableRow>
          <TableHead>Tour</TableHead>
          <TableHead>Trạng thái</TableHead>
          <TableHead className="hidden md:table-cell">Lịch mở bán</TableHead>
          <TableHead className="hidden lg:table-cell">{timeColumn === "submittedAt" ? "Gửi duyệt lúc" : "Cập nhật"}</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {tours.map((tour) => {
          const href = `${basePath}/${tour.id}`;
          return (
            <TableRow key={tour.id} className="cursor-pointer" onClick={() => router.push(href)}>
              <TableCell className="max-w-96">
                <div className="flex items-center gap-3">
                  <div className="relative hidden size-14 shrink-0 overflow-hidden rounded-lg bg-muted sm:block">
                    {tour.coverImageUrl ? (
                      <Image src={tour.coverImageUrl} alt="" fill sizes="56px" className="object-cover" />
                    ) : (
                      <ImageOff className="absolute inset-0 m-auto size-5 text-muted-foreground" />
                    )}
                  </div>
                  <div className="min-w-0">
                    <Link href={href} className="line-clamp-2 font-medium hover:underline" onClick={(e) => e.stopPropagation()}>
                      {tour.title}
                    </Link>
                    <p className="flex items-center gap-1 truncate text-xs text-muted-foreground">
                      {tour.international && <Globe className="size-3 shrink-0" />}
                      {formatDuration(tour.durationDays, tour.durationNights)}
                      {tour.departureLocation && ` · từ ${tour.departureLocation}`}
                      {showProvider && ` · ${tour.platformTour ? "TripConnect" : (tour.provider?.companyName ?? "Đối tác")}`}
                    </p>
                  </div>
                </div>
              </TableCell>
              <TableCell>
                <TourStatusBadge status={tour.status} />
              </TableCell>
              <TableCell className="hidden md:table-cell">
                {tour.openDepartureCount > 0 ? (
                  <>
                    <p>{tour.openDepartureCount} lịch</p>
                    {tour.minAdultPrice !== null && (
                      <p className="text-xs text-muted-foreground">từ {formatPrice(tour.minAdultPrice)}</p>
                    )}
                  </>
                ) : (
                  <span className="text-muted-foreground">Chưa có</span>
                )}
              </TableCell>
              <TableCell className="hidden text-muted-foreground lg:table-cell">
                {formatDateTime(timeColumn === "submittedAt" ? tour.submittedAt : (tour.updatedAt ?? tour.createdAt))}
              </TableCell>
            </TableRow>
          );
        })}
      </TableBody>
    </Table>
  );
}
