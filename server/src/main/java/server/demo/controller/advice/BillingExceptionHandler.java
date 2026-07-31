package server.demo.controller.advice;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import server.demo.controller.billing.BillingController;
import server.demo.dto.ApiResponse;

import server.demo.i18n.ApiMessages;
/**
 * 计费接口参数/业务校验异常（限定 BillingController，避免影响其他全局处理）。
 */
@Order(Ordered.HIGHEST_PRECEDENCE + 40)
@RestControllerAdvice(assignableTypes = BillingController.class)
public class BillingExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Object>> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
    }

    /**
     * 数据完整性冲突兜底（D2）：正常路径下幂等键冲突已由 SaasBillingIdempotentGateway
     * 恢复为重放，此处仅为最终兜底——命中幂等 uk 时给出可理解的重复提交文案，
     * 其他约束冲突给通用文案，统一 409，不再裸 whitelabel 500。
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleDataIntegrityViolation(DataIntegrityViolationException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(DataConflictMessages.resolve(e)));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .orElse(ApiMessages.get("api.t.db9dec64df60"));
        return ResponseEntity.badRequest().body(ApiResponse.error(message));
    }
}
