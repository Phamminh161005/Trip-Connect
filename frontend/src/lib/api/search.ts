import type { PageResponse } from "@/types/common";
import type { PopularDestination, RecentSearch, TourCard, TourSearchParams } from "@/types/search";
import { visitorHeaders } from "@/lib/visitor";
import { apiRequest } from "./client";
import { query } from "./query";

/**
 * Tìm tour đang bán. Gửi kèm token (nếu đã đăng nhập) + mã khách để Backend ghi lịch sử khi track = true.
 * API công khai: chưa đăng nhập vẫn gọi được.
 */
export const searchTours = ({ categoryIds, ...params }: TourSearchParams) =>
  apiRequest<PageResponse<TourCard>>(
    `/api/tours${query({ ...params, categoryIds: categoryIds?.length ? categoryIds.join(",") : undefined })}`,
    { headers: visitorHeaders() },
  );

export const getPopularDestinations = (limit = 8) =>
  apiRequest<PopularDestination[]>(`/api/tours/destinations/popular?limit=${limit}`, { auth: false });

// ----- Lịch sử tìm kiếm của người đang đăng nhập -----

export const getRecentSearches = () => apiRequest<RecentSearch[]>("/api/me/search-history");

export const deleteRecentSearch = (id: number) => apiRequest<void>(`/api/me/search-history/${id}`, { method: "DELETE" });

export const clearSearchHistory = () => apiRequest<void>("/api/me/search-history", { method: "DELETE" });

/** Gọi sau khi đăng nhập: gộp lịch sử tìm kiếm lúc chưa đăng nhập vào tài khoản. */
export const claimSearchHistory = () =>
  apiRequest<{ claimed: number }>("/api/me/search-history/claim", { method: "POST", headers: visitorHeaders() });
