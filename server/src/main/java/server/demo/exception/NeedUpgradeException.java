package server.demo.exception;

/**
 * 当前套餐不包含所需权益或权益已用尽（HTTP 402 语义）。
 * 携带 featureCode / limit / used / reason，供前端做统一升级引导。
 */
public class NeedUpgradeException extends RuntimeException {

    /**
     * 402 触发原因。前端据此区分引导话术：门店尚未开通套餐（先购买）与
     * 已开通但套餐不含该权益/额度用尽（升级）是两种不同的引导路径。
     */
    public enum Reason {
        /** 门店没有任何有效订阅（尚未开通套餐）。 */
        NO_SUBSCRIPTION,
        /** 有有效订阅，但当前套餐不包含该权益。 */
        NOT_INCLUDED,
        /** 配额型权益额度已用尽。 */
        QUOTA_EXHAUSTED,
        /** 容量型权益已达上限。 */
        CAPACITY_EXCEEDED
    }

    private final String featureCode;
    /** 权益上限；NULL 表示不限或不适用。 */
    private final Long limit;
    /** 当前用量；不适用时为 NULL。 */
    private final Long used;
    /** 触发原因；未显式指定时按 NOT_INCLUDED（与历史行为一致）。 */
    private final Reason reason;

    public NeedUpgradeException(String featureCode, Long limit, Long used, String message) {
        this(featureCode, limit, used, message, Reason.NOT_INCLUDED);
    }

    public NeedUpgradeException(String featureCode, Long limit, Long used, String message, Reason reason) {
        super(message);
        this.featureCode = featureCode;
        this.limit = limit;
        this.used = used;
        this.reason = reason;
    }

    public String getFeatureCode() {
        return featureCode;
    }

    public Long getLimit() {
        return limit;
    }

    public Long getUsed() {
        return used;
    }

    public Reason getReason() {
        return reason;
    }
}
