package com.tripconnect.backend.realtime;

import java.security.Principal;

/** Danh tính của một kết nối WebSocket; name = userId để gửi riêng bằng convertAndSendToUser. */
public record StompPrincipal(String name) implements Principal {

    @Override
    public String getName() {
        return name;
    }
}
