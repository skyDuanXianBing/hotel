package server.demo.controller.advice;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import server.demo.dto.ApiResponse;
import server.demo.i18n.ApiMessages;

/**
 * Main API fallback for bean-validation failures → {@link ApiResponse}.
 * <p>
 * Ordered after assignableTypes-scoped handlers (Admin / Billing / IndependentSite)
 * so those keep their own validation envelopes. Spring's
 * {@code ExceptionHandlerExceptionResolver} picks the first matching advice in order.
 */
@Order(Ordered.LOWEST_PRECEDENCE)
@RestControllerAdvice
public class ApiValidationExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        return ResponseEntity.badRequest().body(ApiResponse.error(resolveMessage(e.getBindingResult().getAllErrors())));
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<Object>> handleBindException(BindException e) {
        return ResponseEntity.badRequest().body(ApiResponse.error(resolveMessage(e.getAllErrors())));
    }

    private static String resolveMessage(java.util.List<ObjectError> errors) {
        if (errors == null || errors.isEmpty()) {
            return ApiMessages.get("api.validation.failed");
        }
        ObjectError first = errors.get(0);
        String defaultMessage = first.getDefaultMessage();
        if (defaultMessage != null && !defaultMessage.isBlank()) {
            return defaultMessage;
        }
        if (first instanceof FieldError fieldError) {
            return fieldError.getField() + " " + ApiMessages.get("api.validation.failed");
        }
        return ApiMessages.get("api.validation.failed");
    }
}
