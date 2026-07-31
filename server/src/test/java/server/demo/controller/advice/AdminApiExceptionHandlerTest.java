package server.demo.controller.advice;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import server.demo.dto.ApiResponse;
import server.demo.i18n.TestApiMessages;

import java.sql.SQLIntegrityConstraintViolationException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * D2 兜底：AdminApiExceptionHandler 的 DataIntegrityViolationException 映射——
 * 命中幂等 uk 给重复提交专属文案，其他约束冲突给通用文案，统一 409 不裸 whitelabel；
 * 既有 IllegalArgumentException 映射不受影响。
 */
class AdminApiExceptionHandlerTest {

    private final AdminApiExceptionHandler handler = new AdminApiExceptionHandler();

    @BeforeEach
    void setUp() {
        TestApiMessages.install();
    }

    @Test
    void dataIntegrityViolation_idempotencyKeyConflict_returns409WithDuplicateSubmissionMessage() {
        DataIntegrityViolationException e = new DataIntegrityViolationException(
                "could not execute statement",
                new SQLIntegrityConstraintViolationException(
                        "Duplicate entry '5-key-1' for key 'uk_saas_billing_order_idempotency'"));

        ResponseEntity<ApiResponse<Object>> response = handler.handleDataIntegrityViolation(e);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals("重复提交，已为您恢复之前的订单结果", response.getBody().getMessage());
    }

    @Test
    void dataIntegrityViolation_otherConstraint_returns409WithGenericMessage() {
        DataIntegrityViolationException e = new DataIntegrityViolationException(
                "could not execute statement",
                new SQLIntegrityConstraintViolationException(
                        "Duplicate entry '基础版-1' for key 'uk_saas_package_name_version'"));

        ResponseEntity<ApiResponse<Object>> response = handler.handleDataIntegrityViolation(e);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("请求与现有数据冲突，请稍后重试", response.getBody().getMessage());
    }

    @Test
    void illegalArgument_stillMapsTo400WithOriginalMessage() {
        ResponseEntity<ApiResponse<Object>> response =
                handler.handleIllegalArgument(new IllegalArgumentException("门店不存在: 99"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("门店不存在: 99", response.getBody().getMessage());
    }
}
