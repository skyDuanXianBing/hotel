package server.demo.service.saas;

import java.time.LocalDateTime;

/**
 * 配额用量视图：totalQuota/remaining 为 NULL 表示不限。
 */
public record QuotaUsage(
        String featureCode,
        Long totalQuota,
        long usedQuota,
        Long remaining,
        LocalDateTime periodStart,
        LocalDateTime periodEnd
) {
}
