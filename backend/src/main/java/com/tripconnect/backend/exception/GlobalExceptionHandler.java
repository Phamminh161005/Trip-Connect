package com.tripconnect.backend.exception;

import com.tripconnect.backend.dto.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String OUR_PACKAGE = "com.tripconnect.";

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleEmailExists(EmailAlreadyExistsException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(InvalidOtpException.class)
    public ResponseEntity<ErrorResponse> handleInvalidOtp(InvalidOtpException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(InvalidCredentialsException ex) {
        return build(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRefreshToken(InvalidRefreshTokenException ex) {
        return build(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler(AccountLockedException.class)
    public ResponseEntity<ErrorResponse> handleAccountLocked(AccountLockedException ex) {
        return build(HttpStatus.LOCKED, ex.getMessage());
    }

    @ExceptionHandler(TooManyRequestsException.class)
    public ResponseEntity<ErrorResponse> handleTooManyRequests(TooManyRequestsException ex) {
        return build(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage());
    }

    @ExceptionHandler(InvalidFileException.class)
    public ResponseEntity<ErrorResponse> handleInvalidFile(InvalidFileException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        return build(HttpStatus.PAYLOAD_TOO_LARGE, "File tải lên quá lớn");
    }

    @ExceptionHandler(FileStorageException.class)
    public ResponseEntity<ErrorResponse> handleFileStorage(FileStorageException ex) {
        // 502: lỗi đến từ dịch vụ bên ngoài (Cloudinary), không phải do dữ liệu người dùng gửi
        return build(HttpStatus.BAD_GATEWAY, ex.getMessage());
    }

    @ExceptionHandler(PropertyReferenceException.class)
    public ResponseEntity<ErrorResponse> handleInvalidSort(PropertyReferenceException ex) {
        // Client gửi ?sort=truong_khong_ton_tai
        return build(HttpStatus.BAD_REQUEST, "Tham số sắp xếp không hợp lệ: " + ex.getPropertyName());
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(ForbiddenException ex) {
        return build(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(AccountDisabledException.class)
    public ResponseEntity<ErrorResponse> handleAccountDisabled(AccountDisabledException ex) {
        return build(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex) {
        // Lưới an toàn khi ràng buộc UNIQUE ở DB chặn (ví dụ 2 request gửi cùng lúc)
        return build(HttpStatus.CONFLICT, "Dữ liệu bị trùng hoặc đã thay đổi, vui lòng tải lại trang và thử lại");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableBody(HttpMessageNotReadableException ex) {
        // JSON sai cú pháp, sai kiểu (chữ vào ô số), giá trị enum / ngày giờ không hợp lệ
        return build(HttpStatus.BAD_REQUEST, "Dữ liệu gửi lên không đúng định dạng");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        // Ví dụ ?status=ABC hoặc /tours/abc
        return build(HttpStatus.BAD_REQUEST, "Tham số không hợp lệ: " + ex.getName());
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> handleMethodValidation(HandlerMethodValidationException ex) {
        // Ràng buộc đặt thẳng trên tham số (vd @Min trên ?limit=)
        return build(HttpStatus.BAD_REQUEST, "Tham số không hợp lệ");
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleConcurrentUpdate(ObjectOptimisticLockingFailureException ex) {
        // Hai người cùng thao tác trên một bản ghi (vd Admin duyệt đúng lúc Agent rút lại tour)
        return build(HttpStatus.CONFLICT, "Dữ liệu vừa được người khác thay đổi, vui lòng tải lại trang và thử lại");
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<ErrorResponse> handleBadRequest(RuntimeException ex) {
        // Lỗi nghiệp vụ từ code của ta trả 400; từ thư viện thì trả 500, không lộ message.
        if (!thrownByOurCode(ex)) {
            log.error("Lỗi nội bộ không mong đợi", ex);
            return build(HttpStatus.INTERNAL_SERVER_ERROR, "Hệ thống đang gặp sự cố, vui lòng thử lại sau");
        }
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResource(NoResourceFoundException ex) {
        return build(HttpStatus.NOT_FOUND, "Không tìm thấy đường dẫn API");
    }

    private static boolean thrownByOurCode(Throwable ex) {
        StackTraceElement[] trace = ex.getStackTrace();
        return trace.length > 0 && trace[0].getClassName().startsWith(OUR_PACKAGE);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            // Sai kiểu (chữ vào ô số, ngày sai định dạng...) -> Spring trả câu tiếng Anh kỹ thuật, thay bằng câu dễ hiểu
            fieldErrors.put(error.getField(), error.isBindingFailure() ? "Giá trị không hợp lệ" : error.getDefaultMessage());
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Validation Failed");
        body.put("fieldErrors", fieldErrors);

        return ResponseEntity.badRequest().body(body);
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message) {
        ErrorResponse response = new ErrorResponse(LocalDateTime.now(), status.value(), status.getReasonPhrase(), message);
        return ResponseEntity.status(status).body(response);
    }
}