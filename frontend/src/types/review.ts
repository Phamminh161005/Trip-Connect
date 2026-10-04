// Khớp ReviewResponses của Backend

export interface ReviewImage {
  id: number;
  url: string;
}

export interface PublicReview {
  id: number;
  rating: number;
  comment: string;
  /** Đã rút gọn, vd "Phạm V. Minh" */
  reviewerName: string;
  departureDate: string;
  images: ReviewImage[];
  reply: string | null;
  repliedAt: string | null;
  createdAt: string;
  edited: boolean;
}

export interface PublicReviewPage {
  items: PublicReview[];
  nextCursor: number | null;
}

export interface ReviewSummary {
  average: number | null;
  count: number;
  /** số sao ("5".."1") -> số lượt */
  distribution: Record<string, number>;
}

export interface MyReview {
  id: number;
  bookingId: number;
  tourId: number;
  rating: number;
  comment: string;
  images: ReviewImage[];
  reply: string | null;
  repliedAt: string | null;
  hidden: boolean;
  hiddenReason: string | null;
  canEdit: boolean;
  editableUntil: string;
  createdAt: string;
}

export interface ManagedReview {
  id: number;
  tourId: number;
  tourTitle: string;
  bookingId: number;
  bookingCode: string;
  customerName: string;
  customerEmail: string;
  departureDate: string;
  rating: number;
  comment: string;
  images: ReviewImage[];
  reply: string | null;
  repliedAt: string | null;
  hidden: boolean;
  hiddenReason: string | null;
  hiddenAt: string | null;
  platformTour: boolean;
  createdAt: string;
}

export interface WriteReviewRequest {
  rating: number;
  comment: string;
}
