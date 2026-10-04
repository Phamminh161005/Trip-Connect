"use client";

import { useQuery, useQueryClient } from "@tanstack/react-query";
import { getManagedTour, type TourScope } from "@/lib/api/tours";
import type { TourDetail } from "@/types/tour";

/**
 * Khóa cache bắt đầu bằng "agent" / "admin" -> sau mỗi thao tác chỉ cần làm mới theo tiền tố
 * (danh sách tour, số việc chờ trên menu Admin... cập nhật cùng lúc).
 */
export const tourKeys = {
  scope: (scope: TourScope) => [scope] as const,
  list: (scope: TourScope, params: object) => [scope, "tours", params] as const,
  detail: (scope: TourScope, id: number) => [scope, "tour", id] as const,
};

export function useManagedTour(scope: TourScope, id: number) {
  return useQuery({ queryKey: tourKeys.detail(scope, id), queryFn: () => getManagedTour(scope, id) });
}

/** Sau khi thao tác: cập nhật ngay chi tiết tour (nếu API trả về) và làm mới các dữ liệu liên quan. */
export function useTourRefresh(scope: TourScope, id: number) {
  const queryClient = useQueryClient();
  return (tour?: TourDetail) => {
    if (tour) queryClient.setQueryData(tourKeys.detail(scope, id), tour);
    return queryClient.invalidateQueries({ queryKey: tourKeys.scope(scope) });
  };
}

/** Agent sửa nội dung tour đang bán / đang ẩn -> tour về Nháp, phải gửi duyệt lại. */
export const changeNeedsReview = (scope: TourScope, tour: TourDetail) =>
  scope === "agent" && (tour.status === "PUBLISHED" || tour.status === "HIDDEN");

/** Đang chờ duyệt / bị đình chỉ thì khóa nội dung (khớp TourAccess.requireContentEditable). */
export const contentLocked = (tour: TourDetail) => tour.status === "PENDING_APPROVAL" || tour.status === "SUSPENDED";
