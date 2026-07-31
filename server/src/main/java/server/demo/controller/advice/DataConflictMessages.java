package server.demo.controller.advice;

import org.springframework.dao.DataIntegrityViolationException;
import server.demo.i18n.ApiMessages;

/**
 * 数据完整性冲突的文案解析（D2 兜底，Billing/Admin 两侧 handler 共用）：
 * 命中幂等唯一键（uk_saas_billing_order_idempotency，按约束名中的 "idempotency" 识别，
 * 兼容 MySQL "Duplicate entry ... for key '...'" 与其他数据库的改写文案）时给出
 * 重复提交专属文案；其他约束冲突给通用文案。均不向前端透传 SQL/约束明细。
 */
final class DataConflictMessages {

    private DataConflictMessages() {
    }

    static String resolve(DataIntegrityViolationException e) {
        return isIdempotencyConflict(e)
                ? ApiMessages.get("api.conflict.idempotency")
                : ApiMessages.get("api.conflict.generic");
    }

    private static boolean isIdempotencyConflict(DataIntegrityViolationException e) {
        String message = null;
        if (e.getMostSpecificCause() != null) {
            message = e.getMostSpecificCause().getMessage();
        }
        if (message == null) {
            message = e.getMessage();
        }
        return message != null && message.toLowerCase().contains("idempotency");
    }
}
