package server.demo.enums;

/**
 * 虚拟邮箱状态枚举
 */
public enum MailboxStatus {
    /**
     * 活跃状态 - 可正常收发邮件
     */
    ACTIVE,

    /**
     * 已关闭 - 不再接收新邮件
     */
    CLOSED
}
