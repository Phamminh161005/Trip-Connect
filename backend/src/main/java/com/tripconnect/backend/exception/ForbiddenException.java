package com.tripconnect.backend.exception;

/** Đã đăng nhập nhưng không đủ điều kiện thực hiện thao tác (ví dụ: Agent chưa được duyệt). */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
