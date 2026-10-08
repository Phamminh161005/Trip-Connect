package com.tripconnect.backend.service.settlement;

import com.tripconnect.backend.dto.PageResponse;
import com.tripconnect.backend.dto.TemporaryUrlResponse;
import com.tripconnect.backend.dto.settlement.SettlementRequests;
import com.tripconnect.backend.dto.settlement.SettlementResponses;
import com.tripconnect.backend.entity.*;
import com.tripconnect.backend.enums.SettlementDisputeStatus;
import com.tripconnect.backend.enums.SettlementStatus;
import com.tripconnect.backend.exception.ResourceNotFoundException;
import com.tripconnect.backend.repository.AgentProfileRepository;
import com.tripconnect.backend.repository.SettlementRepository;
import com.tripconnect.backend.repository.StablePaging;
import com.tripconnect.backend.repository.UserRepository;
import com.tripconnect.backend.service.EmailTemplates;
import com.tripconnect.backend.service.NotificationEvents;
import com.tripconnect.backend.service.WebNotifications;
import com.tripconnect.backend.storage.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Đối soát hoa hồng:
 *  - Agent: xem bảng đối soát của mình, xác nhận hoặc khiếu nại (trong 5 ngày, quá hạn tự xác nhận).
 *  - Admin: lập ngay, xử lý khiếu nại (điều chỉnh / bác), ghi nhận đã chuyển khoản (mã giao dịch + biên lai).
 */
@Slf4j
@Service
public class SettlementService {

    private static final Duration RECEIPT_URL_TTL = Duration.ofMinutes(5);

    private final SettlementRepository settlementRepository;
    private final AgentProfileRepository agentProfileRepository;
    private final UserRepository userRepository;
    private final SettlementGenerator generator;
    private final FileStorageService fileStorageService;
    private final FileValidator fileValidator;
    private final TransactionalFileCleanup fileCleanup;
    private final ApplicationEventPublisher eventPublisher;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;
    private final String frontendUrl;

    public SettlementService(SettlementRepository settlementRepository, AgentProfileRepository agentProfileRepository,
                             UserRepository userRepository, SettlementGenerator generator, FileStorageService fileStorageService,
                             FileValidator fileValidator, TransactionalFileCleanup fileCleanup,
                             ApplicationEventPublisher eventPublisher, TransactionTemplate transactionTemplate, Clock clock,
                             @Value("${app.frontend-url}") String frontendUrl) {
        this.settlementRepository = settlementRepository;
        this.agentProfileRepository = agentProfileRepository;
        this.userRepository = userRepository;
        this.generator = generator;
        this.fileStorageService = fileStorageService;
        this.fileValidator = fileValidator;
        this.fileCleanup = fileCleanup;
        this.eventPublisher = eventPublisher;
        this.transactionTemplate = transactionTemplate;
        this.clock = clock;
        this.frontendUrl = frontendUrl.replaceAll("/+$", "");
    }

    // ===================== Xem =====================

    /** @param agentId null = Admin (mọi Agent) */
    @Transactional(readOnly = true)
    public PageResponse<SettlementResponses.Summary> list(Long agentId, SettlementStatus status, Pageable pageable) {
        Specification<Settlement> spec = (root, query, cb) -> cb.conjunction();
        if (agentId != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("agent").get("id"), agentId));
        if (status != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        var page = settlementRepository.findAll(spec, StablePaging.of(pageable));
        var names = companyNames(page.getContent().stream().map(s -> s.getAgent().getId()).distinct().toList());
        return PageResponse.of(page, page.getContent().stream().map(s -> summary(s, names)).toList());
    }

    @Transactional(readOnly = true)
    public SettlementResponses.Detail get(Long agentId, Long settlementId) {
        return detail(require(agentId, settlementId), agentId == null);
    }

    @Transactional(readOnly = true)
    public TemporaryUrlResponse receiptUrl(Long agentId, Long settlementId) {
        Settlement s = require(agentId, settlementId);
        if (s.getReceiptPublicId() == null) throw new ResourceNotFoundException("Bảng đối soát không có biên lai");
        return new TemporaryUrlResponse(fileStorageService.temporaryUrl(s.getReceiptPublicId(), s.getReceiptFormat(), RECEIPT_URL_TTL),
                LocalDateTime.now(clock).plus(RECEIPT_URL_TTL));
    }

