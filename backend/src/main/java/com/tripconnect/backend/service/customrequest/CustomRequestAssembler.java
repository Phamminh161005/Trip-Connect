package com.tripconnect.backend.service.customrequest;

import com.tripconnect.backend.dto.LocationResponse;
import com.tripconnect.backend.dto.TourCategoryResponse;
import com.tripconnect.backend.dto.customrequest.CustomRequestResponses;
import com.tripconnect.backend.dto.tour.TourResponses;
import com.tripconnect.backend.entity.*;
import com.tripconnect.backend.enums.AssignmentStatus;
import com.tripconnect.backend.enums.CustomRequestStatus;
import com.tripconnect.backend.repository.AgentProfileRepository;
import com.tripconnect.backend.repository.ChatMessageRepository;
import com.tripconnect.backend.repository.CustomProposalRepository;
import com.tripconnect.backend.repository.CustomRequestAssignmentRepository;
import com.tripconnect.backend.service.DisplayNames;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Chuyển yêu cầu thành DTO theo người xem. Gọi trong transaction.
 *  - Khách: chỉ thấy tên đơn vị khi Agent đã nhận.
 *  - Agent: tên khách rút gọn, không có email / SĐT (liên hệ qua kênh trao đổi trên hệ thống).
 *  - Admin: đầy đủ + lịch sử giao việc + đề xuất của mọi đơn vị từng phụ trách.
 */
@Component
@RequiredArgsConstructor
public class CustomRequestAssembler {

    public enum Viewer { CUSTOMER, AGENT, ADMIN }

    private final AgentProfileRepository agentProfileRepository;
    private final CustomRequestAssignmentRepository assignmentRepository;
    private final CustomProposalRepository proposalRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final com.tripconnect.backend.repository.BookingRepository bookingRepository;

    static LocationResponse toLocation(Location l) {
        return new LocationResponse(l.getId(), l.getCountry(), l.getProvince());
    }

    /** "Hạ Long, Đà Nẵng, Nhật Bản" */
    public static String destinationsText(CustomRequest r) {
        return r.getDestinations().stream()
                .map(l -> l.getProvince() != null ? l.getProvince() : l.getCountry())
                .sorted().collect(Collectors.joining(", "));
    }

    private static List<LocationResponse> destinations(CustomRequest r) {
        return r.getDestinations().stream().sorted(Comparator.comparing(Location::getId)).map(CustomRequestAssembler::toLocation).toList();
    }

    public Map<Long, String> companyNames(Collection<Long> agentIds) {
        if (agentIds.isEmpty()) return Map.of();
        return agentProfileRepository.findByUserIdIn(agentIds).stream()
                .collect(Collectors.toMap(p -> p.getUser().getId(), AgentProfile::getCompanyName, (a, b) -> a));
    }

    public String companyName(User agent) {
        if (agent == null) return null;
        return companyNames(List.of(agent.getId())).getOrDefault(agent.getId(), agent.getFullName());
    }

    private static String customerName(CustomRequest r, Viewer viewer) {
        return viewer == Viewer.AGENT ? DisplayNames.masked(r.getCustomer().getFullName()) : r.getCustomer().getFullName();
    }

    private static boolean agentVisibleTo(CustomRequest r, Viewer viewer) {
        return viewer != Viewer.CUSTOMER || r.getStatus() == CustomRequestStatus.IN_PROGRESS || r.getAcceptedAt() != null;
    }

    /** Đang chờ ai (chỉ khi IN_PROGRESS). */
    static CustomRequestResponses.Stage stage(CustomRequest r) {
        if (r.getStatus() != CustomRequestStatus.IN_PROGRESS) return null;
        if (r.getProposalDeadline() == null) return CustomRequestResponses.Stage.WAITING_CUSTOMER;
        return r.getRevisionCount() > 0 ? CustomRequestResponses.Stage.REVISING : CustomRequestResponses.Stage.DRAFTING;
    }

