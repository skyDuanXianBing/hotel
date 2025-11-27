package server.demo.enums;

/**
 * 同步方向枚举
 */
public enum SyncDirection {
    /**
     * 出站：从 PMS 推送到 PriceLabs 或 OTA
     */
    OUTBOUND,

    /**
     * 入站：从 PriceLabs 接收数据
     */
    INBOUND
}
