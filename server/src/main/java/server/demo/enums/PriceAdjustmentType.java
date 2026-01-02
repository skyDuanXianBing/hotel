package server.demo.enums;

/**
 * 价格调整类型枚举
 * 用于定义渠道价格相对于基础价格的调整方式
 *
 * 使用场景说明:
 * - OTA 直连集成 (ota_integrations 表): 仅使用 PERCENTAGE 和 FIXED
 * - 其他渠道 (channels 表): 可使用所有三种类型
 */
public enum PriceAdjustmentType {
    /**
     * 佣金加成：渠道价格 = 基础价格 × (1 + 佣金率)
     * 注意：此类型仅用于 Channel 实体，OTA 集成不使用此类型
     * @deprecated 推荐使用 PERCENTAGE 替代，两者计算方式相同
     */
    COMMISSION,

    /**
     * 固定金额：渠道价格 = 基础价格 + 固定金额（可为负数表示更便宜）
     */
    FIXED,

    /**
     * 百分比：渠道价格 = 基础价格 × (1 + 百分比)（可为负数表示更便宜）
     * OTA 集成推荐使用此类型
     */
    PERCENTAGE
}
