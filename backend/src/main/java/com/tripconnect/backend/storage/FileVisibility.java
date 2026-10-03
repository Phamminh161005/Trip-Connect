package com.tripconnect.backend.storage;

public enum FileVisibility {
    /** Ai có link cũng xem được — dùng cho ảnh tour. */
    PUBLIC,
    /** Link gốc không mở được; muốn xem phải xin link tạm thời qua backend — dùng cho giấy tờ pháp lý. */
    PRIVATE
}
