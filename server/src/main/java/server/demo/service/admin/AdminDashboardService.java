package server.demo.service.admin;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.demo.constants.SaasFeatureCodes;
import server.demo.dto.admin.AdminDtos.DashboardResponse;
import server.demo.dto.admin.AdminDtos.PackageSubscriptionCount;
import server.demo.enums.SaasBillingOrderStatus;
import server.demo.enums.SaasSubscriptionStatus;
import server.demo.repository.StoreRepository;
import server.demo.repository.saas.SaasBillingOrderRepository;
import server.demo.repository.saas.SaasQuotaAccountRepository;
import server.demo.repository.saas.SaasSubscriptionRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 平台管理端概览聚合：门店规模、套餐分布、近 30 天订单额、AI 配额消耗。
 */
@Service
public class AdminDashboardService {

    private static final int ORDER_AMOUNT_WINDOW_DAYS = 30;

    private final StoreRepository storeRepository;
    private final SaasSubscriptionRepository subscriptionRepository;
    private final SaasBillingOrderRepository billingOrderRepository;
    private final SaasQuotaAccountRepository quotaAccountRepository;

    public AdminDashboardService(
            StoreRepository storeRepository,
            SaasSubscriptionRepository subscriptionRepository,
            SaasBillingOrderRepository billingOrderRepository,
            SaasQuotaAccountRepository quotaAccountRepository
    ) {
        this.storeRepository = storeRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.billingOrderRepository = billingOrderRepository;
        this.quotaAccountRepository = quotaAccountRepository;
    }

    @Transactional(readOnly = true)
    public DashboardResponse getDashboard() {
        long totalStores = storeRepository.count();
        long activeSubscriptions = subscriptionRepository.countByStatus(SaasSubscriptionStatus.ACTIVE);

        List<PackageSubscriptionCount> packageCounts = subscriptionRepository
                .countGroupByPackageName(SaasSubscriptionStatus.ACTIVE).stream()
                .map(row -> new PackageSubscriptionCount((String) row[0], ((Number) row[1]).longValue()))
                .toList();

        BigDecimal last30DaysOrderAmount = billingOrderRepository.sumAmountByStatusSince(
                SaasBillingOrderStatus.PAID,
                LocalDateTime.now().minusDays(ORDER_AMOUNT_WINDOW_DAYS));

        Long aiQuotaUsedTotal = quotaAccountRepository.sumUsedQuotaByFeatureCode(SaasFeatureCodes.AI_WEBSITE_GEN);

        return new DashboardResponse(
                totalStores,
                activeSubscriptions,
                packageCounts,
                last30DaysOrderAmount,
                aiQuotaUsedTotal);
    }
}
