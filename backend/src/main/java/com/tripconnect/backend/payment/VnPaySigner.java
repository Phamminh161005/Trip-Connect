package com.tripconnect.backend.payment;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Ký / kiểm chữ ký theo chuẩn VNPay 2.1.0: HMAC-SHA512 trên chuỗi "key=value&..." (tham số sắp theo tên,
 * giá trị URL-encode US-ASCII — giống code mẫu của VNPay).
 */
public final class VnPaySigner {

    private VnPaySigner() {
    }

    public static final String SECURE_HASH = "vnp_SecureHash";
    public static final String SECURE_HASH_TYPE = "vnp_SecureHashType";

    public static String hmacSha512(String key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Không tạo được chữ ký VNPay", e);
        }
    }

    /** "a=1&b=2" — tham số rỗng bị bỏ qua, sắp theo tên, giá trị đã URL-encode. */
    public static String canonicalQuery(Map<String, String> params) {
        return new TreeMap<>(params).entrySet().stream()
                .filter(e -> e.getValue() != null && !e.getValue().isEmpty())
                .map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.US_ASCII))
                .collect(Collectors.joining("&"));
    }

    public static String sign(String secret, Map<String, String> params) {
        return hmacSha512(secret, canonicalQuery(params));
    }

    /** Kiểm chữ ký các tham số VNPay gửi về (Return URL / IPN). */
    public static boolean verify(String secret, Map<String, String> params) {
        String received = params.get(SECURE_HASH);
        if (received == null || received.isBlank()) return false;
        Map<String, String> data = params.entrySet().stream()
                .filter(e -> e.getKey().startsWith("vnp_"))
                .filter(e -> !e.getKey().equals(SECURE_HASH) && !e.getKey().equals(SECURE_HASH_TYPE))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        String expected = sign(secret, data);
        // So sánh thời gian cố định — không để lộ thông tin qua thời gian phản hồi
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.US_ASCII),
                received.toLowerCase().getBytes(StandardCharsets.US_ASCII));
    }
}
