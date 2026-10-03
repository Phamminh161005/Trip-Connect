package com.tripconnect.backend.service.agent;

import com.tripconnect.backend.dto.*;
import com.tripconnect.backend.entity.AgentDocument;
import com.tripconnect.backend.entity.Bank;
import com.tripconnect.backend.entity.Location;
import com.tripconnect.backend.entity.AgentProfile;
import com.tripconnect.backend.entity.AgentProfileChangeRequest;
import com.tripconnect.backend.entity.User;
import com.tripconnect.backend.enums.AgentDocumentStatus;
import com.tripconnect.backend.enums.AgentDocumentType;
import com.tripconnect.backend.enums.AgentStatus;
import com.tripconnect.backend.enums.ChangeRequestStatus;
import com.tripconnect.backend.exception.ResourceNotFoundException;
import com.tripconnect.backend.repository.StablePaging;
import com.tripconnect.backend.repository.AgentDocumentRepository;
import com.tripconnect.backend.repository.AgentProfileChangeRequestRepository;
import com.tripconnect.backend.repository.AgentProfileRepository;
import com.tripconnect.backend.repository.UserRepository;
import com.tripconnect.backend.service.EmailTemplates;
import com.tripconnect.backend.service.NotificationEvents;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Yêu cầu cập nhật thông tin pháp lý của Agent ĐÃ được duyệt.
 * Agent vẫn hoạt động bằng thông tin cũ cho tới khi Admin duyệt yêu cầu.
 */
@Service
@RequiredArgsConstructor
public class AgentChangeRequestService {

    private final AgentProfileRepository profileRepository;
    private final AgentProfileChangeRequestRepository changeRequestRepository;
    private final AgentDocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final AgentDocumentStorage documentStorage;
    private final AgentProfileAssembler assembler;
    private final AgentReferenceResolver referenceResolver;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;

    // ===================== Phía Agent =====================

    @Transactional
    public AgentChangeRequestResponse create(Long userId, AgentChangeRequestForm form) {
        AgentProfile profile = requireProfile(userId);
        if (profile.getStatus() != AgentStatus.APPROVED) {
            throw new IllegalStateException(
                    "Chỉ gửi yêu cầu cập nhật khi hồ sơ đã được duyệt. Hồ sơ chưa duyệt thì sửa trực tiếp");
        }
        if (changeRequestRepository.existsByAgentProfileIdAndStatus(profile.getId(), ChangeRequestStatus.PENDING)) {
            throw new IllegalStateException(
                    "Bạn đang có một yêu cầu cập nhật chờ duyệt. Vui lòng chờ kết quả hoặc hủy yêu cầu đó");
        }

        // Chỉ giữ lại những trường thực sự thay đổi so với hiện tại
        String companyName = changedValue(form.getCompanyName(), profile.getCompanyName());
        String taxCode = changedValue(form.getTaxCode(), profile.getTaxCode());
        String businessLicense = changedValue(form.getBusinessLicense(), profile.getBusinessLicense());
        String address = changedValue(form.getAddress(), profile.getAddress());
        if (address != null && address.length() < ValidationPatterns.ADDRESS_DETAIL_MIN_LENGTH) {
            throw new IllegalArgumentException("Địa chỉ chi tiết (số nhà, đường, phường/xã) tối thiểu 10 ký tự");
        }
        Location addressProvince = changedProvince(form.getAddressProvinceId(), profile.getAddressProvince());
        BankChange bankChange = changedBankAccount(form, profile);

        Map<AgentDocumentType, MultipartFile> singleFiles = collectSingleFiles(form);
        List<MultipartFile> otherFiles = form.getOtherDocuments() == null ? List.of()
                : form.getOtherDocuments().stream().filter(f -> f != null && !f.isEmpty()).toList();

        boolean noFieldChanged = companyName == null && taxCode == null && businessLicense == null
                && address == null && addressProvince == null && bankChange == null;
        if (noFieldChanged && singleFiles.isEmpty() && otherFiles.isEmpty()) {
            throw new IllegalArgumentException("Yêu cầu không có thay đổi nào so với hồ sơ hiện tại");
        }

        if (bankChange != null) {
            verifyPasswordForBankChange(profile.getUser(), form.getCurrentPassword());
        }

        if (!otherFiles.isEmpty()) {
            long existingOthers = documentRepository.countByAgentProfileIdAndTypeAndStatusIn(
                    profile.getId(), AgentDocumentType.OTHER, List.of(AgentDocumentStatus.ACTIVE));
            if (existingOthers + otherFiles.size() > AgentDocumentType.MAX_OTHER_DOCUMENTS) {
                throw new IllegalArgumentException("Tổng số giấy tờ khác không được vượt quá "
                        + AgentDocumentType.MAX_OTHER_DOCUMENTS);
            }
        }

        // Kiểm tra TẤT CẢ file trước, để không upload dở dang rồi mới phát hiện file sau bị lỗi
        Map<MultipartFile, String> mimeTypes = new IdentityHashMap<>();
        singleFiles.values().forEach(f -> mimeTypes.put(f, documentStorage.validate(f)));
        otherFiles.forEach(f -> mimeTypes.put(f, documentStorage.validate(f)));

        AgentProfileChangeRequest request = new AgentProfileChangeRequest();
        request.setAgentProfile(profile);
        request.setCompanyName(companyName);
        request.setTaxCode(taxCode);
        request.setBusinessLicense(businessLicense);
        request.setAddressProvince(addressProvince);
        request.setAddress(address);
        request.setNote(trimToNull(form.getNote()));
        if (bankChange != null) {
            request.setBank(bankChange.bank());
            request.setBankAccountNumber(bankChange.accountNumber());
            request.setBankAccountHolder(bankChange.accountHolder());
        }
        request = changeRequestRepository.save(request);

        for (Map.Entry<AgentDocumentType, MultipartFile> entry : singleFiles.entrySet()) {
            documentStorage.store(profile, request, entry.getKey(), AgentDocumentStatus.PENDING,
                    entry.getValue(), mimeTypes.get(entry.getValue()));
        }
        for (MultipartFile file : otherFiles) {
            documentStorage.store(profile, request, AgentDocumentType.OTHER, AgentDocumentStatus.PENDING,
                    file, mimeTypes.get(file));
        }

        eventPublisher.publishEvent(new NotificationEvents.AdminEmailEvent(
                EmailTemplates.agentChangeRequestSubmitted(profile.getCompanyName(), profile.getUser().getEmail())));
        return assembler.toChangeRequestResponse(request);
    }

