// Khớp các DTO module tour của Backend (TourResponses, TourContentRequest, DepartureRequest)
import type { LocationResponse, TourCategoryResponse } from "./catalog";

export type TourStatus = "DRAFT" | "PENDING_APPROVAL" | "NEEDS_REVISION" | "PUBLISHED" | "HIDDEN" | "SUSPENDED";
export type TransportMode = "BUS" | "PLANE" | "TRAIN" | "BOAT" | "MOTORBIKE" | "SELF_ARRANGED";
export type AccommodationType = "NONE" | "HOMESTAY" | "HOTEL_2_3_STAR" | "HOTEL_4_STAR" | "HOTEL_5_STAR" | "RESORT" | "CRUISE";
export type DepartureStatus = "OPEN" | "CLOSED" | "CANCELLED";

/** Đơn vị tổ chức. null = tour của TripConnect. */
export interface TourProvider {
  agentProfileId: number;
  companyName: string | null;
  rating: number | null;
  ratingCount: number;
}

export interface TourImage {
  id: number;
  url: string;
  sortOrder: number;
}

export interface TourItineraryDay {
  dayNumber: number;
  title: string;
  description: string;
  breakfast: boolean;
  lunch: boolean;
  dinner: boolean;
  accommodation: string | null;
}

export interface TourDeparture {
  id: number;
  /** yyyy-MM-dd */
  startDate: string;
  endDate: string;
  capacity: number;
  seatsBooked: number;
  seatsAvailable: number;
  adultPrice: number;
  childPrice: number;
  status: DepartureStatus;
  cancelReason: string | null;
  /** Đã tới ngày đi */
  departed: boolean;
  /** Khách đặt được ngay lúc này */
  bookable: boolean;
}

export interface TourDetail {
  id: number;
  title: string;
  status: TourStatus;
  statusReason: string | null;
  platformTour: boolean;
  provider: TourProvider | null;
  categories: TourCategoryResponse[];
  departureLocation: LocationResponse;
  destinations: LocationResponse[];
  international: boolean;
  durationDays: number;
  durationNights: number;
  highlights: string[];
  itinerary: TourItineraryDay[];
  transportModes: TransportMode[];
  accommodationType: AccommodationType;
  meetingPoint: string;
  /** HH:mm */
  meetingTime: string;
  includedServices: string[];
  excludedServices: string[];
  notes: string | null;
  images: TourImage[];
  itineraryFile: { fileName: string; sizeBytes: number } | null;
  departures: TourDeparture[];
  rating: number | null;
  ratingCount: number;
  submittedAt: string | null;
  reviewedAt: string | null;
  publishedAt: string | null;
  createdAt: string;
  updatedAt: string | null;
  /** Việc còn thiếu trước khi gửi duyệt / công khai (rỗng = đủ điều kiện) */
  missingItems: string[];
}

export interface TourSummary {
  id: number;
  title: string;
  status: TourStatus;
  platformTour: boolean;
  provider: TourProvider | null;
  coverImageUrl: string | null;
  departureLocation: string | null;
  international: boolean;
  durationDays: number;
  durationNights: number;
  openDepartureCount: number;
  /** Giá người lớn thấp nhất trong các lịch đang mở bán; null = chưa có lịch */
  minAdultPrice: number | null;
  submittedAt: string | null;
  publishedAt: string | null;
  createdAt: string;
  updatedAt: string | null;
}

export interface TourContentRequest {
  title: string;
  categoryIds: number[];
  departureLocationId: number;
  destinationIds: number[];
  durationDays: number;
  durationNights: number;
  highlights: string[];
  itinerary: {
    title: string;
    description: string;
    breakfast: boolean;
    lunch: boolean;
    dinner: boolean;
    accommodation: string | null;
  }[];
  transportModes: TransportMode[];
  accommodationType: AccommodationType;
  meetingPoint: string;
  meetingTime: string;
  includedServices: string[];
  excludedServices: string[];
  notes: string | null;
}

export interface DepartureRequest {
  startDate: string;
  capacity: number;
  adultPrice: number;
  childPrice: number;
}
