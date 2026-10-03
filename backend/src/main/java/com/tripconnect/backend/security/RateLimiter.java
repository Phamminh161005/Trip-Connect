package com.tripconnect.backend.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.tripconnect.backend.exception.TooManyRequestsException;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Rate limit kiểu fixed window, lưu trong RAM (chỉ đúng khi chạy 1 instance; nhiều instance thì chuyển sang Redis).
 */
@Component
public class RateLimiter {

    private final Map<String, Cache<String, AtomicInteger>> counters = new ConcurrentHashMap<>();

    /** Mỗi key chỉ được gọi tối đa maxRequests lần trong khoảng window. */
    public void check(String rule, String key, int maxRequests, Duration window) {
        if (key == null) return;
        Cache<String, AtomicInteger> cache = counters.computeIfAbsent(rule, r -> Caffeine.newBuilder()
                .expireAfterWrite(window)
                .maximumSize(100_000)
                .build());
        int count = cache.get(key, k -> new AtomicInteger()).incrementAndGet();
        if (count > maxRequests) {
            throw new TooManyRequestsException("Bạn thao tác quá nhiều lần, vui lòng thử lại sau");
        }
    }
}