    @Transactional(readOnly = true)
    public List<AgentChangeRequestResponse> listMine(Long userId) {
        AgentProfile profile = requireProfile(userId);
        return changeRequestRepository.findByAgentProfileIdOrderByCreatedAtDesc(profile.getId()).stream()
                .map(assembler::toChangeRequestResponse)
                .toList();
    }

    @Transactional
    public void cancel(Long userId, Long requestId) {
        AgentProfile profile = requireProfile(userId);
        AgentProfileChangeRequest request = changeRequestRepository.findByIdAndAgentProfileId(requestId, profile.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu cập nhật"));
        requirePending(request);

        request.setStatus(ChangeRequestStatus.CANCELLED);
        discardPendingDocuments(request);
    }

    // ===================== Phía Admin =====================

    @Transactional(readOnly = true)
    public PageResponse<AdminChangeRequestSummaryResponse> list(ChangeRequestStatus status, Pageable pageable) {
        ChangeRequestStatus filter = status == null ? ChangeRequestStatus.PENDING : status;
        return PageResponse.from(changeRequestRepository.findByStatus(filter, StablePaging.of(pageable)),
                AgentProfileAssembler::toChangeRequestSummary);
    }

    @Transactional(readOnly = true)
    public AdminChangeRequestDetailResponse getDetail(Long requestId) {
        AgentProfileChangeRequest request = requireRequest(requestId);
        return new AdminChangeRequestDetailResponse(
                assembler.toChangeRequestResponse(request),
                assembler.toResponse(request.getAgentProfile()));
    }

    @Transactional
    public void approve(Long requestId, Long adminId) {
        AgentProfileChangeRequest request = requireRequest(requestId);
        requirePending(request);
        AgentProfile profile = request.getAgentProfile();

        if (request.getCompanyName() != null) profile.setCompanyName(request.getCompanyName());
        if (request.getTaxCode() != null) profile.setTaxCode(request.getTaxCode());
        if (request.getBusinessLicense() != null) profile.setBusinessLicense(request.getBusinessLicense());
        if (request.getAddressProvince() != null) profile.setAddressProvince(request.getAddressProvince());
        if (request.getAddress() != null) profile.setAddress(request.getAddress());
        if (request.changesBankAccount()) {
            profile.setBank(request.getBank());
            profile.setBankAccountNumber(request.getBankAccountNumber());
            profile.setBankAccountHolder(request.getBankAccountHolder());
        }

        for (AgentDocument newDocument : documentRepository.findByChangeRequestIdOrderByUploadedAtAsc(request.getId())) {
            if (newDocument.getType().singleFile()) {
                // Giấy tờ cũ cùng loại không xóa mà chuyển sang lưu trữ, để còn tra cứu về sau
                documentRepository.findByAgentProfileIdAndTypeAndStatus(
                                profile.getId(), newDocument.getType(), AgentDocumentStatus.ACTIVE)
                        .forEach(old -> old.setStatus(AgentDocumentStatus.ARCHIVED));
            }
            newDocument.setStatus(AgentDocumentStatus.ACTIVE);
        }

        markReviewed(request, ChangeRequestStatus.APPROVED, adminId, null);
        eventPublisher.publishEvent(new NotificationEvents.UserEmailEvent(
                profile.getUser().getEmail(), EmailTemplates.agentChangeRequestApproved()));
    }

    @Transactional
    public void reject(Long requestId, Long adminId, String reason) {
        AgentProfileChangeRequest request = requireRequest(requestId);
        requirePending(request);

        markReviewed(request, ChangeRequestStatus.REJECTED, adminId, reason.trim());
        discardPendingDocuments(request);
        eventPublisher.publishEvent(new NotificationEvents.UserEmailEvent(
                request.getAgentProfile().getUser().getEmail(), EmailTemplates.agentChangeRequestRejected(reason.trim())));
    }

    // ===================== Tiện ích =====================

    private void verifyPasswordForBankChange(User user, String currentPassword) {
        if (user.getPasswordHash() == null) {
            throw new IllegalStateException("Tài khoản đăng nhập bằng Google chưa có mật khẩu. "
                    + "Hãy tạo mật khẩu bằng chức năng \"Quên mật khẩu\" trước khi đổi tài khoản ngân hàng");
        }
        if (currentPassword == null || currentPassword.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập mật khẩu hiện tại để xác nhận đổi tài khoản ngân hàng");
        }
        // Trả 400 (không phải 401) để Frontend không hiểu nhầm là phiên đăng nhập hết hạn
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Mật khẩu hiện tại không đúng");
        }
    }

