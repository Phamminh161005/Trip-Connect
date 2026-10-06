package com.tripconnect.backend.realtime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * STOMP trên WebSocket tại /ws. Client chỉ nhận (đăng ký hộp thư riêng /user/queue/events); gửi tin đi qua REST.
 * Broker chạy trong bộ nhớ: đủ cho một máy chủ. Chạy nhiều máy chủ thì phải chuyển sang broker ngoài
 * (RabbitMQ / Redis) để tin từ máy này tới được người đang kết nối vào máy khác.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /** Hộp thư riêng của mỗi người dùng (client đăng ký /user/queue/events). */
    public static final String USER_QUEUE = "/queue/events";
    /** Nhịp tim 2 chiều để phát hiện kết nối chết. */
    private static final long HEARTBEAT_MS = 10_000;

    private final StompAuthInterceptor authInterceptor;
    private final String frontendUrl;

    public WebSocketConfig(StompAuthInterceptor authInterceptor, @Value("${app.frontend-url}") String frontendUrl) {
        this.authInterceptor = authInterceptor;
        this.frontendUrl = frontendUrl.replaceAll("/+$", "");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setAllowedOrigins(frontendUrl);
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Không khai báo thành bean để không chiếm chỗ bộ lập lịch của các job @Scheduled
        ThreadPoolTaskScheduler heartbeatScheduler = new ThreadPoolTaskScheduler();
        heartbeatScheduler.setPoolSize(1);
        heartbeatScheduler.setThreadNamePrefix("ws-heartbeat-");
        heartbeatScheduler.initialize();

        registry.enableSimpleBroker("/queue")
                .setHeartbeatValue(new long[]{HEARTBEAT_MS, HEARTBEAT_MS})
                .setTaskScheduler(heartbeatScheduler);
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(authInterceptor);
    }
}
