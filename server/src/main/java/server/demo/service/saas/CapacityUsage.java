package server.demo.service.saas;

/**
 * 容量型权益用量视图：limit 为 NULL 表示不限；used 为业务库实时 COUNT（容量型不经配额账户记账）。
 */
public record CapacityUsage(
        String featureCode,
        Long limit,
        long used
) {
}
