package com.tripconnect.backend.enums;

/** Giao dịch trả cho phần nào của đơn. */
public enum PaymentPurpose {
    /** Trả toàn bộ một lần. */
    FULL,
    /** Tiền cọc tour riêng. */
    DEPOSIT,
    /** Phần còn lại sau khi đã cọc. */
    BALANCE
}
