package com.tripconnect.backend.service.tour;

import com.tripconnect.backend.enums.UserRole;
import com.tripconnect.backend.security.AuthenticatedUser;

/**
 * Người đang thao tác trên tour.
 * - Agent: chỉ quản lý tour của chính mình, sửa tour đang bán thì phải gửi duyệt lại.
 * - Admin: quản lý tour của TripConnect (không có Agent), không cần duyệt.
 */
public record TourActor(Long userId, boolean admin) {

    public static TourActor of(AuthenticatedUser user) {
        return new TourActor(user.userId(), UserRole.ADMIN.name().equals(user.role()));
    }
}
