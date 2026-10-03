package com.tripconnect.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import java.util.TimeZone;

@EnableAsync
@EnableScheduling
// Dùng JWT, tắt user mặc định của Spring Security
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class BackendApplication {

    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        SpringApplication.run(BackendApplication.class, args);
    }

}
