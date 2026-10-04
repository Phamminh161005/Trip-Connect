package com.tripconnect.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * Đồng hồ theo giờ Việt Nam cho các quy tắc tính theo NGÀY (vd "khởi hành sau hôm nay ít nhất 3 ngày"),
 * để kết quả không phụ thuộc múi giờ của máy chủ. Unit test thay bằng đồng hồ cố định.
 */
@Configuration
public class TimeConfig {

    public static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Bean
    public Clock clock() {
        return Clock.system(VIETNAM_ZONE);
    }
}
