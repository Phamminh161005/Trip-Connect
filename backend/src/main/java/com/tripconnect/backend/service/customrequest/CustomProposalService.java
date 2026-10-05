package com.tripconnect.backend.service.customrequest;

import com.tripconnect.backend.dto.customrequest.CustomRequestRequests;
import com.tripconnect.backend.dto.customrequest.CustomRequestResponses;
import com.tripconnect.backend.dto.tour.TourContentRequest.ItineraryDayRequest;
import com.tripconnect.backend.entity.CustomProposal;
import com.tripconnect.backend.entity.CustomProposalDay;
import com.tripconnect.backend.entity.CustomRequest;
import com.tripconnect.backend.entity.CustomRequestAssignment;
import com.tripconnect.backend.enums.CustomRequestStatus;
import com.tripconnect.backend.enums.ProposalStatus;
import com.tripconnect.backend.exception.ResourceNotFoundException;
import com.tripconnect.backend.repository.CustomProposalRepository;
import com.tripconnect.backend.repository.CustomRequestAssignmentRepository;
import com.tripconnect.backend.repository.CustomRequestRepository;
import com.tripconnect.backend.service.EmailTemplates;
import com.tripconnect.backend.service.NotificationEvents;
import com.tripconnect.backend.service.WebNotifications;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Đề xuất tour riêng: Agent gửi (bản đầu / bản chỉnh sửa), khách đồng ý hoặc yêu cầu chỉnh sửa. */
@Service
@RequiredArgsConstructor
public class CustomProposalService {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final CustomRequestRepository requestRepository;
    private final CustomRequestAssignmentRepository assignmentRepository;
    private final CustomProposalRepository proposalRepository;
    private final CustomRequestAssembler assembler;
    private final CustomRequestLifecycle lifecycle;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    // ===================== Agent =====================

