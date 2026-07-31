package server.demo.controller.advice;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import server.demo.controller.admin.AdminAuthController;
import server.demo.controller.admin.AdminDashboardController;
import server.demo.controller.admin.AdminFeatureController;
import server.demo.controller.admin.AdminPackageController;
import server.demo.controller.admin.AdminQuotaController;
import server.demo.controller.admin.AdminStoreController;
import server.demo.controller.admin.AdminSubscriptionController;
import server.demo.dto.ApiResponse;

import server.demo.i18n.ApiMessages;
/**
 * 平台管理端（/api/admin/**）参数/业务校验异常统一处理，
 * 限定 admin 控制器，避免影响其他全局处理。
 */
@Order(Ordered.HIGHEST_PRECEDENCE + 40)
@RestControllerAdvice(assignableTypes = {
        AdminAuthController.class,
        AdminPackageController.class,
        AdminFeatureController.class,
        AdminSubscriptionController.class,
        AdminQuotaController.class,
        AdminStoreController.class,
        AdminDashboardController.class
})
public class AdminApiExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Object>> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
    }

    /**
     * 数据完整性冲突兜底（D2）：人工开通的并发同幂等键冲突正常已由
     * AdminSubscriptionController 边界恢复为重放，此处仅为最终兜底——命中幂等 uk
     * 给重复提交文案，其他约束冲突给通用文案，统一 409，不再裸 whitelabel 500。
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

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Object>> handleNotReadable(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest().body(ApiResponse.error(ApiMessages.get("api.t.18e8b7db0150")));
    }
}
