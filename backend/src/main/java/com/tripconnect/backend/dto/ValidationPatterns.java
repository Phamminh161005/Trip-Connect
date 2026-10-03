package com.tripconnect.backend.dto;

/** Các mẫu kiểm tra dữ liệu dùng chung giữa nhiều DTO (phía Frontend có luật tương ứng trong lib/validation). */
public final class ValidationPatterns {

    private ValidationPatterns() {
    }

    /**
     * Mã số thuế Việt Nam:
     *  - 10 chữ số: doanh nghiệp (vd 0101234567)
     *  - 10 số + "-" + 3 số: chi nhánh / đơn vị phụ thuộc (vd 0101234567-001)
     *  - 12 chữ số: cá nhân, hộ kinh doanh dùng số định danh CCCD làm mã số thuế
     * Chỉ kiểm tra định dạng; việc mã có thật hay không do Admin đối chiếu với giấy tờ khi duyệt.
     */
    public static final String TAX_CODE = "^(\\d{10}(-\\d{3})?|\\d{12})$";

    /** Như TAX_CODE nhưng cho phép để trống (dùng trong form "để trống = không đổi"). */
    public static final String TAX_CODE_OR_EMPTY = "^(|\\d{10}(-\\d{3})?|\\d{12})$";

    public static final String TAX_CODE_MESSAGE =
            "Mã số thuế gồm 10 số, 10 số-3 số (chi nhánh) hoặc 12 số (hộ kinh doanh)";

    /** Mã BIN ngân hàng (Napas): 6 chữ số. */
    public static final String BANK_BIN = "^\\d{6}$";
    public static final String BANK_BIN_OR_EMPTY = "^(|\\d{6})$";
    public static final String BANK_BIN_MESSAGE = "Vui lòng chọn ngân hàng trong danh sách";

    /** Số tài khoản ngân hàng Việt Nam: chỉ gồm chữ số, 6-19 số. */
    public static final String BANK_ACCOUNT_NUMBER = "^\\d{6,19}$";
    public static final String BANK_ACCOUNT_NUMBER_OR_EMPTY = "^(|\\d{6,19})$";
    public static final String BANK_ACCOUNT_NUMBER_MESSAGE = "Số tài khoản chỉ gồm chữ số (6-19 số)";

    /** Địa chỉ chi tiết tối thiểu 10 ký tự để chặn kiểu nhập bừa ("abc"). */
    public static final int ADDRESS_DETAIL_MIN_LENGTH = 10;
}
