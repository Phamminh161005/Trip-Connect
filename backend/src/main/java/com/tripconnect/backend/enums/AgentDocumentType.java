package com.tripconnect.backend.enums;

public enum AgentDocumentType {
    TRAVEL_LICENSE("Giấy phép kinh doanh lữ hành", true),
    BUSINESS_REGISTRATION("Giấy chứng nhận đăng ký kinh doanh", true),
    REPRESENTATIVE_ID_FRONT("CCCD người đại diện (mặt trước)", true),
    REPRESENTATIVE_ID_BACK("CCCD người đại diện (mặt sau)", true),
    OTHER("Giấy tờ khác", false);

    /** Số giấy tờ "khác" tối đa cho một hồ sơ. */
    public static final int MAX_OTHER_DOCUMENTS = 5;

    private final String label;
    private final boolean required;

    AgentDocumentType(String label, boolean required) {
        this.label = label;
        this.required = required;
    }

    public String label() {
        return label;
    }

    public boolean required() {
        return required;
    }

    /** Loại chỉ có đúng 1 file (upload file mới sẽ thay file cũ). OTHER thì được nhiều file. */
    public boolean singleFile() {
        return this != OTHER;
    }
}
