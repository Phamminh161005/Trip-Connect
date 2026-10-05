package com.tripconnect.backend.controller;

import com.tripconnect.backend.dto.AdminSummaryResponse;
import com.tripconnect.backend.enums.AgentStatus;
import com.tripconnect.backend.enums.ChangeRequestStatus;
import com.tripconnect.backend.repository.AgentProfileChangeRequestRepository;
import com.tripconnect.backend.repository.AgentProfileRepository;
import com.tripconnect.backend.enums.CustomRequestStatus;
import com.tripconnect.backend.enums.RefundStatus;
import com.tripconnect.backend.enums.TourStatus;
import com.tripconnect.backend.repository.BookingRepository;
import com.tripconnect.backend.repository.CustomRequestRepository;
import com.tripconnect.backend.repository.TourRepository;
import com.tripconnect.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Số liệu tổng quan cho trang quản trị. */
@RestController
@RequestMapping("/api/admin/summary")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AgentProfileRepository agentProfileRepository;
    private final AgentProfileChangeRequestRepository changeRequestRepository;
    private final UserRepository userRepository;
    private final TourRepository tourRepository;
    private final BookingRepository bookingRepository;
    private final CustomRequestRepository customRequestRepository;

    @GetMapping
    @Transactional(readOnly = true)
    public AdminSummaryResponse getSummary() {
        return new AdminSummaryResponse(
                agentProfileRepository.countByStatus(AgentStatus.PENDING_APPROVAL),
                changeRequestRepository.countByStatus(ChangeRequestStatus.PENDING),
                userRepository.count(),
                agentProfileRepository.countByStatus(AgentStatus.APPROVED),
                tourRepository.countByStatus(TourStatus.PENDING_APPROVAL),
                bookingRepository.countByRefundStatus(RefundStatus.MANUAL_REQUIRED),
                customRequestRepository.countByStatus(CustomRequestStatus.NEW)
        );
    }
}
