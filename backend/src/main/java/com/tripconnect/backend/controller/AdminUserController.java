package com.tripconnect.backend.controller;

import com.tripconnect.backend.dto.AdminUserResponse;
import com.tripconnect.backend.dto.DeactivateUserRequest;
import com.tripconnect.backend.dto.PageResponse;
import com.tripconnect.backend.enums.UserRole;
import com.tripconnect.backend.security.AuthenticatedUser;
import com.tripconnect.backend.service.AdminUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** Admin quản lý người dùng. */
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    /** Ví dụ: ?q=nguyen&role=AGENT&active=true&page=0&size=20 (mọi tham số đều không bắt buộc). */
    @GetMapping
    public PageResponse<AdminUserResponse> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) UserRole role,
            @RequestParam(required = false) Boolean active,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return adminUserService.search(q, role, active, pageable);
    }

    @PatchMapping("/{id}/deactivate")
    public AdminUserResponse deactivate(@AuthenticationPrincipal AuthenticatedUser currentUser,
                                        @PathVariable Long id,
                                        @Valid @RequestBody DeactivateUserRequest request) {
        return adminUserService.deactivate(id, currentUser.userId(), request.getReason());
    }

    @PatchMapping("/{id}/activate")
    public AdminUserResponse activate(@PathVariable Long id) {
        return adminUserService.activate(id);
    }
}
