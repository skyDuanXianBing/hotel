package server.demo.service.admin;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.data.jpa.repository.Query;
import server.demo.constants.SaasFeatureCodes;
import server.demo.dto.admin.AdminDtos.DashboardResponse;
import server.demo.enums.SaasBillingOrderStatus;
import server.demo.enums.SaasSubscriptionStatus;
import server.demo.repository.StoreRepository;
import server.demo.repository.saas.SaasBillingOrderRepository;
import server.demo.repository.saas.SaasQuotaAccountRepository;
import server.demo.repository.saas.SaasSubscriptionRepository;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

/**
 * 平台管理端概览聚合：门店总数、ACTIVE 订阅数、按套餐分布、
 * 近 30 天 PAID 订单额、AI 配额已用总量。
 */
class AdminDashboardServiceTest {

    private StoreRepository storeRepository;
    private SaasSubscriptionRepository subscriptionRepository;
    private SaasBillingOrderRepository billingOrderRepository;
    private SaasQuotaAccountRepository quotaAccountRepository;
    private AdminDashboardService service;

    @BeforeEach
    void setUp() {
        storeRepository = Mockito.mock(StoreRepository.class);
        subscriptionRepository = Mockito.mock(SaasSubscriptionRepository.class);
        billingOrderRepository = Mockito.mock(SaasBillingOrderRepository.class);
        quotaAccountRepository = Mockito.mock(SaasQuotaAccountRepository.class);
        service = new AdminDashboardService(
                storeRepository, subscriptionRepository, billingOrderRepository, quotaAccountRepository);
    }

    @Test
    void getDashboard_aggregatesAllSources() {
        Mockito.when(storeRepository.count()).thenReturn(5L);
        Mockito.when(subscriptionRepository.countByStatus(SaasSubscriptionStatus.ACTIVE))
                .thenReturn(5L);
        Mockito.when(subscriptionRepository.countGroupByPackageName(SaasSubscriptionStatus.ACTIVE))
                .thenReturn(List.of(
                        new Object[]{"默认版", 4L},
                        new Object[]{"标准版", 1L}));
        Mockito.when(billingOrderRepository.sumAmountByStatusSince(
                        eq(SaasBillingOrderStatus.PAID), any()))
                .thenReturn(new BigDecimal("2999.00"));
        Mockito.when(quotaAccountRepository.sumUsedQuotaByFeatureCode(SaasFeatureCodes.AI_WEBSITE_GEN))
                .thenReturn(42L);

        DashboardResponse response = service.getDashboard();

        assertEquals(5L, response.totalStores());
        assertEquals(5L, response.activeSubscriptions());

        assertEquals(2, response.packageSubscriptionCounts().size());
        assertEquals("默认版", response.packageSubscriptionCounts().get(0).packageName());
        assertEquals(4L, response.packageSubscriptionCounts().get(0).count());
        assertEquals("标准版", response.packageSubscriptionCounts().get(1).packageName());
        assertEquals(1L, response.packageSubscriptionCounts().get(1).count());

        assertEquals(0, response.last30DaysOrderAmount().compareTo(new BigDecimal("2999.00")));
        assertEquals(42L, response.aiQuotaUsedTotal());

        // 订单额统计窗口为近 30 天，且只统计 PAID 订单
        ArgumentCaptor<LocalDateTime> sinceCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        Mockito.verify(billingOrderRepository).sumAmountByStatusSince(
                eq(SaasBillingOrderStatus.PAID), sinceCaptor.capture());
        LocalDateTime since = sinceCaptor.getValue();
        assertTrue(since.isAfter(LocalDateTime.now().minusDays(31)),
                "窗口起点应约在 30 天前，实际: " + since);
        assertTrue(since.isBefore(LocalDateTime.now().minusDays(29)),
                "窗口起点应约在 30 天前，实际: " + since);
    }

    @Test
    void getDashboard_emptyPlatform_returnsZerosAndEmptyDistribution() {
        Mockito.when(storeRepository.count()).thenReturn(0L);
        Mockito.when(subscriptionRepository.countByStatus(SaasSubscriptionStatus.ACTIVE))
                .thenReturn(0L);
        Mockito.when(subscriptionRepository.countGroupByPackageName(SaasSubscriptionStatus.ACTIVE))
                .thenReturn(List.of());
        Mockito.when(billingOrderRepository.sumAmountByStatusSince(
                        eq(SaasBillingOrderStatus.PAID), any()))
                .thenReturn(BigDecimal.ZERO);
        Mockito.when(quotaAccountRepository.sumUsedQuotaByFeatureCode(SaasFeatureCodes.AI_WEBSITE_GEN))
                .thenReturn(0L);

        DashboardResponse response = service.getDashboard();

        assertEquals(0L, response.totalStores());
        assertEquals(0L, response.activeSubscriptions());
        assertTrue(response.packageSubscriptionCounts().isEmpty());
        assertEquals(0, response.last30DaysOrderAmount().compareTo(BigDecimal.ZERO));
        assertEquals(0L, response.aiQuotaUsedTotal());
    }

    /**
     * 仓储层静态审查：人工补偿贷记可使单行 used_quota 为负，已用总量必须
     * 按行夹回 0 再求和（GREATEST），否则概览会出现负数。mock 测试覆盖不到
     * JPQL 本身，此处守住查询契约。
     */
    @Test
    void sumUsedQuotaByFeatureCode_clampsNegativeRowsPerRow() throws Exception {
        Method method = SaasQuotaAccountRepository.class
                .getMethod("sumUsedQuotaByFeatureCode", String.class);
        Query query = method.getAnnotation(Query.class);
        assertNotNull(query, "sumUsedQuotaByFeatureCode 应显式声明 JPQL");
        String jpql = query.value().replaceAll("\\s+", " ").toUpperCase();
        assertTrue(jpql.contains("SUM(GREATEST("),
                "已用总量应按行 GREATEST(usedQuota, 0) 夹回再求和，实际: " + query.value());
    }
}
