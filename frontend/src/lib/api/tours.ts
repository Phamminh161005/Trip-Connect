import type { TemporaryUrlResponse } from "@/types/agent";
import type { PageResponse } from "@/types/common";
import type {
  DepartureRequest,
  TourContentRequest,
  TourDeparture,
  TourDetail,
  TourImage,
  TourStatus,
  TourSummary,
} from "@/types/tour";
import { apiRequest } from "./client";
import { query } from "./query";

/**
 * Ai đang quản lý tour:
 *  - "agent": tour của chính Agent (/api/agent/tours)
 *  - "admin": tour của TripConnect do Admin tạo (/api/admin/tours)
 * Hai bên dùng chung một bộ API quản lý (Backend: TourManagementEndpoints).
 */
export type TourScope = "agent" | "admin";

const base = (scope: TourScope) => `/api/${scope}/tours`;

export interface TourListParams {
  status?: TourStatus;
  q?: string;
  page?: number;
  size?: number;
  sort?: string;
}

// ----- Quản lý tour (Agent / Admin) -----

export const listMyTours = (params: TourListParams) =>
  apiRequest<PageResponse<TourSummary>>(`${base("agent")}${query({ ...params })}`);

export const getManagedTour = (scope: TourScope, id: number) => apiRequest<TourDetail>(`${base(scope)}/${id}`);

export const createTour = (scope: TourScope, data: TourContentRequest) =>
  apiRequest<TourDetail>(base(scope), { method: "POST", body: data });

export const updateTour = (scope: TourScope, id: number, data: TourContentRequest) =>
  apiRequest<TourDetail>(`${base(scope)}/${id}`, { method: "PUT", body: data });

export const deleteTour = (scope: TourScope, id: number) =>
  apiRequest<void>(`${base(scope)}/${id}`, { method: "DELETE" });

export const hideTour = (scope: TourScope, id: number) =>
  apiRequest<TourDetail>(`${base(scope)}/${id}/hide`, { method: "POST" });

export const unhideTour = (scope: TourScope, id: number) =>
  apiRequest<TourDetail>(`${base(scope)}/${id}/unhide`, { method: "POST" });

/** Agent gửi Admin duyệt. */
export const submitTour = (id: number) => apiRequest<TourDetail>(`${base("agent")}/${id}/submit`, { method: "POST" });

export const withdrawTour = (id: number) => apiRequest<TourDetail>(`${base("agent")}/${id}/withdraw`, { method: "POST" });

/** Admin công khai tour của TripConnect (không qua duyệt). */
export const publishTour = (id: number) => apiRequest<TourDetail>(`${base("admin")}/${id}/publish`, { method: "POST" });

// ----- Ảnh -----

export const uploadTourImage = (scope: TourScope, id: number, file: File) => {
  const formData = new FormData();
  formData.append("file", file);
  return apiRequest<TourImage>(`${base(scope)}/${id}/images`, { method: "POST", body: formData });
};

export const deleteTourImage = (scope: TourScope, id: number, imageId: number) =>
  apiRequest<void>(`${base(scope)}/${id}/images/${imageId}`, { method: "DELETE" });

/** Ảnh đầu tiên là ảnh bìa. */
export const reorderTourImages = (scope: TourScope, id: number, imageIds: number[]) =>
  apiRequest<TourImage[]>(`${base(scope)}/${id}/images/order`, { method: "PUT", body: { imageIds } });

// ----- File chương trình tour (PDF) -----

export const uploadItineraryFile = (scope: TourScope, id: number, file: File) => {
  const formData = new FormData();
  formData.append("file", file);
  return apiRequest<{ fileName: string; sizeBytes: number }>(`${base(scope)}/${id}/itinerary-file`, {
    method: "PUT",
    body: formData,
  });
};

export const deleteItineraryFile = (scope: TourScope, id: number) =>
  apiRequest<void>(`${base(scope)}/${id}/itinerary-file`, { method: "DELETE" });

/** Link xem file cho người quản lý (Agent: tour của mình; Admin: mọi tour). */
export const getManagedItineraryFileUrl = (scope: TourScope, id: number) =>
  apiRequest<TemporaryUrlResponse>(`${base(scope)}/${id}/itinerary-file/url`);

// ----- Lịch khởi hành -----

export const addDeparture = (scope: TourScope, id: number, data: DepartureRequest) =>
  apiRequest<TourDeparture>(`${base(scope)}/${id}/departures`, { method: "POST", body: data });

export const updateDeparture = (scope: TourScope, id: number, departureId: number, data: DepartureRequest) =>
  apiRequest<TourDeparture>(`${base(scope)}/${id}/departures/${departureId}`, { method: "PUT", body: data });

export const closeDeparture = (scope: TourScope, id: number, departureId: number) =>
  apiRequest<TourDeparture>(`${base(scope)}/${id}/departures/${departureId}/close`, { method: "POST" });

export const reopenDeparture = (scope: TourScope, id: number, departureId: number) =>
  apiRequest<TourDeparture>(`${base(scope)}/${id}/departures/${departureId}/reopen`, { method: "POST" });

export const cancelDeparture = (scope: TourScope, id: number, departureId: number, reason: string) =>
  apiRequest<TourDeparture>(`${base(scope)}/${id}/departures/${departureId}/cancel`, { method: "POST", body: { reason } });

export const deleteDeparture = (scope: TourScope, id: number, departureId: number) =>
  apiRequest<void>(`${base(scope)}/${id}/departures/${departureId}`, { method: "DELETE" });

// ----- Admin duyệt tour -----

export interface AdminTourListParams extends TourListParams {
  provider?: "AGENT" | "PLATFORM";
}

export const listToursForAdmin = (params: AdminTourListParams) =>
  apiRequest<PageResponse<TourSummary>>(`${base("admin")}${query({ ...params })}`);

export const approveTour = (id: number) => apiRequest<TourDetail>(`${base("admin")}/${id}/approve`, { method: "POST" });

export const requestTourRevision = (id: number, reason: string) =>
  apiRequest<TourDetail>(`${base("admin")}/${id}/request-revision`, { method: "POST", body: { reason } });

export const suspendTour = (id: number, reason: string) =>
  apiRequest<TourDetail>(`${base("admin")}/${id}/suspend`, { method: "POST", body: { reason } });

export const unsuspendTour = (id: number) => apiRequest<TourDetail>(`${base("admin")}/${id}/unsuspend`, { method: "POST" });

// ----- Khách xem (không cần đăng nhập) -----

export const getPublicTour = (id: number) => apiRequest<TourDetail>(`/api/tours/${id}`, { auth: false });

export const getPublicItineraryFileUrl = (id: number) =>
  apiRequest<TemporaryUrlResponse>(`/api/tours/${id}/itinerary-file/url`, { auth: false });
