package com.tripconnect.backend.service;

import com.tripconnect.backend.entity.User;
import com.tripconnect.backend.enums.UserRole;
import com.tripconnect.backend.repository.AgentProfileRepository;
import com.tripconnect.backend.repository.RefreshTokenRepository;
import com.tripconnect.backend.repository.UserRepository;
import com.tripconnect.backend.security.UserStatusCache;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    private static final long ADMIN_ID = 1L;

    @Mock private UserRepository userRepository;
    @Mock private AgentProfileRepository agentProfileRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private UserStatusCache userStatusCache;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks private AdminUserService service;

    @Test
    void deactivate_customer_disablesAccountAndLogsOutEverywhere() {
        User customer = user(5L, UserRole.CUSTOMER);
        when(userRepository.findById(5L)).thenReturn(Optional.of(customer));

        service.deactivate(5L, ADMIN_ID, "  Spam đánh giá  ");

        assertThat(customer.isActive()).isFalse();
        assertThat(customer.getDeactivatedReason()).isEqualTo("Spam đánh giá");
        assertThat(customer.getDeactivatedAt()).isNotNull();
        verify(refreshTokenRepository).revokeAllByUserId(5L);
        verify(userStatusCache).evict(5L); // access token còn hạn cũng bị chặn ngay
        verify(eventPublisher).publishEvent(any(NotificationEvents.UserEmailEvent.class));
    }

    @Test
    void deactivate_anotherAdmin_isNotAllowed() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L, UserRole.ADMIN)));

        assertThatThrownBy(() -> service.deactivate(2L, ADMIN_ID, "lý do"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Admin");
        verifyNoInteractions(refreshTokenRepository);
    }

    @Test
    void deactivate_self_isNotAllowed() {
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(user(ADMIN_ID, UserRole.ADMIN)));

        assertThatThrownBy(() -> service.deactivate(ADMIN_ID, ADMIN_ID, "lý do"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("chính mình");
    }

    @Test
    void activate_clearsDeactivationInfo() {
        User agent = user(9L, UserRole.AGENT);
        agent.setActive(false);
        agent.setDeactivatedReason("vi phạm");
        when(userRepository.findById(9L)).thenReturn(Optional.of(agent));

        service.activate(9L);

        assertThat(agent.isActive()).isTrue();
        assertThat(agent.getDeactivatedReason()).isNull();
        assertThat(agent.getDeactivatedAt()).isNull();
        verify(userStatusCache).evict(9L);
    }

    private static User user(long id, UserRole role) {
        User user = new User();
        user.setId(id);
        user.setRole(role);
        user.setEmail("user" + id + "@example.com");
        return user;
    }
}