    @Transactional
    public CustomRequestResponses.Detail submit(Long agentId, Long requestId, CustomRequestRequests.Proposal body) {
        LocalDateTime now = LocalDateTime.now(clock);
        CustomRequest request = lock(requestId);
        CustomRequestAssignment mine = assignmentRepository.findFirstByRequestIdAndAgentIdOrderByIdDesc(requestId, agentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu"));
        if (request.getAgent() == null || !request.getAgent().getId().equals(agentId) || !CustomRequestRules.awaitingAgent(request)) {
            throw new IllegalStateException("Yêu cầu không ở bước chờ bạn gửi đề xuất");
        }
        if (!request.getProposalDeadline().isAfter(now)) {
            throw new IllegalStateException("Đã quá hạn gửi đề xuất cho yêu cầu này");
        }
        validate(body, request, now.toLocalDate());

        long total = request.getAdults() * body.getAdultPrice() + request.getChildren() * body.getChildPrice();
        CustomProposal p = new CustomProposal();
        p.setRequest(request);
        p.setAgent(request.getAgent());
        p.setVersionNo((short) (proposalRepository.countByRequestIdAndAgentId(requestId, agentId) + 1));
        p.setStatus(ProposalStatus.SENT);
        p.setTitle(body.getTitle().trim());
        p.setStartDate(body.getStartDate());
        p.setEndDate(body.getStartDate().plusDays(body.getDurationDays() - 1));
        p.setDurationDays(body.getDurationDays().shortValue());
        p.setDurationNights(body.getDurationNights().shortValue());
        p.setAccommodationType(body.getAccommodationType());
        p.getTransportModes().addAll(body.getTransportModes());
        p.setMeetingPoint(body.getMeetingPoint().trim());
        p.setMeetingTime(body.getMeetingTime());
        p.setIncludedServices(body.getIncludedServices().stream().map(String::trim).toList());
        p.setExcludedServices(body.getExcludedServices() == null ? List.of() : body.getExcludedServices().stream().map(String::trim).toList());
        p.setNotes(blankToNull(body.getNotes()));
        p.setAdultPrice(body.getAdultPrice());
        p.setChildPrice(body.getChildPrice());
        p.setTotalPrice(total);
        p.setDepositAmount(CustomRequestRules.deposit(total));
        p.setAgentMessage(blankToNull(body.getMessage()));
        p.setExpiresAt(now.plusDays(CustomRequestRules.RESPONSE_DAYS));
        p.setCreatedAt(now);
        List<ItineraryDayRequest> items = body.getItinerary();
        for (int i = 0; i < items.size(); i++) {
            ItineraryDayRequest item = items.get(i);
            CustomProposalDay day = new CustomProposalDay();
            day.setProposal(p);
            day.setDayNumber((short) (i + 1));
            day.setTitle(item.getTitle().trim());
            day.setDescription(item.getDescription().trim());
            day.setHasBreakfast(item.isBreakfast());
            day.setHasLunch(item.isLunch());
            day.setHasDinner(item.isDinner());
            day.setAccommodation(blankToNull(item.getAccommodation()));
            p.getDays().add(day);
        }
        proposalRepository.save(p);

        request.setProposalDeadline(null);
        request.setDeadlineReminded(false);
        request.setLastActivityAt(now);

        String companyName = assembler.companyName(request.getAgent());
        String path = "/account/requests/" + request.getId();
        eventPublisher.publishEvent(new NotificationEvents.UserWebEvent(request.getCustomer().getId(),
                WebNotifications.customProposalReceived(request.getId(), request.getCode(), companyName, p.getVersionNo(),
                        total, CustomRequestRules.RESPONSE_DAYS)));
        eventPublisher.publishEvent(new NotificationEvents.UserEmailEvent(request.getCustomer().getEmail(),
                EmailTemplates.customProposalReceived(request.getCode(), companyName, p.getVersionNo(), p.getTitle(),
                        p.getStartDate(), p.getEndDate(), total, p.getDepositAmount(), p.getExpiresAt(), lifecycle.url(path))));
        return assembler.toDetail(request, CustomRequestAssembler.Viewer.AGENT, mine, false, false);
    }

    /** Các luật nhiều trường của đề xuất (ràng buộc từng trường đã kiểm ở DTO). */
    static void validate(CustomRequestRequests.Proposal body, CustomRequest request, LocalDate today) {
        LocalDate start = body.getStartDate();
        if (start.isBefore(request.getEarliestStart()) || start.isAfter(request.getLatestStart())) {
            throw new IllegalArgumentException("Ngày khởi hành phải trong khoảng khách mong muốn ("
                    + request.getEarliestStart().format(DAY) + " - " + request.getLatestStart().format(DAY) + ")");
        }
        LocalDate earliestAllowed = today.plusDays(CustomRequestRules.MIN_PROPOSAL_LEAD_DAYS);
        if (start.isBefore(earliestAllowed)) {
            throw new IllegalArgumentException("Ngày khởi hành phải từ " + earliestAllowed.format(DAY) + " (cần ít nhất "
                    + CustomRequestRules.MIN_PROPOSAL_LEAD_DAYS + " ngày để khách đặt cọc và chuẩn bị)");
        }
        int days = body.getDurationDays();
        int nights = body.getDurationNights();
        if (nights != days && nights != days - 1) {
            throw new IllegalArgumentException("Số đêm bằng số ngày hoặc ít hơn 1 (vd 3 ngày 2 đêm)");
        }
        if (body.getItinerary().size() != days) {
            throw new IllegalArgumentException("Số ngày trong lịch trình phải bằng số ngày của chuyến đi");
        }
        if (body.getChildPrice() > body.getAdultPrice()) {
            throw new IllegalArgumentException("Giá trẻ em không được cao hơn giá người lớn");
        }
    }

    // ===================== Khách =====================

    @Transactional
    public CustomRequestResponses.Detail accept(Long customerId, Long requestId, Long proposalId) {
        LocalDateTime now = LocalDateTime.now(clock);
        CustomRequest request = lockMine(customerId, requestId);
        CustomProposal p = requireLatest(request, proposalId);
        if (!CustomRequestRules.canAcceptProposal(request, p) || !p.getExpiresAt().isAfter(now)) {
            throw new IllegalStateException("Đề xuất này không còn chờ bạn phản hồi");
        }
        if (p.getStartDate().isBefore(now.toLocalDate().plusDays(CustomRequestRules.MIN_PROPOSAL_LEAD_DAYS))) {
            throw new IllegalStateException("Ngày khởi hành trong đề xuất đã quá gần, vui lòng yêu cầu đơn vị chỉnh sửa ngày đi");
        }
        p.setStatus(ProposalStatus.ACCEPTED);
        p.setRespondedAt(now);
        request.setStatus(CustomRequestStatus.AGREED);
        request.setAgreedAt(now);
        request.setLastActivityAt(now);

        String path = "/agent/requests/" + request.getId();
        eventPublisher.publishEvent(new NotificationEvents.UserWebEvent(p.getAgent().getId(),
                WebNotifications.customProposalAccepted(request.getId(), request.getCode(), p.getTitle())));
        eventPublisher.publishEvent(new NotificationEvents.UserEmailEvent(p.getAgent().getEmail(),
                EmailTemplates.customProposalAccepted(request.getCode(), request.getCustomer().getFullName(), p.getTitle(),
                        p.getStartDate(), p.getTotalPrice(), lifecycle.url(path))));
        return customerDetail(request);
    }

    @Transactional
    public CustomRequestResponses.Detail requestRevision(Long customerId, Long requestId, Long proposalId, String feedback) {
        if (feedback == null || feedback.isBlank()) throw new IllegalArgumentException("Vui lòng cho biết bạn muốn chỉnh sửa gì");
        LocalDateTime now = LocalDateTime.now(clock);
        CustomRequest request = lockMine(customerId, requestId);
        CustomProposal p = requireLatest(request, proposalId);
        if (!CustomRequestRules.canRequestRevision(request, p)) {
            throw new IllegalStateException(request.getRevisionCount() >= CustomRequestRules.MAX_REVISIONS
                    ? "Đã hết " + CustomRequestRules.MAX_REVISIONS + " lượt chỉnh sửa. Bạn có thể đồng ý hoặc hủy yêu cầu"
                    : "Đề xuất này không còn chờ bạn phản hồi");
        }
        p.setStatus(ProposalStatus.REVISION_REQUESTED);
        p.setCustomerFeedback(feedback.trim());
        p.setRespondedAt(now);
        request.setRevisionCount((short) (request.getRevisionCount() + 1));
        request.setProposalDeadline(now.plusHours(CustomRequestRules.REVISION_HOURS));
        request.setDeadlineReminded(false);
        request.setLastActivityAt(now);

        eventPublisher.publishEvent(new NotificationEvents.UserWebEvent(p.getAgent().getId(),
                WebNotifications.customProposalRevisionRequested(request.getId(), request.getCode(), request.getRevisionCount(),
                        CustomRequestRules.MAX_REVISIONS, CustomRequestRules.REVISION_HOURS)));
        return customerDetail(request);
    }

    private CustomRequestResponses.Detail customerDetail(CustomRequest request) {
        return assembler.toDetail(request, CustomRequestAssembler.Viewer.CUSTOMER, null,
                CustomRequestRules.OPEN_STATUSES.contains(request.getStatus()), false);
    }

    private CustomRequest lock(Long requestId) {
        return requestRepository.findByIdForUpdate(requestId).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu"));
    }

    private CustomRequest lockMine(Long customerId, Long requestId) {
        CustomRequest request = lock(requestId);
        if (!request.getCustomer().getId().equals(customerId)) throw new ResourceNotFoundException("Không tìm thấy yêu cầu");
        return request;
    }

    /** Khách chỉ phản hồi được bản mới nhất của đơn vị đang phụ trách (chống bấm trên trang cũ). */
    private CustomProposal requireLatest(CustomRequest request, Long proposalId) {
        if (request.getAgent() == null) throw new IllegalStateException("Đề xuất này không còn chờ bạn phản hồi");
        CustomProposal latest = proposalRepository.findFirstByRequestIdAndAgentIdOrderByIdDesc(request.getId(), request.getAgent().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đề xuất"));
        if (!latest.getId().equals(proposalId)) {
            throw new IllegalStateException("Đã có phiên bản đề xuất mới hơn, vui lòng tải lại trang");
        }
        return latest;
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
