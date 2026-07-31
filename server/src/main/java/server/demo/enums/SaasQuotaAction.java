package server.demo.enums;

public enum SaasQuotaAction {
    DEDUCT,
    REFUND,
    GRANT,
    ADJUST,
    RESET,
    /** 套餐激活/续费时仅调整配额账户的额度上限（used 与周期锚点不变，delta 恒为 0）。 */
    LIMIT_CHANGE
}