    /**
     * Danh sách của khách / Admin (lần giao đang chờ lấy từ chính yêu cầu).
     *
     * @param viewerId người xem — đếm tin nhắn chưa đọc (khách); Admin truyền null
     */
    public List<CustomRequestResponses.Summary> toSummaries(List<CustomRequest> requests, Viewer viewer, Long viewerId) {
        Map<Long, String> names = companyNames(requests.stream().map(CustomRequest::getAgent).filter(Objects::nonNull)
                .map(User::getId).distinct().toList());
        Map<Long, Long> unread = viewerId == null ? Map.of()
                : unreadMessages(requests.stream().map(CustomRequest::getId).toList(), viewerId, false);
        return requests.stream().map(r -> summary(r, viewer,
                r.getAgent() != null && agentVisibleTo(r, viewer) ? names.get(r.getAgent().getId()) : null,
                null, unread.getOrDefault(r.getId(), 0L))).toList();
    }

    /** Danh sách của Agent: mỗi dòng là một lần được giao. */
    public List<CustomRequestResponses.Summary> toAgentSummaries(List<CustomRequestAssignment> assignments, Long agentId) {
        Map<Long, Long> unread = unreadMessages(assignments.stream().map(a -> a.getRequest().getId()).toList(), agentId, true);
        return assignments.stream().map(a -> summary(a.getRequest(), Viewer.AGENT, null, a,
                unread.getOrDefault(a.getRequest().getId(), 0L))).toList();
    }

    /** [requestId -> số tin chưa đọc]; Agent chỉ tính cuộc trò chuyện của mình. */
    private Map<Long, Long> unreadMessages(List<Long> requestIds, Long userId, boolean agentOnly) {
        if (requestIds.isEmpty()) return Map.of();
        return chatMessageRepository.countUnreadByRequest(requestIds, userId, agentOnly).stream()
                .collect(Collectors.toMap(row -> ((Number) row[0]).longValue(), row -> ((Number) row[1]).longValue()));
    }

    private CustomRequestResponses.Summary summary(CustomRequest r, Viewer viewer, String agentName, CustomRequestAssignment mine,
                                                   long unreadMessages) {
        return new CustomRequestResponses.Summary(
                r.getId(), r.getCode(), r.getStatus(), toLocation(r.getDepartureLocation()), destinations(r),
                r.getEarliestStart(), r.getLatestStart(), r.getDurationDays(), r.getAdults(), r.getChildren(), r.getInfants(),
                r.getBudgetMin(), r.getBudgetMax(), customerName(r, viewer), agentName,
                mine != null ? mine.getStatus() : null,
                mine != null ? mine.getDeadline() : null,
                stage(r), r.getProposalDeadline(), unreadMessages,
                r.getCreatedAt());
    }

    /**
     * @param mine lần giao gần nhất cho Agent đang xem (chỉ dùng khi viewer = AGENT)
     */
    public CustomRequestResponses.Detail toDetail(CustomRequest r, Viewer viewer, CustomRequestAssignment mine,
                                                  boolean canCancel, boolean canRespond) {
        boolean admin = viewer == Viewer.ADMIN;
        List<CustomRequestResponses.AssignmentView> history = List.of();
        if (admin) {
            List<CustomRequestAssignment> rows = assignmentRepository.findByRequestIdOrderByIdAsc(r.getId());
            Map<Long, String> names = companyNames(rows.stream().map(a -> a.getAgent().getId()).distinct().toList());
            history = rows.stream().map(a -> new CustomRequestResponses.AssignmentView(
                    a.getId(), a.getAgent().getId(), names.getOrDefault(a.getAgent().getId(), a.getAgent().getFullName()),
                    a.getStatus(), a.getMatchScore(), a.getDeadline(), a.getDeclineReason(), a.getRespondedAt(),
                    a.getAssignedBy().getFullName(), a.getCreatedAt())).toList();
        }
        boolean showAgent = r.getAgent() != null && agentVisibleTo(r, viewer);
        List<CustomProposal> proposals = visibleProposals(r, viewer, mine);
        CustomProposal latest = proposals.isEmpty() ? null : proposals.get(proposals.size() - 1);
        // Agent bị thay / khách khác không tới được đây; đơn chỉ có khi đã chốt
        Booking booking = r.getAgreedAt() == null ? null
                : bookingRepository.findByCustomRequestId(r.getId()).stream().findFirst().orElse(null);
        return new CustomRequestResponses.Detail(
                r.getId(), r.getCode(), r.getStatus(), toLocation(r.getDepartureLocation()), destinations(r),
                r.getCategories().stream().sorted(Comparator.comparing(TourCategory::getId))
                        .map(c -> new TourCategoryResponse(c.getId(), c.getName())).toList(),
                new TreeSet<>(r.getTransportModes()), r.getAccommodationType(),
                r.getEarliestStart(), r.getLatestStart(), r.getDurationDays(), r.getAdults(), r.getChildren(), r.getInfants(),
                r.getBudgetMin(), r.getBudgetMax(), r.getNotes(),
                customerName(r, viewer),
                admin ? r.getCustomer().getEmail() : null,
                admin ? r.getCustomer().getPhone() : null,
                showAgent ? r.getAgent().getId() : null,
                showAgent ? companyName(r.getAgent()) : null,
                r.getAssignedAt(), r.getAcceptedAt(), r.getProposalDeadline(), r.getClosedAt(), r.getClosedReason(),
                mine != null ? mine.getStatus() : null,
                mine != null ? mine.getDeadline() : null,
                history, canCancel, canRespond,
                stage(r), r.getRevisionCount(), CustomRequestRules.MAX_REVISIONS, r.getAgreedAt(),
                toProposals(proposals, r, admin),
                canPropose(r, viewer, mine),
                viewer == Viewer.CUSTOMER && CustomRequestRules.canAcceptProposal(r, latest),
                viewer == Viewer.CUSTOMER && CustomRequestRules.canRequestRevision(r, latest),
                booking == null ? null : booking.getId(),
                booking == null ? null : booking.getCode(),
                booking == null ? null : booking.getStatus(),
                r.getCreatedAt());
    }