    // ===================== Agent =====================

    @Transactional
    public SettlementResponses.Detail confirm(Long agentId, Long settlementId) {
        Settlement s = requireForUpdate(agentId, settlementId);
        requireStatus(s, SettlementStatus.PENDING_CONFIRM, "Bảng đối soát không ở trạng thái chờ xác nhận");
        markConfirmed(s, false);
        return detail(s, false);
    }

    @Transactional
    public SettlementResponses.Detail dispute(Long agentId, Long settlementId, String reason) {
        Settlement s = requireForUpdate(agentId, settlementId);
        requireStatus(s, SettlementStatus.PENDING_CONFIRM, "Chỉ khiếu nại được khi bảng đối soát đang chờ xác nhận");
        LocalDateTime now = LocalDateTime.now(clock);
        SettlementDispute dispute = new SettlementDispute();
        dispute.setSettlement(s);
        dispute.setReason(reason.trim());
        dispute.setStatus(SettlementDisputeStatus.OPEN);
        dispute.setCreatedAt(now);
        s.getDisputes().add(dispute);
        s.setStatus(SettlementStatus.DISPUTED);
        s.setConfirmDeadline(null);
        eventPublisher.publishEvent(new NotificationEvents.AdminWebEvent(
                WebNotifications.settlementDisputed(s.getId(), s.getCode(), companyName(s.getAgent()))));
        return detail(s, false);
    }

    // ===================== Admin =====================

    /** Lập ngay cho mọi đơn chưa đối soát tới thời điểm hiện tại (ngoài lượt tự động ngày 1). */
    public int generateNow() {
        return generator.generate(LocalDateTime.now(clock));
    }

    @Transactional
    public SettlementResponses.Detail resolve(Long adminId, Long settlementId, SettlementRequests.Resolve body) {
        Settlement s = requireForUpdate(null, settlementId);
        requireStatus(s, SettlementStatus.DISPUTED, "Bảng đối soát không có khiếu nại đang chờ xử lý");
        SettlementDispute open = s.getDisputes().stream().filter(d -> d.getStatus() == SettlementDisputeStatus.OPEN)
                .reduce((a, b) -> b).orElseThrow(() -> new IllegalStateException("Không có khiếu nại đang chờ xử lý"));
        LocalDateTime now = LocalDateTime.now(clock);
        User admin = userRepository.getReferenceById(adminId);
        boolean adjust = body.decision() == SettlementRequests.Decision.ADJUST;

        if (adjust) {
            List<SettlementRequests.AdjustmentInput> inputs = body.adjustments() == null ? List.of() : body.adjustments();
            if (inputs.isEmpty() || inputs.stream().anyMatch(a -> a.amount() == 0)) {
                throw new IllegalArgumentException("Điều chỉnh cần ít nhất một khoản tiền khác 0");
            }
            for (SettlementRequests.AdjustmentInput input : inputs) {
                SettlementAdjustment adjustment = new SettlementAdjustment();
                adjustment.setSettlement(s);
                adjustment.setAmount(input.amount());
                adjustment.setReason(input.reason().trim());
                adjustment.setCreatedBy(admin);
                adjustment.setCreatedAt(now);
                s.getAdjustments().add(adjustment);
            }
            s.recalculate();
            if (s.getPayoutAmount() < 0) throw new IllegalArgumentException("Sau điều chỉnh, số tiền Agent nhận không được âm");
            s.setStatus(SettlementStatus.PENDING_CONFIRM);
            s.setConfirmDeadline(now.plusDays(SettlementRules.CONFIRM_DAYS));
        }
        open.setStatus(adjust ? SettlementDisputeStatus.ADJUSTED : SettlementDisputeStatus.REJECTED);
        open.setResolution(body.resolution().trim());
        open.setResolvedBy(admin);
        open.setResolvedAt(now);
        eventPublisher.publishEvent(new NotificationEvents.UserWebEvent(s.getAgent().getId(),
                WebNotifications.settlementResolved(s.getId(), s.getCode(), adjust)));
        if (!adjust) markConfirmed(s, false);
        return detail(s, true);
    }

