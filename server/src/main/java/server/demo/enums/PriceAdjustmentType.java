package server.demo.enums;

/**
 * 价格调整类型枚举
 * 用于定义渠道价格相对于基础价格的调整方式
 */
public enum PriceAdjustmentType {
    /**
     * 佣金加成：渠道价格 = 基础价格 × (1 + 佣金率)
     */
    COMMISSION,

    /**
     * 固定金额：渠道价格 = 基础价格 + 固定金额（可为负数表示更便宜）
     */
    FIXED,

    /**
     * 百分比：渠道价格 = 基础价格 × (1 + 百分比)（可为负数表示更便宜）
     */
    PERCENTAGE
}
