import type { PageResponse } from "@/types/common";
import type {
  ManagedReview,
  MyReview,
  PublicReviewPage,
  ReviewImage,
  ReviewSummary,
  WriteReviewRequest,
} from "@/types/review";
import { apiRequest } from "./client";
import { query } from "./query";

// ----- Trang tour (công khai) -----

export const listTourReviews = (tourId: number, params: { before?: number; size?: number; rating?: number }) =>
  apiRequest<PublicReviewPage>(`/api/tours/${tourId}/reviews${query({ ...params })}`, { auth: false });

export const getTourReviewSummary = (tourId: number) =>
  apiRequest<ReviewSummary>(`/api/tours/${tourId}/reviews/summary`, { auth: false });

// ----- Khách -----

/** null khi chưa đánh giá (Backend trả 204). */
export const getMyReview = (bookingId: number) =>
  apiRequest<MyReview | undefined>(`/api/bookings/${bookingId}/review`).then((review) => review ?? null);

export const createReview = (bookingId: number, data: WriteReviewRequest) =>
  apiRequest<MyReview>(`/api/bookings/${bookingId}/review`, { method: "POST", body: data });

export const updateReview = (reviewId: number, data: WriteReviewRequest) =>
  apiRequest<MyReview>(`/api/reviews/${reviewId}`, { method: "PUT", body: data });

export function uploadReviewImage(reviewId: number, file: File) {
  const formData = new FormData();
  formData.append("file", file);
  return apiRequest<ReviewImage>(`/api/reviews/${reviewId}/images`, { method: "POST", body: formData });
}

export const deleteReviewImage = (reviewId: number, imageId: number) =>
  apiRequest<void>(`/api/reviews/${reviewId}/images/${imageId}`, { method: "DELETE" });

// ----- Quản lý (Agent: tour của mình; Admin: mọi đánh giá) -----

export type ReviewScope = "agent" | "admin";

export interface ManagedReviewParams {
  rating?: number;
  replied?: boolean;
  hidden?: boolean;
  q?: string;
  page?: number;
  size?: number;
}

export const listManagedReviews = (scope: ReviewScope, params: ManagedReviewParams) =>
  apiRequest<PageResponse<ManagedReview>>(`/api/${scope}/reviews${query({ ...params })}`);

export const replyToReview = (scope: ReviewScope, reviewId: number, reply: string) =>
  apiRequest<ManagedReview>(`/api/${scope}/reviews/${reviewId}/reply`, { method: "PUT", body: { reply } });

export const hideReview = (reviewId: number, reason: string) =>
  apiRequest<ManagedReview>(`/api/admin/reviews/${reviewId}/hide`, { method: "POST", body: { reason } });

export const unhideReview = (reviewId: number) =>
  apiRequest<ManagedReview>(`/api/admin/reviews/${reviewId}/unhide`, { method: "POST" });
