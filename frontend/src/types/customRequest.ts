// Khớp CustomRequestResponses / CustomRequestRequests của Backend
import type { LocationResponse, TourCategoryResponse } from "./catalog";
import type { AccommodationType, TourItineraryDay, TransportMode } from "./tour";

export type CustomRequestStatus = "NEW" | "WAITING_AGENT" | "IN_PROGRESS" | "AGREED" | "CANCELLED" | "CLOSED";
export type AssignmentStatus = "PENDING" | "ACCEPTED" | "DECLINED" | "EXPIRED" | "REVOKED" | "OVERDUE";
export type ProposalStatus = "SENT" | "ACCEPTED" | "REVISION_REQUESTED" | "DECLINED" | "EXPIRED" | "WITHDRAWN";
/** Đang chờ ai (chỉ khi IN_PROGRESS) */
export type RequestStage = "DRAFTING" | "REVISING" | "WAITING_CUSTOMER";

export interface CustomRequestSummary {
  id: number;
  code: string;
  status: CustomRequestStatus;
  departureLocation: LocationResponse;
  destinations: LocationResponse[];
  earliestStart: string;
  latestStart: string;
  durationDays: number;
  adults: number;
  children: number;
  infants: number;
  budgetMin: number | null;
  budgetMax: number | null;
  customerName: string;
  agentName: string | null;
  /** Danh sách của Agent: lần giao cho mình */
  assignmentStatus: AssignmentStatus | null;
  assignmentDeadline: string | null;
  stage: RequestStage | null;
  /** Hạn Agent gửi đề xuất tiếp theo */
  proposalDeadline: string | null;
  createdAt: string;
}

export interface AssignmentView {
  id: number;
  agentId: number;
  agentName: string;
  status: AssignmentStatus;
  matchScore: number | null;
  deadline: string;
  declineReason: string | null;
  respondedAt: string | null;
  assignedByName: string;
  createdAt: string;
}

export interface CustomRequestDetail {
  id: number;
  code: string;
  status: CustomRequestStatus;
  departureLocation: LocationResponse;
  destinations: LocationResponse[];
  categories: TourCategoryResponse[];
  transportModes: TransportMode[];
  accommodationType: AccommodationType | null;
  earliestStart: string;
  latestStart: string;
  durationDays: number;
  adults: number;
  children: number;
  infants: number;
  budgetMin: number | null;
  budgetMax: number | null;
  notes: string | null;
  customerName: string;
  /** Chỉ Admin */
  customerEmail: string | null;
  customerPhone: string | null;
  agentId: number | null;
  agentName: string | null;
  assignedAt: string | null;
  acceptedAt: string | null;
  proposalDeadline: string | null;
  closedAt: string | null;
  closedReason: string | null;
  myAssignmentStatus: AssignmentStatus | null;
  myAssignmentDeadline: string | null;
  assignments: AssignmentView[];
  canCancel: boolean;
  canRespond: boolean;
  stage: RequestStage | null;
  revisionCount: number;
  maxRevisions: number;
  agreedAt: string | null;
  /** Cũ trước, mới sau */
  proposals: ProposalView[];
  canPropose: boolean;
  canAcceptProposal: boolean;
  canRequestRevision: boolean;
  createdAt: string;
}

export interface ProposalView {
  id: number;
  versionNo: number;
  status: ProposalStatus;
  /** Chỉ Admin */
  agentName: string | null;
  title: string;
  startDate: string;
  endDate: string;
  durationDays: number;
  durationNights: number;
  itinerary: TourItineraryDay[];
  transportModes: TransportMode[];
  accommodationType: AccommodationType;
  meetingPoint: string;
  /** HH:mm */
  meetingTime: string;
  includedServices: string[];
  excludedServices: string[];
  notes: string | null;
  adultPrice: number;
  childPrice: number;
  totalPrice: number;
  depositAmount: number;
  agentMessage: string | null;
  customerFeedback: string | null;
  expiresAt: string;
  respondedAt: string | null;
  createdAt: string;
}

export interface ProposalRequest {
  title: string;
  startDate: string;
  durationDays: number;
  durationNights: number;
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
  adultPrice: number;
  childPrice: number;
  message: string | null;
}

export interface AgentCandidate {
  agentId: number;
  agentProfileId: number;
  companyName: string;
  rating: number | null;
  ratingCount: number;
  locationScore: number;
  ratingScore: number;
  totalScore: number;
  openRequests: number;
  maxOpenRequests: number;
  matchedAreas: string[];
  lastLoginAt: string | null;
  recommended: boolean;
}

export interface CreateCustomRequest {
  departureLocationId: number;
  destinationIds: number[];
  earliestStart: string;
  latestStart: string;
  durationDays: number;
  adults: number;
  children: number;
  infants: number;
  budgetMin: number | null;
  budgetMax: number | null;
  categoryIds: number[];
  transportModes: TransportMode[];
  accommodationType: AccommodationType | null;
  notes: string | null;
}
