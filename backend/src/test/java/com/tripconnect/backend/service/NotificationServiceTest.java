package com.tripconnect.backend.service;

import com.tripconnect.backend.entity.Notification;
import com.tripconnect.backend.enums.NotificationType;
import com.tripconnect.backend.enums.UserRole;
import com.tripconnect.backend.realtime.RealtimePublisher;
import com.tripconnect.backend.repository.NotificationRepository;
import com.tripconnect.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.time.*;
import java.util.List;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 4, 9, 0);

    @Mock private NotificationRepository notificationRepository;
    @Mock private UserRepository userRepository;
    @Mock private RealtimePublisher realtimePublisher;

    private NotificationService service;

    @BeforeEach
    void setUp() {
        service = new NotificationService(notificationRepository, userRepository, realtimePublisher, Clock.fixed(NOW.atZone(VN).toInstant(), VN));
    }

    private static Notification notification(long id) {
        Notification n = new Notification();
        n.setId(id);
        n.setUserId(5L);
        n.setType(NotificationType.BOOKING_PAID);
        n.setTitle("Đặt tour thành công");
        n.setCreatedAt(NOW);
        return n;
    }

    @SuppressWarnings("unchecked")
    @Test
    void adminMessageIsCopiedToEveryActiveAdmin() {
        when(userRepository.findActiveIdsByRole(UserRole.ADMIN)).thenReturn(List.of(1L, 2L));

        service.recordForAdmins(new NotificationEvents.WebMessage(NotificationType.TOUR_SUBMITTED, "Tour mới cần duyệt", "x", "/admin/tours/3"));

        ArgumentCaptor<List<Notification>> saved = ArgumentCaptor.forClass(List.class);
        verify(notificationRepository).saveAll(saved.capture());
        assertThat(saved.getValue()).extracting(Notification::getUserId).containsExactly(1L, 2L);
        assertThat(saved.getValue()).allSatisfy(n -> {
            assertThat(n.getReadAt()).isNull();
            assertThat(n.getCreatedAt()).isEqualTo(NOW);
            assertThat(n.getLink()).isEqualTo("/admin/tours/3");
        });
    }

    @Test
    void list_fetchesOneExtraRowToKnowIfThereIsANextPage() {
        // size 3 -> lấy 4 dòng; có dòng thứ 4 nghĩa là còn trang sau, con trỏ = id dòng cuối của trang này
        when(notificationRepository.findPage(5L, null, false, PageRequest.of(0, 4)))
                .thenReturn(LongStream.of(40, 39, 38, 37).mapToObj(NotificationServiceTest::notification).toList());

        var page = service.list(5L, null, 3, false);

        assertThat(page.items()).extracting(i -> i.id()).containsExactly(40L, 39L, 38L);
        assertThat(page.nextCursor()).isEqualTo(38L);
    }

    @Test
    void list_lastPageHasNoCursor_andSizeIsCapped() {
        when(notificationRepository.findPage(eq(5L), eq(38L), eq(true), any()))
                .thenReturn(List.of(notification(12)));

        var page = service.list(5L, 38L, 500, true);

        assertThat(page.nextCursor()).isNull();
        verify(notificationRepository).findPage(5L, 38L, true, PageRequest.of(0, NotificationService.MAX_PAGE_SIZE + 1));
    }

    @Test
    void markReadOnlyTouchesOwnNotifications() {
        service.markRead(5L, 40L);
        service.markAllRead(5L);

        verify(notificationRepository).markRead(40L, 5L, NOW);
        verify(notificationRepository).markAllRead(5L, NOW);
    }

    @Test
    void cleanupDeletesNotificationsReadMoreThanSixtyDaysAgo() {
        service.deleteOldRead();

        verify(notificationRepository).deleteReadBefore(NOW.minusDays(60));
    }
}
