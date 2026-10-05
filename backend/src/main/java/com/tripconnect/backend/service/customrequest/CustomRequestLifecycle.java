package com.tripconnect.backend.service.customrequest;

import com.tripconnect.backend.entity.CustomProposal;
import com.tripconnect.backend.entity.CustomRequest;
import com.tripconnect.backend.entity.CustomRequestAssignment;
import com.tripconnect.backend.entity.User;
import com.tripconnect.backend.enums.AssignmentStatus;
import com.tripconnect.backend.enums.CustomRequestStatus;
import com.tripconnect.backend.enums.ProposalStatus;
import com.tripconnect.backend.repository.CustomProposalRepository;
import com.tripconnect.backend.repository.CustomRequestAssignmentRepository;
import com.tripconnect.backend.repository.CustomRequestRepository;
import com.tripconnect.backend.service.EmailTemplates;
import com.tripconnect.backend.service.NotificationEvents;
import com.tripconnect.backend.service.WebNotifications;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Function;

/**
 * Các chuyển trạng thái dùng chung + việc chạy định kỳ: hết hạn nhận (48 giờ), hạn gửi đề xuất, hạn khách phản hồi,
 * tự đóng yêu cầu bị bỏ dở / sát ngày khởi hành.
 */
@Slf4j
@Component
public class CustomRequestLifecycle {

    private final CustomRequestRepository requestRepository;
    private final CustomRequestAssignmentRepository assignmentRepository;
    private final CustomProposalRepository proposalRepository;
    private final CustomRequestAssembler assembler;
    private final ApplicationEventPublisher eventPublisher;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;
    private final String frontendUrl;

    public CustomRequestLifecycle(CustomRequestRepository requestRepository, CustomRequestAssignmentRepository assignmentRepository,
                                  CustomProposalRepository proposalRepository, CustomRequestAssembler assembler,
                                  ApplicationEventPublisher eventPublisher, TransactionTemplate transactionTemplate, Clock clock,
                                  @Value("${app.frontend-url}") String frontendUrl) {
        this.requestRepository = requestRepository;
        this.assignmentRepository = assignmentRepository;
        this.proposalRepository = proposalRepository;
        this.assembler = assembler;
        this.eventPublisher = eventPublisher;
        this.transactionTemplate = transactionTemplate;
        this.clock = clock;
        this.frontendUrl = frontendUrl.replaceAll("/+$", "");
    }

    public String url(String path) {
        return frontendUrl + path;
    }

    /** Lần giao đang chờ Agent phản hồi -> REVOKED (yêu cầu bị hủy / đóng). */
    void revokePending(CustomRequest request, LocalDateTime now) {
        assignmentRepository.findFirstByRequestIdAndStatus(request.getId(), AssignmentStatus.PENDING).ifPresent(a -> {
            a.setStatus(AssignmentStatus.REVOKED);
            a.setRespondedAt(now);
        });
    }

    /**
     * Kết thúc yêu cầu (khách hủy -> CANCELLED; Admin / hệ thống đóng -> CLOSED). Đề xuất đang chờ khách bị gỡ.
     * Agent đang được giao / đang xử lý nhận thông báo; khách nhận thông báo + email khi bị đóng (khách tự hủy thì không cần).
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void finish(CustomRequest request, CustomRequestStatus status, String reason) {
        LocalDateTime now = LocalDateTime.now(clock);
        revokePending(request, now);
        proposalRepository.findFirstByRequestIdAndStatus(request.getId(), ProposalStatus.SENT).ifPresent(p -> {
            p.setStatus(status == CustomRequestStatus.CANCELLED ? ProposalStatus.DECLINED : ProposalStatus.WITHDRAWN);
            p.setRespondedAt(now);
        });
        Long agentId = request.getAgent() == null ? null : request.getAgent().getId();
        request.setStatus(status);
        request.setProposalDeadline(null);
        request.setClosedAt(now);
        request.setClosedReason(reason);
        if (agentId != null) {
            eventPublisher.publishEvent(new NotificationEvents.UserWebEvent(agentId,
                    WebNotifications.customRequestCancelled(request.getId(), request.getCode(), reason)));
        }
        if (status == CustomRequestStatus.CLOSED) {
            String path = "/account/requests/" + request.getId();
            eventPublisher.publishEvent(new NotificationEvents.UserWebEvent(request.getCustomer().getId(),
                    WebNotifications.customRequestClosed(request.getId(), request.getCode(), reason)));
            eventPublisher.publishEvent(new NotificationEvents.UserEmailEvent(request.getCustomer().getEmail(),
                    EmailTemplates.customRequestClosed(request.getCode(), reason, url(path))));
        }
    }

    /** Về hàng chờ Admin giao (Agent từ chối / hết hạn nhận / quá hạn gửi đề xuất). Lượt chỉnh sửa tính lại với đơn vị mới. */
    void backToQueue(CustomRequest request) {
        request.setStatus(CustomRequestStatus.NEW);
        request.setAgent(null);
        request.setAssignedAt(null);
        request.setAcceptedAt(null);
        request.setProposalDeadline(null);
        request.setDeadlineReminded(false);
        request.setRevisionCount((short) 0);
        request.setLastActivityAt(null);
    }