    /** Đề xuất người xem được thấy: khách — của đơn vị đang phụ trách; Agent — của mình; Admin — tất cả. */
    private List<CustomProposal> visibleProposals(CustomRequest r, Viewer viewer, CustomRequestAssignment mine) {
        return switch (viewer) {
            case ADMIN -> proposalRepository.findByRequestIdOrderByIdAsc(r.getId());
            case AGENT -> mine == null ? List.of()
                    : proposalRepository.findByRequestIdAndAgentIdOrderByIdAsc(r.getId(), mine.getAgent().getId());
            case CUSTOMER -> r.getAgent() == null || r.getAcceptedAt() == null ? List.of()
                    : proposalRepository.findByRequestIdAndAgentIdOrderByIdAsc(r.getId(), r.getAgent().getId());
        };
    }

    private static boolean canPropose(CustomRequest r, Viewer viewer, CustomRequestAssignment mine) {
        return viewer == Viewer.AGENT && mine != null && mine.getStatus() == AssignmentStatus.ACCEPTED
                && r.getAgent() != null && r.getAgent().getId().equals(mine.getAgent().getId())
                && CustomRequestRules.awaitingAgent(r);
    }

    private List<CustomRequestResponses.ProposalView> toProposals(List<CustomProposal> proposals, CustomRequest r, boolean admin) {
        Map<Long, String> names = admin
                ? companyNames(proposals.stream().map(p -> p.getAgent().getId()).distinct().toList()) : Map.of();
        return proposals.stream().map(p -> toProposal(p, admin ? names.getOrDefault(p.getAgent().getId(), p.getAgent().getFullName()) : null))
                .toList();
    }

    static CustomRequestResponses.ProposalView toProposal(CustomProposal p, String agentName) {
        return new CustomRequestResponses.ProposalView(
                p.getId(), p.getVersionNo(), p.getStatus(), agentName, p.getTitle(), p.getStartDate(), p.getEndDate(),
                p.getDurationDays(), p.getDurationNights(),
                p.getDays().stream().map(d -> new TourResponses.ItineraryDay(d.getDayNumber(), d.getTitle(), d.getDescription(),
                        d.isHasBreakfast(), d.isHasLunch(), d.isHasDinner(), d.getAccommodation())).toList(),
                new TreeSet<>(p.getTransportModes()), p.getAccommodationType(), p.getMeetingPoint(), p.getMeetingTime(),
                List.copyOf(p.getIncludedServices()), List.copyOf(p.getExcludedServices()), p.getNotes(),
                p.getAdultPrice(), p.getChildPrice(), p.getTotalPrice(), p.getDepositAmount(),
                p.getAgentMessage(), p.getCustomerFeedback(), p.getExpiresAt(), p.getRespondedAt(), p.getCreatedAt());
    }
}
