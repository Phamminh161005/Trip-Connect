package com.tripconnect.backend.payment;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Giao tiếp với VNPay: tạo link thanh toán, tra cứu giao dịch (querydr), hoàn tiền (refund).
 * Thời gian gửi VNPay theo giờ Việt Nam, dạng yyyyMMddHHmmss. Số tiền gửi VNPay = VNĐ x 100.
 */
@Slf4j
@Component
public class VnPayClient {

    public static final DateTimeFormatter VNP_DATE = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final String VERSION = "2.1.0";
    private static final String VNPAY_ROOT_CA = "certs/sectigo-public-server-authentication-root-r46.pem";

    private final VnPayProperties props;
    private final RestClient restClient;

    public VnPayClient(VnPayProperties props) {
        this.props = props;
        SSLSocketFactory sslSocketFactory = trustingVnPayRoot().getSocketFactory();
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory() {
            @Override
            protected void prepareConnection(HttpURLConnection connection, String httpMethod) throws IOException {
                super.prepareConnection(connection, httpMethod);
                if (connection instanceof HttpsURLConnection https) {
                    https.setSSLSocketFactory(sslSocketFactory);
                }
            }
        };
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(20));
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    /**
     * sandbox.vnpayment.vn dùng chứng chỉ gốc Sectigo R46 (2021), chưa có trong cacerts của một số bản JDK 17
     * -> lỗi "PKIX path building failed". Tin thêm đúng chứng chỉ gốc đó (bên cạnh các CA mặc định của JDK),
     * chỉ cho các lệnh gọi VNPay.
     */
    private static SSLContext trustingVnPayRoot() {
        try {
            TrustManagerFactory defaults = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            defaults.init((KeyStore) null);
            KeyStore store = KeyStore.getInstance(KeyStore.getDefaultType());
            store.load(null, null);
            int index = 0;
            for (TrustManager manager : defaults.getTrustManagers()) {
                if (manager instanceof X509TrustManager x509) {
                    for (X509Certificate cert : x509.getAcceptedIssuers()) {
                        store.setCertificateEntry("jdk-" + index++, cert);
                    }
                }
            }
            try (InputStream in = new ClassPathResource(VNPAY_ROOT_CA).getInputStream()) {
                store.setCertificateEntry("sectigo-r46", CertificateFactory.getInstance("X.509").generateCertificate(in));
            }
            TrustManagerFactory merged = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            merged.init(store);
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, merged.getTrustManagers(), null);
            return context;
        } catch (Exception e) {
            throw new IllegalStateException("Không nạp được chứng chỉ SSL cho VNPay", e);
        }
    }

    public boolean isConfigured() {
        return props.isConfigured();
    }

    /** Ném lỗi rõ ràng nếu chưa điền VNPAY_TMN_CODE / VNPAY_HASH_SECRET trong .env. */
    public void requireConfigured() {
        if (!props.isConfigured()) {
            throw new IllegalStateException("Hệ thống chưa cấu hình cổng thanh toán VNPay, vui lòng thử lại sau");
        }
    }

    /**
     * @param orderInfo chỉ dùng chữ không dấu (VNPay khuyến nghị)
     * @param expireAt  quá thời điểm này VNPay không cho thanh toán (= hết hạn giữ chỗ)
     */
    public String createPaymentUrl(String txnRef, long amount, String orderInfo, String ipAddr,
                                   LocalDateTime createAt, LocalDateTime expireAt) {
        requireConfigured();
        Map<String, String> params = new LinkedHashMap<>();
        params.put("vnp_Version", VERSION);
        params.put("vnp_Command", "pay");
        params.put("vnp_TmnCode", props.getTmnCode());
        params.put("vnp_Amount", String.valueOf(amount * 100));
        params.put("vnp_CurrCode", "VND");
        params.put("vnp_TxnRef", txnRef);
        params.put("vnp_OrderInfo", orderInfo);
        params.put("vnp_OrderType", "other");
        params.put("vnp_Locale", "vn");
        params.put("vnp_ReturnUrl", props.getReturnUrl());
        params.put("vnp_IpAddr", ipAddr);
        params.put("vnp_CreateDate", createAt.format(VNP_DATE));
        params.put("vnp_ExpireDate", expireAt.format(VNP_DATE));

        String query = VnPaySigner.canonicalQuery(params);
        return props.getPayUrl() + "?" + query + "&" + VnPaySigner.SECURE_HASH + "=" + VnPaySigner.hmacSha512(props.getHashSecret(), query);
    }

    public boolean verify(Map<String, String> params) {
        return props.isConfigured() && VnPaySigner.verify(props.getHashSecret(), params);
    }

    /** Kết quả tra cứu. transactionStatus "00" = khách đã thanh toán thành công. */
    public record QueryResult(boolean reachable, String responseCode, String transactionStatus, Long amount,
                              String transactionNo, String bankCode, String payDate, String message) {
        public boolean paid() {
            return "00".equals(responseCode) && "00".equals(transactionStatus);
        }
    }

    /** Hỏi VNPay kết quả một giao dịch (dùng khi không nhận được Return URL / IPN). */
    public QueryResult query(String txnRef, String transactionDate, String ipAddr, LocalDateTime now) {
        requireConfigured();
        String requestId = UUID.randomUUID().toString().replace("-", "").substring(0, 20);
        String createDate = now.format(VNP_DATE);
        String orderInfo = "Tra cuu giao dich " + txnRef;
        String hashData = String.join("|", requestId, VERSION, "querydr", props.getTmnCode(), txnRef,
                transactionDate, createDate, ipAddr, orderInfo);

        Map<String, String> body = new LinkedHashMap<>();
        body.put("vnp_RequestId", requestId);
        body.put("vnp_Version", VERSION);
        body.put("vnp_Command", "querydr");
        body.put("vnp_TmnCode", props.getTmnCode());
        body.put("vnp_TxnRef", txnRef);
        body.put("vnp_OrderInfo", orderInfo);
        body.put("vnp_TransactionDate", transactionDate);
        body.put("vnp_CreateDate", createDate);
        body.put("vnp_IpAddr", ipAddr);
        body.put("vnp_SecureHash", VnPaySigner.hmacSha512(props.getHashSecret(), hashData));

        Map<String, Object> response = post(body);
        if (response == null) return new QueryResult(false, null, null, null, null, null, null, "Không kết nối được VNPay");
        Object amount = response.get("vnp_Amount");
        return new QueryResult(true, str(response.get("vnp_ResponseCode")), str(response.get("vnp_TransactionStatus")),
                amount == null ? null : Long.parseLong(amount.toString()) / 100,
                str(response.get("vnp_TransactionNo")), str(response.get("vnp_BankCode")),
                str(response.get("vnp_PayDate")), str(response.get("vnp_Message")));
    }

    public record RefundResult(boolean success, String responseCode, String message) {
    }

    /**
     * Hoàn tiền một giao dịch đã thanh toán.
     *
     * @param full            hoàn toàn bộ ("02") hay một phần ("03")
     * @param transactionDate vnp_CreateDate của giao dịch thanh toán gốc
     */
    public RefundResult refund(String txnRef, long amount, String transactionNo, String transactionDate, boolean full,
                               String createdBy, String ipAddr, LocalDateTime now) {
        requireConfigured();
        String requestId = UUID.randomUUID().toString().replace("-", "").substring(0, 20);
        String transactionType = full ? "02" : "03";
        String createDate = now.format(VNP_DATE);
        String orderInfo = "Hoan tien giao dich " + txnRef;
        String amountText = String.valueOf(amount * 100);
        String txnNo = transactionNo == null ? "" : transactionNo;
        String hashData = String.join("|", requestId, VERSION, "refund", props.getTmnCode(), transactionType, txnRef,
                amountText, txnNo, transactionDate, createdBy, createDate, ipAddr, orderInfo);

        Map<String, String> body = new LinkedHashMap<>();
        body.put("vnp_RequestId", requestId);
        body.put("vnp_Version", VERSION);
        body.put("vnp_Command", "refund");
        body.put("vnp_TmnCode", props.getTmnCode());
        body.put("vnp_TransactionType", transactionType);
        body.put("vnp_TxnRef", txnRef);
        body.put("vnp_Amount", amountText);
        body.put("vnp_OrderInfo", orderInfo);
        body.put("vnp_TransactionNo", txnNo);
        body.put("vnp_TransactionDate", transactionDate);
        body.put("vnp_CreateBy", createdBy);
        body.put("vnp_CreateDate", createDate);
        body.put("vnp_IpAddr", ipAddr);
        body.put("vnp_SecureHash", VnPaySigner.hmacSha512(props.getHashSecret(), hashData));

        Map<String, Object> response = post(body);
        if (response == null) return new RefundResult(false, null, "Không kết nối được VNPay");
        String code = str(response.get("vnp_ResponseCode"));
        return new RefundResult("00".equals(code), code, str(response.get("vnp_Message")));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> post(Map<String, String> body) {
        try {
            return restClient.post().uri(props.getApiUrl())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);
        } catch (Exception e) {
            log.warn("Gọi API VNPay ({}) thất bại: {}", body.get("vnp_Command"), e.getMessage());
            return null;
        }
    }

    private static String str(Object value) {
        return value == null ? null : value.toString();
    }
}