    // ===================== Việc định kỳ =====================

    /** Chạy từng yêu cầu trong transaction riêng (lỗi một yêu cầu không chặn các yêu cầu khác). Trả về số yêu cầu đã xử lý. */
    private int forEach(List<Long> ids, String job, Function<Long, Boolean> action) {
        int done = 0;
        for (Long id : ids) {
            try {
                Boolean changed = transactionTemplate.execute(status -> action.apply(id));
                if (Boolean.TRUE.equals(changed)) done++;
            } catch (RuntimeException e) {
                log.warn("{} - yêu cầu id={} lỗi: {}", job, id, e.getMessage());
            }
        }
        return done;
    }

    private CustomRequest lock(Long id) {
        return requestRepository.findByIdForUpdate(id).orElseThrow();
    }

    /** Lần giao quá 48 giờ chưa phản hồi -> EXPIRED, yêu cầu quay lại chờ Admin giao người khác. */
    public int expireOverdueAssignments() {
        return forEach(assignmentRepository.findRequestIdsWithExpiredAssignment(LocalDateTime.now(clock)),
                "Hết hạn nhận yêu cầu", this::expireAssignment);
    }

    private boolean expireAssignment(Long requestId) {
        LocalDateTime now = LocalDateTime.now(clock);
        CustomRequest request = lock(requestId);
        CustomRequestAssignment assignment = assignmentRepository
                .findFirstByRequestIdAndStatus(requestId, AssignmentStatus.PENDING).orElse(null);
        if (assignment == null || !assignment.getDeadline().isBefore(now)
                || request.getStatus() != CustomRequestStatus.WAITING_AGENT) return false;

        assignment.setStatus(AssignmentStatus.EXPIRED);
        assignment.setRespondedAt(now);
        String companyName = assembler.companyName(assignment.getAgent());
        backToQueue(request);
        eventPublisher.publishEvent(new NotificationEvents.UserWebEvent(assignment.getAgent().getId(),
                WebNotifications.customRequestExpired(request.getId(), request.getCode())));
        eventPublisher.publishEvent(new NotificationEvents.AdminWebEvent(WebNotifications.customRequestNeedsReassign(
                request.getId(), request.getCode(), companyName + " không phản hồi kịp")));
        return true;
    }

    /** Agent quá hạn gửi đề xuất (bản đầu hoặc bản chỉnh sửa) -> OVERDUE, yêu cầu quay lại chờ Admin giao đơn vị khác. */
    public int dropOverdueAgents() {
        return forEach(requestRepository.findProposalDueBefore(LocalDateTime.now(clock), false),
                "Quá hạn gửi đề xuất", this::dropOverdueAgent);
    }

    private boolean dropOverdueAgent(Long requestId) {
        LocalDateTime now = LocalDateTime.now(clock);
        CustomRequest request = lock(requestId);
        if (!CustomRequestRules.awaitingAgent(request) || request.getProposalDeadline().isAfter(now)) return false;

        User agent = request.getAgent();
        assignmentRepository.findFirstByRequestIdAndStatus(requestId, AssignmentStatus.ACCEPTED).ifPresent(a -> {
            a.setStatus(AssignmentStatus.OVERDUE);
            a.setRespondedAt(now);
        });
        String companyName = assembler.companyName(agent);
        backToQueue(request);
        eventPublisher.publishEvent(new NotificationEvents.UserWebEvent(agent.getId(),
                WebNotifications.customProposalOverdue(request.getId(), request.getCode())));
        eventPublisher.publishEvent(new NotificationEvents.UserWebEvent(request.getCustomer().getId(),
                WebNotifications.customRequestReassigning(request.getId(), request.getCode(), companyName)));
        eventPublisher.publishEvent(new NotificationEvents.AdminWebEvent(WebNotifications.customRequestNeedsReassign(
                request.getId(), request.getCode(), companyName + " quá hạn gửi đề xuất")));
        return true;
    }

    /** Còn dưới 24 giờ tới hạn gửi đề xuất -> nhắc Agent một lần. */
    public int remindAgentsDueSoon() {
        LocalDateTime before = LocalDateTime.now(clock).plusHours(CustomRequestRules.REMIND_BEFORE_HOURS);
        return forEach(requestRepository.findProposalDueBefore(before, true), "Nhắc hạn gửi đề xuất", id -> {
            LocalDateTime now = LocalDateTime.now(clock);
            CustomRequest request = lock(id);
            if (!CustomRequestRules.awaitingAgent(request) || request.isDeadlineReminded()
                    || !request.getProposalDeadline().isAfter(now)) return false;
            request.setDeadlineReminded(true);
            eventPublisher.publishEvent(new NotificationEvents.UserWebEvent(request.getAgent().getId(),
                    WebNotifications.customProposalDueSoon(request.getId(), request.getCode(), request.getProposalDeadline())));
            return true;
        });
    }

