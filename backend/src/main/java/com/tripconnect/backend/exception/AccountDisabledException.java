package com.tripconnect.backend.exception;

/** Tài khoản đã bị Admin vô hiệu hóa. */
public class AccountDisabledException extends RuntimeException {
    public AccountDisabledException(String message) {
        super(message);
    }
}
