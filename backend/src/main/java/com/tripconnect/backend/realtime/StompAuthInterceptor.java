package com.tripconnect.backend.realtime;

import com.tripconnect.backend.security.JwtUtil;
import com.tripconnect.backend.security.UserStatusCache;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

/**
 * Xác thực kết nối STOMP. Trình duyệt không gắn được header vào lúc bắt tay WebSocket, nên access token
 * đi trong khung CONNECT ("Authorization: Bearer ..."). Sau đó chỉ cho đăng ký hộp thư riêng, không nhận SEND.
 */
@Component
@RequiredArgsConstructor
public class StompAuthInterceptor implements ChannelInterceptor {

    private static final String SUBSCRIBE_DESTINATION = "/user" + WebSocketConfig.USER_QUEUE;

    private final JwtUtil jwtUtil;
    private final UserStatusCache userStatusCache;

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) return message;

        switch (accessor.getCommand()) {
            case CONNECT -> accessor.setUser(authenticate(accessor.getFirstNativeHeader("Authorization")));
            case SUBSCRIBE -> {
                if (accessor.getUser() == null || !SUBSCRIBE_DESTINATION.equals(accessor.getDestination())) {
                    throw new MessageDeliveryException("Không được đăng ký kênh này");
                }
            }
            case SEND -> throw new MessageDeliveryException("Gửi tin qua API, không qua WebSocket");
            default -> {
            }
        }
        return message;
    }

    StompPrincipal authenticate(String header) {
        if (header == null || !header.startsWith("Bearer ")) throw new MessageDeliveryException("Chưa đăng nhập");
        return jwtUtil.parseValidToken(header.substring(7))
                .map(claims -> claims.get("userId", Long.class))
                .filter(userStatusCache::isActive)
                .map(userId -> new StompPrincipal(String.valueOf(userId)))
                .orElseThrow(() -> new MessageDeliveryException("Phiên đăng nhập không hợp lệ hoặc đã hết hạn"));
    }
}