    /** Còn dưới 24 giờ để khách phản hồi đề xuất -> nhắc khách một lần. */
    public int remindCustomersExpiring() {
        LocalDateTime before = LocalDateTime.now(clock).plusHours(CustomRequestRules.REMIND_BEFORE_HOURS);
        int done = 0;
        for (Long proposalId : proposalRepository.findExpiringUnreminded(before)) {
            try {
                Boolean reminded = transactionTemplate.execute(status -> {
                    CustomProposal p = proposalRepository.findById(proposalId).orElseThrow();
                    CustomRequest request = lock(p.getRequest().getId());
                    if (p.getStatus() != ProposalStatus.SENT || p.isExpiryReminded()
                            || !p.getExpiresAt().isAfter(LocalDateTime.now(clock))) return false;
                    p.setExpiryReminded(true);
                    eventPublisher.publishEvent(new NotificationEvents.UserWebEvent(request.getCustomer().getId(),
                            WebNotifications.customProposalExpiring(request.getId(), request.getCode())));
                    return true;
                });
                if (Boolean.TRUE.equals(reminded)) done++;
            } catch (RuntimeException e) {
                log.warn("Nhắc khách phản hồi đề xuất id={} lỗi: {}", proposalId, e.getMessage());
            }
        }
        return done;
    }

    /** Khách không phản hồi đề xuất trong 3 ngày -> EXPIRED (vẫn yêu cầu chỉnh sửa được nếu còn lượt). */
    public int expireUnansweredProposals() {
        return forEach(proposalRepository.findRequestIdsWithExpiredProposal(LocalDateTime.now(clock)),
                "Hết hạn phản hồi đề xuất", requestId -> {
                    LocalDateTime now = LocalDateTime.now(clock);
                    CustomRequest request = lock(requestId);
                    CustomProposal p = proposalRepository.findFirstByRequestIdAndStatus(requestId, ProposalStatus.SENT).orElse(null);
                    if (p == null || p.getExpiresAt().isAfter(now)) return false;
                    p.setStatus(ProposalStatus.EXPIRED);
                    eventPublisher.publishEvent(new NotificationEvents.UserWebEvent(request.getCustomer().getId(),
                            WebNotifications.customProposalExpiredForCustomer(request.getId(), request.getCode(),
                                    CustomRequestRules.canRequestRevision(request, p), CustomRequestRules.INACTIVE_CLOSE_DAYS)));
                    eventPublisher.publishEvent(new NotificationEvents.UserWebEvent(p.getAgent().getId(),
                            WebNotifications.customProposalExpiredForAgent(request.getId(), request.getCode())));
                    return true;
                });
    }

    /** Đang chờ khách mà không có trao đổi nào trong 5 ngày -> đóng. */
    public int closeInactive() {
        LocalDateTime cutoff = LocalDateTime.now(clock).minusDays(CustomRequestRules.INACTIVE_CLOSE_DAYS);
        return forEach(requestRepository.findInactiveSince(cutoff), "Đóng yêu cầu không hoạt động", id -> {
            CustomRequest request = lock(id);
            if (request.getStatus() != CustomRequestStatus.IN_PROGRESS || request.getProposalDeadline() != null
                    || request.getLastActivityAt() == null || !request.getLastActivityAt().isBefore(cutoff)) return false;
            finish(request, CustomRequestStatus.CLOSED,
                    "Không có trao đổi nào trong " + CustomRequestRules.INACTIVE_CLOSE_DAYS + " ngày sau đề xuất gần nhất");
            return true;
        });
    }

    /** Chưa chốt được mà ngày khởi hành muộn nhất đã quá sát -> đóng, báo khách. */
    public int closeOpenNearStart() {
        LocalDate before = LocalDate.now(clock).plusDays(CustomRequestRules.AUTO_CLOSE_START_WITHIN_DAYS);
        return forEach(requestRepository.findOpenStartingBefore(before), "Tự đóng yêu cầu sát ngày", id -> {
            CustomRequest request = lock(id);
            if (!CustomRequestRules.OPEN_STATUSES.contains(request.getStatus()) || !request.getLatestStart().isBefore(before)) {
                return false;
            }
            finish(request, CustomRequestStatus.CLOSED, request.getStatus() == CustomRequestStatus.IN_PROGRESS
                    ? "Chưa chốt được lịch trình kịp trước thời gian khởi hành mong muốn"
                    : "Chưa tìm được đơn vị tổ chức phù hợp kịp trước thời gian khởi hành mong muốn");
            return true;
        });
    }
}