    private void discardPendingDocuments(AgentProfileChangeRequest request) {
        documentStorage.deletePermanently(documentRepository.findByChangeRequestIdOrderByUploadedAtAsc(request.getId()));
    }

    private void markReviewed(AgentProfileChangeRequest request, ChangeRequestStatus status, Long adminId,
                              String reason) {
        request.setStatus(status);
        request.setRejectionReason(reason);
        request.setReviewedBy(userRepository.getReferenceById(adminId));
        request.setReviewedAt(LocalDateTime.now());
    }

    private static Map<AgentDocumentType, MultipartFile> collectSingleFiles(AgentChangeRequestForm form) {
        Map<AgentDocumentType, MultipartFile> files = new EnumMap<>(AgentDocumentType.class);
        putIfPresent(files, AgentDocumentType.TRAVEL_LICENSE, form.getTravelLicense());
        putIfPresent(files, AgentDocumentType.BUSINESS_REGISTRATION, form.getBusinessRegistration());
        putIfPresent(files, AgentDocumentType.REPRESENTATIVE_ID_FRONT, form.getRepresentativeIdFront());
        putIfPresent(files, AgentDocumentType.REPRESENTATIVE_ID_BACK, form.getRepresentativeIdBack());
        return files;
    }

    private static void putIfPresent(Map<AgentDocumentType, MultipartFile> files, AgentDocumentType type,
                                     MultipartFile file) {
        if (file != null && !file.isEmpty()) {
            files.put(type, file);
        }
    }

    /** Tài khoản ngân hàng mới (3 trường luôn đi cùng nhau). */
    private record BankChange(Bank bank, String accountNumber, String accountHolder) {
    }

    /** null nếu không đổi Tỉnh/Thành trụ sở. */
    private Location changedProvince(Long provinceId, Location current) {
        if (provinceId == null || (current != null && provinceId.equals(current.getId()))) return null;
        return referenceResolver.requireVietnamProvince(provinceId);
    }

    /** null nếu không đổi tài khoản ngân hàng. Đổi thì bắt buộc nhập đủ cả 3 trường. */
    private BankChange changedBankAccount(AgentChangeRequestForm form, AgentProfile profile) {
        String bin = trimToNull(form.getBankBin());
        String number = trimToNull(form.getBankAccountNumber());
        String holder = BankAccountHolder.normalize(trimToNull(form.getBankAccountHolder()));

        if (bin == null && number == null && holder == null) return null;
        if (bin == null || number == null || holder == null) {
            throw new IllegalArgumentException(
                    "Để đổi tài khoản ngân hàng, vui lòng nhập đủ ngân hàng, số tài khoản và tên chủ tài khoản");
        }
        boolean unchanged = profile.getBank() != null && bin.equals(profile.getBank().getBin())
                && number.equals(profile.getBankAccountNumber()) && holder.equals(profile.getBankAccountHolder());
        return unchanged ? null : new BankChange(referenceResolver.requireBank(bin), number, holder);
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /** null nếu để trống hoặc giống giá trị hiện tại (tức là không đổi). */
    static String changedValue(String newValue, String currentValue) {
        if (newValue == null || newValue.isBlank()) return null;
        String trimmed = newValue.trim();
        return trimmed.equals(currentValue) ? null : trimmed;
    }

    private static void requirePending(AgentProfileChangeRequest request) {
        if (request.getStatus() != ChangeRequestStatus.PENDING) {
            throw new IllegalStateException("Yêu cầu này đã được xử lý trước đó");
        }
    }

    private AgentProfile requireProfile(Long userId) {
        return profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ Agent"));
    }

    private AgentProfileChangeRequest requireRequest(Long requestId) {
        return changeRequestRepository.findWithProfileById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu cập nhật id=" + requestId));
    }
}
