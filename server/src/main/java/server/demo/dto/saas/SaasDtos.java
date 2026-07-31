package server.demo.dto.saas;

import jakarta.validation.constraints.NotNull;
import server.demo.enums.SaasFeatureType;
import server.demo.enums.SaasPackagePeriod;
import server.demo.enums.SaasSubscriptionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * SaaS 计费/订阅接口 DTO。
 */
public final class SaasDtos {

    private SaasDtos() {
    }

    public record PackageFeatureView(
            String featureCode,
            String name,
            SaasFeatureType type,
            String unit,
            Long quotaLimit
    ) {
    }

    public record PackageView(
            Long id,
            String name,
            Integer version,
            BigDecimal price,
            SaasPackagePeriod period,
            String description,
            boolean systemPackage,
            List<PackageFeatureView> features
    ) {
    }

    public record SubscribeRequest(
            @NotNull(message = "packageId 不能为空") Long packageId,
            String idempotencyKey
    ) {
    }

    public record QuotaUsageView(
            String featureCode,
            String name,
            Long totalQuota,
            long usedQuota,
            Long remaining,
            LocalDateTime periodStart,
            LocalDateTime periodEnd
    ) {
    }

    public record EntitlementView(
            String featureCode,
            SaasFeatureType type,
            Long limit
    ) {
    }

    public record CapacityUsageView(
            String featureCode,
            String name,
            Long limit,
            long used
    ) {
    }

    public record SubscriptionView(
            Long id,
            Long packageId,
            String packageName,
            BigDecimal pricePaid,
            LocalDateTime startTime,
            LocalDateTime endTime,
            SaasSubscriptionStatus status,
            boolean systemPackage,
            List<EntitlementView> entitlements,
            List<QuotaUsageView> quotas,
            List<CapacityUsageView> capacityUsages
    ) {
    }
}
