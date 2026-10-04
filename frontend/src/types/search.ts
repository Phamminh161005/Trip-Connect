// Khớp các DTO tìm kiếm của Backend (TourSearchRequest, SearchResponses)
import type { LocationResponse } from "./catalog";

export type TourSort = "RECOMMENDED" | "PRICE_ASC" | "PRICE_DESC" | "DEPARTURE_SOON" | "NEWEST" | "RATING";

export interface TourSearchParams {
  q?: string;
  destinationId?: number;
  departureLocationId?: number;
  /** yyyy-MM-dd */
  dateFrom?: string;
  dateTo?: string;
  priceMin?: number;
  priceMax?: number;
  durationMin?: number;
  durationMax?: number;
  categoryIds?: number[];
  international?: boolean;
  sort?: TourSort;
  page?: number;
  size?: number;
  /** Ghi vào lịch sử tìm kiếm (chỉ trang kết quả bật). */
  track?: boolean;
}

export interface TourCard {
  id: number;
  title: string;
  coverImageUrl: string | null;
  durationDays: number;
  durationNights: number;
  departureLocation: string | null;
  destinations: string[];
  international: boolean;
  highlights: string[];
  rating: number | null;
  ratingCount: number;
  providerName: string;
  /** Giá người lớn thấp nhất trong các lịch khớp bộ lọc */
  minPrice: number;
  nextDepartureDate: string;
  departureCount: number;
}

export interface PopularDestination {
  location: LocationResponse;
  tourCount: number;
  coverImageUrl: string | null;
}

export interface RecentSearch {
  id: number;
  keyword: string | null;
  destination: LocationResponse | null;
  dateFrom: string | null;
  dateTo: string | null;
  searchedAt: string;
}
