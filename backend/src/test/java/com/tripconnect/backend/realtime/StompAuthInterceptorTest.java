package com.tripconnect.backend.realtime;

import com.tripconnect.backend.security.JwtUtil;
import com.tripconnect.backend.security.UserStatusCache;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StompAuthInterceptorTest {

    @Mock private JwtUtil jwtUtil;
    @Mock private UserStatusCache userStatusCache;
    @InjectMocks private StompAuthInterceptor interceptor;

    private final MessageChannel channel = mock(MessageChannel.class);

    private static Message<byte[]> frame(StompCommand command, String token, String destination, boolean authenticated) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        if (token != null) accessor.addNativeHeader("Authorization", "Bearer " + token);
        if (destination != null) accessor.setDestination(destination);
        if (authenticated) accessor.setUser(new StompPrincipal("15"));
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    @Test
    void connectWithValidTokenAttachesUserId() {
        Claims claims = mock(Claims.class);
        when(claims.get("userId", Long.class)).thenReturn(15L);
        when(jwtUtil.parseValidToken("good")).thenReturn(Optional.of(claims));
        when(userStatusCache.isActive(15L)).thenReturn(true);

        Message<?> result = interceptor.preSend(frame(StompCommand.CONNECT, "good", null, false), channel);

        assertThat(StompHeaderAccessor.wrap(result).getUser()).isEqualTo(new StompPrincipal("15"));
    }

    @Test
    void connectRejectsMissingInvalidOrDeactivated() {
        assertThatThrownBy(() -> interceptor.preSend(frame(StompCommand.CONNECT, null, null, false), channel))
                .isInstanceOf(MessageDeliveryException.class);

        when(jwtUtil.parseValidToken("bad")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> interceptor.preSend(frame(StompCommand.CONNECT, "bad", null, false), channel))
                .isInstanceOf(MessageDeliveryException.class);

        Claims claims = mock(Claims.class);
        when(claims.get("userId", Long.class)).thenReturn(15L);
        when(jwtUtil.parseValidToken("locked")).thenReturn(Optional.of(claims));
        when(userStatusCache.isActive(15L)).thenReturn(false);
        assertThatThrownBy(() -> interceptor.preSend(frame(StompCommand.CONNECT, "locked", null, false), channel))
                .isInstanceOf(MessageDeliveryException.class);
    }

    @Test
    void onlyOwnQueueCanBeSubscribed_andClientSendIsRefused() {
        interceptor.preSend(frame(StompCommand.SUBSCRIBE, null, "/user/queue/events", true), channel);

        assertThatThrownBy(() -> interceptor.preSend(frame(StompCommand.SUBSCRIBE, null, "/queue/events", true), channel))
                .isInstanceOf(MessageDeliveryException.class);
        assertThatThrownBy(() -> interceptor.preSend(frame(StompCommand.SUBSCRIBE, null, "/user/16/queue/events", true), channel))
                .isInstanceOf(MessageDeliveryException.class);
        assertThatThrownBy(() -> interceptor.preSend(frame(StompCommand.SUBSCRIBE, null, "/user/queue/events", false), channel))
                .isInstanceOf(MessageDeliveryException.class);
        assertThatThrownBy(() -> interceptor.preSend(frame(StompCommand.SEND, null, "/app/anything", true), channel))
                .isInstanceOf(MessageDeliveryException.class);
    }
}
