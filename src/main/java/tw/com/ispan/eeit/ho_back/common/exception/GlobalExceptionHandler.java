package tw.com.ispan.eeit.ho_back.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.time.LocalDateTime;
import java.util.Map;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GlobalExceptionHandler {

    // 統一建構錯誤回應
    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message, HttpServletRequest req) {
        ErrorResponse body = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(req.getRequestURI())
                .build();
        return ResponseEntity.status(status).body(body);
    }

    // 404：找不到資源（你可在 Controller 查無資料時丟 IllegalArgumentException）
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(IllegalArgumentException ex, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), req);
    }

    // 400：JSON 格式錯誤 / 無法反序列化
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleBadJson(HttpMessageNotReadableException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "Malformed JSON request", req);
    }

    // 400：缺少必要的 query/path 參數
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(MissingServletRequestParameterException ex,
            HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "Missing parameter: " + ex.getParameterName(), req);
    }

    // 400：@Valid 驗證錯誤（Body 物件欄位）
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        // 聚合所有欄位錯誤為 field -> message 的 map
        java.util.Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
                .collect(java.util.stream.Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage,
                        (m1, m2) -> m1 + "; " + m2));

        String message = errors.values().stream().findFirst().orElse("Validation failed");
        ErrorResponse body = ErrorResponse.builder()
                .timestamp(java.time.LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message(message)
                .path(req.getRequestURI())
                .errors(errors)
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // 400：@Validated 驗證錯誤（Query/Path 參數）
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex,
            HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), req);
    }

    // 405：HTTP 方法不支援
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex,
            HttpServletRequest req) {
        return build(HttpStatus.METHOD_NOT_ALLOWED, "Method not allowed", req);
    }

    // 409：資料庫唯一鍵/外鍵等衝突
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex,
            HttpServletRequest req) {
        return build(HttpStatus.CONFLICT, "Data integrity violation", req);
    }

    // 404：路由不存在（需開啟 NoHandlerFound，見下方小提醒）
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoHandler(NoHandlerFoundException ex, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, "No handler found for " + ex.getHttpMethod() + " " + ex.getRequestURL(),
                req);
    }

    // 404：飯店不存在
    @ExceptionHandler(HotelNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleHotelNotFoundException(HotelNotFoundException ex,
            HttpServletRequest req) {
        String message = ex.getMessage() != null ? ex.getMessage() : "飯店不存在";
        return build(HttpStatus.NOT_FOUND, message, req);
    }

    // 404：房型床型配置不存在
    @ExceptionHandler(RoomTypeBedTypeNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleRoomTypeBedTypeNotFoundException(
            RoomTypeBedTypeNotFoundException ex, HttpServletRequest req) {
        String message = ex.getMessage() != null ? ex.getMessage() : "房型床型配置不存在";
        return build(HttpStatus.NOT_FOUND, message, req);
    }

    // 403：安全異常（權限不足）
    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ErrorResponse> handleSecurityException(SecurityException ex, HttpServletRequest req) {
        String message = ex.getMessage() != null ? ex.getMessage() : "權限不足";
        return build(HttpStatus.FORBIDDEN, message, req);
    }

    // 400：空指標異常
    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<ErrorResponse> handleNullPointerException(NullPointerException ex, HttpServletRequest req) {
        ErrorResponse body = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message("發生空指標異常，請檢查資料完整性")
                .path(req.getRequestURI())
                .build();
        // 在開發環境中輸出堆疊追蹤
        ex.printStackTrace();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // 處理登入錯誤（統一返回格式為 ErrorResponse）
    @ExceptionHandler(LoginException.class)
    public ResponseEntity<ErrorResponse> handleLoginException(LoginException ex, HttpServletRequest req) {
        Map<String, String> errorMessages = ex.getErrors();
        String message = errorMessages != null && !errorMessages.isEmpty()
                ? errorMessages.values().iterator().next()
                : "登入失敗";

        ErrorResponse body = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message(message)
                .path(req.getRequestURI())
                .errors(errorMessages)
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // 500：兜底
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex, HttpServletRequest req) {
        // 印出完整的錯誤訊息，方便除錯
        ex.printStackTrace();
        String message = ex.getMessage() != null ? ex.getMessage() : "Internal Server Error";
        return build(HttpStatus.INTERNAL_SERVER_ERROR, message, req);
    }
}
