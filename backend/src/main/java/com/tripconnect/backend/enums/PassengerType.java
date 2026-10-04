package com.tripconnect.backend.enums;

/** Loại hành khách tính theo tuổi vào ngày khởi hành. */
public enum PassengerType {
    /** Từ 12 tuổi — giá người lớn. */
    ADULT,
    /** 2-11 tuổi — giá trẻ em. */
    CHILD,
    /** Dưới 2 tuổi — miễn phí, không tính chỗ (ngồi cùng người lớn). */
    INFANT
}
