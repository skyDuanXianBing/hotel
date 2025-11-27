package server.demo.enums;

/**
 * PriceLabs 同步类型枚举
 */
public enum SyncType {
    /**
     * 房源同步
     */
    LISTING,

    /**
     * 价格计划同步
     */
    RATE_PLAN,

    /**
     * 日历数据同步
     */
    CALENDAR,

    /**
     * 预订数据同步
     */
    RESERVATION,

    /**
     * 价格更新（从 PriceLabs 接收）
     */
    PRICE_UPDATE
}
