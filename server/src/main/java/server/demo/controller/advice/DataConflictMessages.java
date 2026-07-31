package server.demo.controller.advice;

import org.springframework.dao.DataIntegrityViolationException;

/**
 * 数据完整性冲突的文案解析（D2 兜底，Billing/Admin 两侧 handler 共用）：
 * 命中幂等唯一键（uk_saas_billing_order_idempotency，按约束名中的 "idempotency" 识别，
 * 兼容 MySQL "Duplicate entry ... for key '...'" 与其他数据库的改写文案）时给出
 * 重复提交专属文案；其他约束冲突给通用文案。均不向前端透传 SQL/约束明细。
 */
final class DataConflictMessages {

    /** 幂等键冲突：重复提交/双击/重试被唯一键拦截（正常路径已被包装层恢复为重放）。 */
    static final String IDEMPOTENCY_CONFLICT = "重复提交，已为您恢复之前的订单结果";

    /** 其他唯一键/外键/非空等约束冲突的通用文案。 */
    static final String GENERIC_CONFLICT = "请求与现有数据冲突，请稍后重试";

    private DataConflictMessages() {
    }

    static String resolve(DataIntegrityViolationException e) {
        return isIdempotencyConflict(e) ? IDEMPOTENCY_CONFLICT : GENERIC_CONFLICT;
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