    /** Admin đã chuyển khoản cho Agent: chụp lại tài khoản nhận, ghi mã giao dịch, lưu biên lai (nếu có). */
    @Transactional
    public SettlementResponses.Detail markPaid(Long adminId, Long settlementId, String transactionRef, MultipartFile receipt) {
        if (transactionRef == null || transactionRef.isBlank()) throw new IllegalArgumentException("Vui lòng nhập mã giao dịch ngân hàng");
        if (transactionRef.trim().length() > 100) throw new IllegalArgumentException("Mã giao dịch tối đa 100 ký tự");
        Settlement s = requireForUpdate(null, settlementId);
        requireStatus(s, SettlementStatus.AWAITING_PAYMENT, "Bảng đối soát chưa ở bước chờ thanh toán");
        AgentProfile profile = agentProfileRepository.findByUserId(s.getAgent().getId())
                .filter(p -> p.getBank() != null && p.getBankAccountNumber() != null)
                .orElseThrow(() -> new IllegalStateException("Đơn vị tổ chức chưa có tài khoản ngân hàng nhận tiền"));
        if (receipt != null && !receipt.isEmpty()) {
            fileValidator.validate(receipt, FileRule.IMAGE);
            StoredFile stored = fileStorageService.upload(receipt, "settlements/" + s.getId(), FileVisibility.PRIVATE);
            fileCleanup.deleteOnRollback(stored.publicId(), FileVisibility.PRIVATE);
            s.setReceiptPublicId(stored.publicId());
            s.setReceiptFormat(stored.format());
        }
        s.setBankBin(profile.getBank().getBin());
        s.setBankName(profile.getBank().getShortName());
        s.setBankAccountNumber(profile.getBankAccountNumber());
        s.setBankAccountHolder(profile.getBankAccountHolder());
        s.setTransactionRef(transactionRef.trim());
        s.setPaidAt(LocalDateTime.now(clock));
        s.setPaidBy(userRepository.getReferenceById(adminId));
        s.setStatus(SettlementStatus.PAID);

        eventPublisher.publishEvent(new NotificationEvents.UserWebEvent(s.getAgent().getId(),
                WebNotifications.settlementPaid(s.getId(), s.getCode(), s.getPayoutAmount(), s.getTransactionRef())));
        eventPublisher.publishEvent(new NotificationEvents.UserEmailEvent(s.getAgent().getEmail(),
                EmailTemplates.settlementPaid(s.getCode(), s.getPayoutAmount(), s.getBankName(), s.getBankAccountNumber(),
                        s.getTransactionRef(), frontendUrl + "/agent/settlements/" + s.getId())));
        return detail(s, true);
    }

    // ===================== Việc định kỳ =====================

    /** Quá hạn Agent phản hồi -> tự xác nhận. */
    public int autoConfirmOverdue() {
        int done = 0;
        for (Long id : settlementRepository.findPastConfirmDeadline(LocalDateTime.now(clock))) {
            try {
                Boolean confirmed = transactionTemplate.execute(status -> {
                    Settlement s = settlementRepository.findByIdForUpdate(id).orElseThrow();
                    if (s.getStatus() != SettlementStatus.PENDING_CONFIRM || s.getConfirmDeadline() == null
                            || s.getConfirmDeadline().isAfter(LocalDateTime.now(clock))) return false;
                    markConfirmed(s, true);
                    eventPublisher.publishEvent(new NotificationEvents.UserWebEvent(s.getAgent().getId(),
                            WebNotifications.settlementAutoConfirmed(s.getId(), s.getCode())));
                    return true;
                });
                if (Boolean.TRUE.equals(confirmed)) done++;
            } catch (RuntimeException e) {
                log.warn("Tự xác nhận đối soát id={} lỗi: {}", id, e.getMessage());
            }
        }
        return done;
    }

    // ===================== Tiện ích =====================

    private void markConfirmed(Settlement s, boolean auto) {
        s.setStatus(SettlementStatus.AWAITING_PAYMENT);
        s.setConfirmedAt(LocalDateTime.now(clock));
        s.setAutoConfirmed(auto);
        s.setConfirmDeadline(null);
        eventPublisher.publishEvent(new NotificationEvents.AdminWebEvent(WebNotifications.settlementReady(
                s.getId(), s.getCode(), companyName(s.getAgent()), s.getPayoutAmount())));
    }

    private static void requireStatus(Settlement s, SettlementStatus expected, String message) {
        if (s.getStatus() != expected) throw new IllegalStateException(message);
    }

