package server.demo.controller.advice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import server.demo.controller.IndependentSiteController;
import server.demo.controller.PublicIndependentSiteController;
import server.demo.controller.PublicIndependentSiteStripeWebhookController;
import server.demo.dto.ApiResponse;
import server.demo.exception.PermissionDeniedException;
import server.demo.service.IndependentSiteServiceException;

import java.util.Map;

@RestControllerAdvice(assignableTypes = {
        IndependentSiteController.class,
        PublicIndependentSiteController.class,
        PublicIndependentSiteStripeWebhookController.class
})
public class IndependentSiteApiExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(IndependentSiteApiExceptionHandler.class);

    @ExceptionHandler(IndependentSiteServiceException.class)
    public ResponseEntity<ApiResponse<Object>> handleService(IndependentSiteServiceException exception) {
        return ResponseEntity.status(exception.getStatus())
                .body(ApiResponse.error(
                        exception.getMessage(),
                        Map.of("code", exception.getCode())
                ));
    }

    @ExceptionHandler(PermissionDeniedException.class)
    public ResponseEntity<ApiResponse<Object>> handlePermissionDenied(
            PermissionDeniedException exception
    ) {
        return ResponseEntity.status(403).body(ApiResponse.error(exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidation(MethodArgumentNotValidException exception) {
        FieldError firstError = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .orElse(null);
        String message = firstError == null
                ? "请求参数不正确"
                : firstError.getField() + " 参数不正确";
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(message, Map.of("code", "VALIDATION_FAILED")));
    }

    @ExceptionHandler({IllegalArgumentException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<ApiResponse<Object>> handleBadRequest(Exception exception) {
        String message = exception instanceof IllegalArgumentException
                ? exception.getMessage()
                : "请求格式不正确";
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(message, Map.of("code", "INVALID_REQUEST")));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleConflict(DataIntegrityViolationException exception) {
        logger.warn("Independent-site data conflict: {}", exception.getMostSpecificCause().getMessage());
        return ResponseEntity.status(409)
                .body(ApiResponse.error("请求与现有数据冲突", Map.of("code", "DATA_CONFLICT")));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleUnexpected(Exception exception) {
        logger.error("Independent-site unexpected error", exception);
        return ResponseEntity.status(500)
                .body(ApiResponse.error("系统错误，请稍后重试", Map.of("code", "INTERNAL_ERROR")));
    }
}
