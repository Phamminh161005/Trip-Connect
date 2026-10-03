package com.tripconnect.backend.exception;

/** Lỗi phía dịch vụ lưu trữ file (Cloudinary không phản hồi, sai cấu hình...), không phải lỗi của người dùng. */
public class FileStorageException extends RuntimeException {
    public FileStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
