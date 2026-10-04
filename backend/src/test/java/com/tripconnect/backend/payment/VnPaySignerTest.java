package com.tripconnect.backend.payment;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class VnPaySignerTest {

    private static final String SECRET = "TESTSECRET0123456789ABCDEFGHIJKL";

    private static Map<String, String> signedResult() {
        Map<String, String> params = new HashMap<>();
        params.put("vnp_Amount", "459000000");
        params.put("vnp_TxnRef", "TC26100312345601");
        params.put("vnp_ResponseCode", "00");
        params.put("vnp_TransactionStatus", "00");
        params.put("vnp_OrderInfo", "Thanh toan don dat tour TC261003123456");
        params.put("vnp_SecureHash", VnPaySigner.sign(SECRET, params));
        return params;
    }

    @Test
    void canonicalQuery_sortsKeys_encodesValues_skipsEmpty() {
        String query = VnPaySigner.canonicalQuery(Map.of("vnp_b", "x y", "vnp_a", "1", "vnp_c", ""));
        assertThat(query).isEqualTo("vnp_a=1&vnp_b=x+y");
    }

    @Test
    void validSignature_isAccepted() {
        assertThat(VnPaySigner.verify(SECRET, signedResult())).isTrue();
    }

    @Test
    void tamperedAmount_isRejected() {
        // Khách sửa số tiền trên link quay về -> chữ ký không còn khớp
        Map<String, String> params = signedResult();
        params.put("vnp_Amount", "100");
        assertThat(VnPaySigner.verify(SECRET, params)).isFalse();
    }

    @Test
    void wrongSecretOrMissingHash_isRejected() {
        assertThat(VnPaySigner.verify("OTHER-SECRET", signedResult())).isFalse();
        Map<String, String> params = signedResult();
        params.remove("vnp_SecureHash");
        assertThat(VnPaySigner.verify(SECRET, params)).isFalse();
    }

    @Test
    void hashTypeAndNonVnpParams_areIgnored() {
        Map<String, String> params = signedResult();
        params.put("vnp_SecureHashType", "HmacSHA512");
        params.put("utm_source", "facebook");
        assertThat(VnPaySigner.verify(SECRET, params)).isTrue();
    }
}
