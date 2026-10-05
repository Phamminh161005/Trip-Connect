package com.tripconnect.backend.service.customrequest;

import com.tripconnect.backend.dto.customrequest.CustomRequestRequests;
import com.tripconnect.backend.entity.CustomRequest;
import com.tripconnect.backend.entity.Location;
import com.tripconnect.backend.entity.User;
import com.tripconnect.backend.enums.CustomRequestStatus;
import com.tripconnect.backend.exception.ForbiddenException;
import com.tripconnect.backend.repository.*;
import com.tripconnect.backend.service.NotificationEvents;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.*;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomRequestServiceTest {

    private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 4);
    private static final long CUSTOMER_ID = 5L;

    @Mock private CustomRequestRepository requestRepository;
    @Mock private LocationRepository locationRepository;
    @Mock private TourCategoryRepository categoryRepository;
    @Mock private UserRepository userRepository;
    @Mock private CustomRequestAssembler assembler;
    @Mock private CustomRequestLifecycle lifecycle;
    @Mock private ApplicationEventPublisher eventPublisher;

    private CustomRequestService service;

    @BeforeEach
    void setUp() {
        service = new CustomRequestService(requestRepository, locationRepository, categoryRepository, userRepository, assembler,
                lifecycle, eventPublisher, Clock.fixed(TODAY.atTime(9, 0).atZone(VN).toInstant(), VN));
        Location hanoi = new Location();
        hanoi.setId(1L);
        hanoi.setCountry("Việt Nam");
        hanoi.setProvince("Thành phố Hà Nội");
        lenient().when(locationRepository.findById(1L)).thenReturn(Optional.of(hanoi));
        lenient().when(locationRepository.findAllById(any())).thenReturn(List.of(hanoi));
        lenient().when(userRepository.getReferenceById(CUSTOMER_ID)).thenReturn(new User());
    }

    private static CustomRequestRequests.Create body() {
        CustomRequestRequests.Create b = new CustomRequestRequests.Create();
        b.setDepartureLocationId(1L);
        b.setDestinationIds(List.of(1L));
        b.setEarliestStart(TODAY.plusDays(20));
        b.setLatestStart(TODAY.plusDays(40));
        b.setDurationDays(4);
        b.setAdults(2);
        b.setChildren(1);
        b.setInfants(0);
        b.setBudgetMin(5_000_000L);
        b.setBudgetMax(8_000_000L);
        return b;
    }

    @Test
    void create_savesNewRequest_andTellsAdmins() {
        service.create(CUSTOMER_ID, "CUSTOMER", body());

        ArgumentCaptor<CustomRequest> saved = ArgumentCaptor.forClass(CustomRequest.class);
        verify(requestRepository).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(CustomRequestStatus.NEW);
        assertThat(saved.getValue().getCode()).startsWith("YC261004");
        assertThat(saved.getValue().travellers()).isEqualTo(3);
        verify(eventPublisher).publishEvent(any(NotificationEvents.AdminWebEvent.class));
    }

    @Test
    void create_rejectsTooSoonWrongWindowBadGroupAndBudget() {
        var tooSoon = body();
        tooSoon.setEarliestStart(TODAY.plusDays(13));
        assertThatThrownBy(() -> service.create(CUSTOMER_ID, "CUSTOMER", tooSoon)).hasMessageContaining("ít nhất 14 ngày");

        var reversed = body();
        reversed.setLatestStart(reversed.getEarliestStart().minusDays(1));
        assertThatThrownBy(() -> service.create(CUSTOMER_ID, "CUSTOMER", reversed)).hasMessageContaining("sau ngày sớm nhất");

        var wide = body();
        wide.setLatestStart(wide.getEarliestStart().plusDays(61));
        assertThatThrownBy(() -> service.create(CUSTOMER_ID, "CUSTOMER", wide)).hasMessageContaining("tối đa 60 ngày");

        var infants = body();
        infants.setInfants(3);
        assertThatThrownBy(() -> service.create(CUSTOMER_ID, "CUSTOMER", infants)).hasMessageContaining("trẻ sơ sinh");

        var budget = body();
        budget.setBudgetMin(9_000_000L);
        assertThatThrownBy(() -> service.create(CUSTOMER_ID, "CUSTOMER", budget)).hasMessageContaining("Ngân sách");
        verify(requestRepository, never()).save(any());
    }

    @Test
    void create_limitsOpenRequestsPerCustomer_andOnlyCustomersCanCreate() {
        when(requestRepository.countByCustomerIdAndStatusIn(eq(CUSTOMER_ID), any())).thenReturn(3L);
        assertThatThrownBy(() -> service.create(CUSTOMER_ID, "CUSTOMER", body())).hasMessageContaining("3 yêu cầu chưa kết thúc");

        assertThatThrownBy(() -> service.create(1L, "ADMIN", body())).isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> service.create(9L, "AGENT", body()))
                .isInstanceOf(ForbiddenException.class).hasMessageContaining("Chỉ tài khoản khách hàng");
    }

    @Test
    void cancel_onlyOwnOpenRequest() {
        CustomRequest request = new CustomRequest();
        User customer = new User();
        customer.setId(CUSTOMER_ID);
        request.setCustomer(customer);
        request.setStatus(CustomRequestStatus.WAITING_AGENT);
        when(requestRepository.findByIdForUpdate(9L)).thenReturn(Optional.of(request));

        service.cancel(CUSTOMER_ID, 9L, " Đổi kế hoạch ");
        verify(lifecycle).finish(request, CustomRequestStatus.CANCELLED, "Khách hủy yêu cầu: Đổi kế hoạch");

        assertThatThrownBy(() -> service.cancel(77L, 9L, null)).hasMessageContaining("Không tìm thấy");
        request.setStatus(CustomRequestStatus.CLOSED);
        assertThatThrownBy(() -> service.cancel(CUSTOMER_ID, 9L, null)).hasMessageContaining("đã kết thúc");
    }

    private static <T> T eq(T value) {
        return org.mockito.ArgumentMatchers.eq(value);
    }
}
