package server.demo.dto;

/**
 * App 图标角标汇总：未读聊天消息数 + 待审查表格数。
 */
public record NotificationBadgeSummaryDTO(long unreadMessages, long pendingReviews, long total) {
}