    /** Agent chỉ thấy bảng của mình (404 nếu không phải). agentId null = Admin. */
    private Settlement require(Long agentId, Long settlementId) {
        return settlementRepository.findById(settlementId)
                .filter(s -> agentId == null || s.getAgent().getId().equals(agentId))
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bảng đối soát"));
    }

    private Settlement requireForUpdate(Long agentId, Long settlementId) {
        return settlementRepository.findByIdForUpdate(settlementId)
                .filter(s -> agentId == null || s.getAgent().getId().equals(agentId))
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bảng đối soát"));
    }

    private java.util.Map<Long, String> companyNames(List<Long> agentIds) {
        if (agentIds.isEmpty()) return java.util.Map.of();
        return agentProfileRepository.findByUserIdIn(agentIds).stream()
                .collect(java.util.stream.Collectors.toMap(p -> p.getUser().getId(), AgentProfile::getCompanyName, (a, b) -> a));
    }

    private String companyName(User agent) {
        return companyNames(List.of(agent.getId())).getOrDefault(agent.getId(), agent.getFullName());
    }

    private static SettlementResponses.Summary summary(Settlement s, java.util.Map<Long, String> names) {
        return new SettlementResponses.Summary(s.getId(), s.getCode(), s.getAgent().getId(),
                names.getOrDefault(s.getAgent().getId(), s.getAgent().getFullName()), SettlementRules.periodLabel(s.getCutoffAt()),
                s.getCutoffAt(), s.getStatus(), s.getItemCount(), s.getRetainedAmount(), s.getCommissionAmount(),
                s.getAdjustmentAmount(), s.getPayoutAmount(), s.getConfirmDeadline(), s.getPaidAt(), s.getCreatedAt());
    }

    private SettlementResponses.Detail detail(Settlement s, boolean admin) {
        var names = companyNames(List.of(s.getAgent().getId()));
        List<SettlementResponses.Item> items = s.getItems().stream().map(i -> {
            Booking b = i.getBooking();
            return new SettlementResponses.Item(i.getId(), b.getId(), b.getCode(), b.getTour().getTitle(),
                    b.getDeparture().getStartDate(), b.getContactName(), i.getKind(), i.getEventAt(), i.getPaidAmount(),
                    i.getRefundedAmount(), i.getRetainedAmount(), i.getCommissionRate(), i.getCommissionAmount(), i.getPayoutAmount());
        }).toList();
        List<SettlementResponses.Adjustment> adjustments = s.getAdjustments().stream()
                .map(a -> new SettlementResponses.Adjustment(a.getId(), a.getAmount(), a.getReason(), a.getCreatedBy().getFullName(),
                        a.getCreatedAt())).toList();
        List<SettlementResponses.Dispute> disputes = s.getDisputes().stream()
                .map(d -> new SettlementResponses.Dispute(d.getId(), d.getReason(), d.getStatus(), d.getResolution(),
                        d.getResolvedBy() == null ? null : d.getResolvedBy().getFullName(), d.getResolvedAt(), d.getCreatedAt()))
                .toList();
        SettlementResponses.BankAccount bank;
        if (s.getStatus() == SettlementStatus.PAID) {
            bank = new SettlementResponses.BankAccount(s.getBankBin(), s.getBankName(), s.getBankAccountNumber(), s.getBankAccountHolder());
        } else {
            bank = agentProfileRepository.findByUserId(s.getAgent().getId())
                    .filter(p -> p.getBank() != null && p.getBankAccountNumber() != null)
                    .map(p -> new SettlementResponses.BankAccount(p.getBank().getBin(), p.getBank().getShortName(),
                            p.getBankAccountNumber(), p.getBankAccountHolder()))
                    .orElse(null);
        }
        boolean pendingConfirm = s.getStatus() == SettlementStatus.PENDING_CONFIRM;
        return new SettlementResponses.Detail(summary(s, names), items, adjustments, disputes, bank, s.getConfirmedAt(),
                s.isAutoConfirmed(), s.getPaidBy() == null ? null : s.getPaidBy().getFullName(), s.getTransactionRef(),
                s.getReceiptPublicId() != null,
                !admin && pendingConfirm, !admin && pendingConfirm,
                admin && s.getStatus() == SettlementStatus.DISPUTED,
                admin && s.getStatus() == SettlementStatus.AWAITING_PAYMENT && bank != null);
    }
}
