package com.tripconnect.backend.service;

import com.tripconnect.backend.enums.OtpPurpose;

public record OtpEmailEvent(String email, String otp, OtpPurpose purpose) {
}
