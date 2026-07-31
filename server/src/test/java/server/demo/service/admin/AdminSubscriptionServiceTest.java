package server.demo.service.admin;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import server.demo.dto.admin.AdminDtos.PagedResponse;
import server.demo.dto.admin.AdminDtos.SubscriptionGrantRequest;
import server.demo.dto.admin.AdminDtos.SubscriptionView;
import server.demo.entity.Store;
import server.demo.entity.saas.SaasSubscription;
import server.demo.enums.SaasSubscriptionStatus;
import server.demo.repository.StoreRepository;
import server.demo.repository.saas.SaasSubscriptionRepository;
import server.demo.service.saas.SaasBillingReplayService;
import server.demo.service.saas.SaasBillingService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;

/**
 * 平台管理端订阅查询 / 人工开通 / 人工取消：
 * 分页边界（size 上限 100、非正回退 20、负页回退 0）、storeId 筛选、门店名关联、
 * 非 ACTIVE 取消拒绝、门店不存在拒绝、套餐不存在由计费层透传。
 */
class AdminSubscriptionServiceTest {

    private SaasSubscriptionRepository subscriptionRepository;
    private StoreRepository storeRepository;
    private SaasBillingService billingService;
    private SaasBillingReplayService billingReplayService;
    private AdminSubscriptionService service;

    @BeforeEach
    void setUp() {
        subscriptionRepository = Mockito.mock(SaasSubscriptionRepository.class);
        storeRepository = Mockito.mock(StoreRepository.class);
        billingService = Mockito.mock(SaasBillingService.class);
        billingReplayService = Mockito.mock(SaasBillingReplayService.class);
        service = new AdminSubscriptionService(
                subscriptionRepository, storeRepository, billingService, billingReplayService);

        lenient().when(subscriptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private SaasSubscription subscription(Long id, Long storeId, SaasSubscriptionStatus status) {
        SaasSubscription subscription = new SaasSubscription();
        subscription.setId(id);
        subscription.setStoreId(storeId);
        subscription.setPackageId(1L);
        subscription.setPackageName("标准版");
        subscription.setPricePaid(new BigDecimal("99.00"));
        subscription.setStartTime(LocalDateTime.now().minusDays(5));
        subscription.setEndTime(LocalDateTime.now().plusDays(25));
        subscription.setStatus(status);
        return subscription;
    }

    private void stubFindAllPage(List<SaasSubscription> content, long total) {
        Mockito.when(subscriptionRepository.findAllByOrderByIdDesc(any()))
                .thenAnswer(inv -> new PageImpl<>(content, inv.getArgument(0), total));
    }

    // ------------------------------------------------------------------
    // 分页边界
    // ------------------------------------------------------------------

    @Test
    void listSubscriptions_sizeAboveMax_clampedTo100() {
        stubFindAllPage(List.of(subscription(1L, 5L, SaasSubscriptionStatus.ACTIVE)), 1);

        PagedResponse<SubscriptionView> response = service.listSubscriptions(null, 0, 500);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        Mockito.verify(subscriptionRepository).findAllByOrderByIdDesc(pageableCaptor.capture());
        assertEquals(100, pageableCaptor.getValue().getPageSize());
        assertEquals(100, response.size());
    }

    @Test
    void listSubscriptions_nonPositiveSize_defaultsTo20() {
        stubFindAllPage(List.of(), 0);

        PagedResponse<SubscriptionView> response = service.listSubscriptions(null, 0, 0);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        Mockito.verify(subscriptionRepository).findAllByOrderByIdDesc(pageableCaptor.capture());
        assertEquals(20, pageableCaptor.getValue().getPageSize());
        assertEquals(20, response.size());
    }

    @Test
    void listSubscriptions_negativePage_clampedToZero() {
        stubFindAllPage(List.of(), 0);

        PagedResponse<SubscriptionView> response = service.listSubscriptions(null, -3, 10);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        Mockito.verify(subscriptionRepository).findAllByOrderByIdDesc(pageableCaptor.capture());
        assertEquals(0, pageableCaptor.getValue().getPageNumber());
        assertEquals(0, response.page());
    }

    // ------------------------------------------------------------------
    // storeId 筛选与门店名关联
    // ------------------------------------------------------------------

    @Test
    void listSubscriptions_withStoreId_usesFilteredQuery() {
        Mockito.when(subscriptionRepository.findByStoreIdOrderByIdDesc(eq(5L), any()))
                .thenAnswer(inv -> new PageImpl<>(
                        List.of(subscription(1L, 5L, SaasSubscriptionStatus.ACTIVE)),
                        inv.getArgument(1), 1));

        PagedResponse<SubscriptionView> response = service.listSubscriptions(5L, 0, 20);

        Mockito.verify(subscriptionRepository).findByStoreIdOrderByIdDesc(eq(5L), any());
        Mockito.verify(subscriptionRepository, never()).findAllByOrderByIdDesc(any());
        assertEquals(1, response.content().size());
        assertEquals(5L, response.content().get(0).storeId());
    }

    @Test
    void listSubscriptions_enrichesStoreNameAndLeavesNullWhenStoreMissing() {
        SaasSubscription sub1 = subscription(1L, 5L, SaasSubscriptionStatus.ACTIVE);
        SaasSubscription sub2 = subscription(2L, 6L, SaasSubscriptionStatus.CANCELLED);
        stubFindAllPage(List.of(sub1, sub2), 2);

        Store store5 = new Store();
        store5.setId(5L);
        store5.setName("门店五");
        Mockito.when(storeRepository.findAllById(any())).thenReturn(List.of(store5));

        PagedResponse<SubscriptionView> response = service.listSubscriptions(null, 0, 20);

        assertEquals(2, response.content().size());
        assertEquals("门店五", response.content().get(0).storeName());
        // 门店记录缺失时不阻断列表，storeName 为 null
        assertNull(response.content().get(1).storeName());
        assertEquals(2L, response.totalElements());
        assertEquals("标准版", response.content().get(0).packageName());
        assertEquals(SaasSubscriptionStatus.CANCELLED, response.content().get(1).status());
    }

    // ------------------------------------------------------------------
    // 人工取消
    // ------------------------------------------------------------------

    @Test
    void cancelSubscription_activeSubscription_markedCancelledAndSaved() {
        SaasSubscription subscription = subscription(10L, 5L, SaasSubscriptionStatus.ACTIVE);
        Mockito.when(subscriptionRepository.findById(10L)).thenReturn(Optional.of(subscription));

        SubscriptionView view = service.cancelSubscription(10L, "admin");

        assertEquals(SaasSubscriptionStatus.CANCELLED, subscription.getStatus());
        Mockito.verify(subscriptionRepository).save(subscription);
        assertEquals(SaasSubscriptionStatus.CANCELLED, view.status());
        assertEquals(10L, view.id());
    }

    @Test
    void cancelSubscription_nonActiveSubscription_rejected() {
        SaasSubscription expired = subscription(11L, 5L, SaasSubscriptionStatus.EXPIRED);
        Mockito.when(subscriptionRepository.findById(11L)).thenReturn(Optional.of(expired));

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> service.cancelSubscription(11L, "admin"));
        assertTrue(e.getMessage().contains("仅进行中的订阅可取消"));
        assertEquals(SaasSubscriptionStatus.EXPIRED, expired.getStatus());
        Mockito.verify(subscriptionRepository, never()).save(any());

        SaasSubscription cancelled = subscription(12L, 5L, SaasSubscriptionStatus.CANCELLED);
        Mockito.when(subscriptionRepository.findById(12L)).thenReturn(Optional.of(cancelled));
        assertThrows(IllegalArgumentException.class,
                () -> service.cancelSubscription(12L, "admin"));
    }

    @Test
    void cancelSubscription_missingSubscription_rejected() {
        Mockito.when(subscriptionRepository.findById(99L)).thenReturn(Optional.empty());

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> service.cancelSubscription(99L, "admin"));
        assertTrue(e.getMessage().contains("订阅不存在"));
        Mockito.verify(subscriptionRepository, never()).save(any());
    }

    // ------------------------------------------------------------------
    // 人工开通/切换（复用 SaasBillingService.grantByAdmin）
    // ------------------------------------------------------------------

    @Test
    void grantSubscription_storeExists_delegatesToBillingGrantByAdmin() {
        Store store = new Store();
        store.setId(5L);
        Mockito.when(storeRepository.findById(5L)).thenReturn(Optional.of(store));

        SaasSubscription granted = subscription(20L, 5L, SaasSubscriptionStatus.ACTIVE);
        granted.setPackageId(2L);
        granted.setPackageName("豪华版");
        Mockito.when(billingService.grantByAdmin(5L, 2L, "admin", "key-1", null, "操作人:admin；客服开通"))
                .thenReturn(granted);

        SubscriptionView view = service.grantSubscription(
                new SubscriptionGrantRequest(5L, 2L, "客服开通", "key-1", null, null), "admin");

        Mockito.verify(billingService).grantByAdmin(5L, 2L, "admin", "key-1", null, "操作人:admin；客服开通");
        assertEquals(20L, view.id());
        assertEquals("豪华版", view.packageName());
        assertEquals(SaasSubscriptionStatus.ACTIVE, view.status());
        // store 未设名称（null）时视图 storeName 为 null，不阻断
        assertNull(view.storeName());
    }

    @Test
    void grantSubscription_responseCarriesStoreName() {
        // P10 回归：grant 响应补 storeName（与列表口径一致，取 stores.name）
        Store store = new Store();
        store.setId(5L);
        store.setName("门店五");
        Mockito.when(storeRepository.findById(5L)).thenReturn(Optional.of(store));

        SaasSubscription granted = subscription(21L, 5L, SaasSubscriptionStatus.ACTIVE);
        granted.setPackageId(2L);
        granted.setPackageName("豪华版");
        Mockito.when(billingService.grantByAdmin(5L, 2L, "admin", "key-2", null, "操作人:admin；客服开通"))
                .thenReturn(granted);

        SubscriptionView view = service.grantSubscription(
                new SubscriptionGrantRequest(5L, 2L, "客服开通", "key-2", null, null), "admin");

        assertEquals("门店五", view.storeName());
    }

    @Test
    void cancelSubscription_responseCarriesStoreName() {
        Store store = new Store();
        store.setId(5L);
        store.setName("门店五");
        Mockito.when(storeRepository.findById(5L)).thenReturn(Optional.of(store));
        SaasSubscription subscription = subscription(22L, 5L, SaasSubscriptionStatus.ACTIVE);
        Mockito.when(subscriptionRepository.findById(22L)).thenReturn(Optional.of(subscription));

        SubscriptionView view = service.cancelSubscription(22L, "admin");

        assertEquals(SaasSubscriptionStatus.CANCELLED, view.status());
        assertEquals("门店五", view.storeName());
    }

    @Test
    void grantSubscription_missingStore_rejectedBeforeBilling() {
        Mockito.when(storeRepository.findById(99L)).thenReturn(Optional.empty());

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> service.grantSubscription(
                        new SubscriptionGrantRequest(99L, 2L, null, null, null, null), "admin"));
        assertTrue(e.getMessage().contains("门店不存在"));
        Mockito.verifyNoInteractions(billingService);
    }

    @Test
    void grantSubscription_unknownPackage_propagatesBillingRejection() {
        Store store = new Store();
        store.setId(5L);
        Mockito.when(storeRepository.findById(5L)).thenReturn(Optional.of(store));
        Mockito.when(billingService.grantByAdmin(5L, 999L, "admin", null, null, "操作人:admin；客服开通"))
                .thenThrow(new IllegalArgumentException("套餐不存在"));

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> service.grantSubscription(
                        new SubscriptionGrantRequest(5L, 999L, "客服开通", null, null, null), "admin"));
        assertTrue(e.getMessage().contains("套餐不存在"));
    }

    // ------------------------------------------------------------------
    // 等级调控（P9）：durationDays / permanent / remark 校验与订单备注
    // ------------------------------------------------------------------

    @Test
    void grantSubscription_durationDays_endTimeOverrideAndOperatorRemark() {
        Store store = new Store();
        store.setId(5L);
        Mockito.when(storeRepository.findById(5L)).thenReturn(Optional.of(store));
        SaasSubscription granted = subscription(21L, 5L, SaasSubscriptionStatus.ACTIVE);
        Mockito.when(billingService.grantByAdmin(eq(5L), eq(2L), eq("admin"), eq(null), any(), any()))
                .thenReturn(granted);

        LocalDateTime before = LocalDateTime.now();
        service.grantSubscription(
                new SubscriptionGrantRequest(5L, 2L, "客服补偿 30 天", null, 30, null), "admin");

        ArgumentCaptor<LocalDateTime> endTimeCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<String> remarkCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(billingService).grantByAdmin(
                eq(5L), eq(2L), eq("admin"), eq(null), endTimeCaptor.capture(), remarkCaptor.capture());
        LocalDateTime override = endTimeCaptor.getValue();
        assertTrue(!override.isBefore(before.plusDays(30)) && !override.isAfter(LocalDateTime.now().plusDays(30)),
                "durationDays=30 时 endTime 应为 now+30 天，实际: " + override);
        assertEquals("操作人:admin；客服补偿 30 天", remarkCaptor.getValue());
    }

    @Test
    void grantSubscription_permanent_endTime2099() {
        Store store = new Store();
        store.setId(5L);
        Mockito.when(storeRepository.findById(5L)).thenReturn(Optional.of(store));
        SaasSubscription granted = subscription(22L, 5L, SaasSubscriptionStatus.ACTIVE);
        Mockito.when(billingService.grantByAdmin(eq(5L), eq(2L), eq("admin"), eq(null), any(), any()))
                .thenReturn(granted);

        service.grantSubscription(
                new SubscriptionGrantRequest(5L, 2L, "战略合作永久授权", null, null, true), "admin");

        ArgumentCaptor<LocalDateTime> endTimeCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        Mockito.verify(billingService).grantByAdmin(
                eq(5L), eq(2L), eq("admin"), eq(null), endTimeCaptor.capture(), any());
        assertEquals(AdminSubscriptionService.PERMANENT_END_TIME, endTimeCaptor.getValue());
        assertEquals(LocalDateTime.of(2099, 12, 31, 23, 59, 59), endTimeCaptor.getValue());
    }

    @Test
    void grantSubscription_permanentWithDurationDays_rejected() {
        Store store = new Store();
        store.setId(5L);
        Mockito.when(storeRepository.findById(5L)).thenReturn(Optional.of(store));

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> service.grantSubscription(
                        new SubscriptionGrantRequest(5L, 2L, "备注", null, 30, true), "admin"));
        assertTrue(e.getMessage().contains("permanent 与 durationDays 不可同时指定"));
        Mockito.verifyNoInteractions(billingService);
    }

    @Test
    void grantSubscription_durationDaysOutOfRange_rejected() {
        Store store = new Store();
        store.setId(5L);
        Mockito.when(storeRepository.findById(5L)).thenReturn(Optional.of(store));

        assertThrows(IllegalArgumentException.class,
                () -> service.grantSubscription(
                        new SubscriptionGrantRequest(5L, 2L, "备注", null, 0, null), "admin"));
        assertThrows(IllegalArgumentException.class,
                () -> service.grantSubscription(
                        new SubscriptionGrantRequest(5L, 2L, "备注", null, 36501, null), "admin"));
        Mockito.verifyNoInteractions(billingService);
    }

    @Test
    void grantSubscription_blankOrMissingRemark_rejected() {
        Store store = new Store();
        store.setId(5L);
        Mockito.when(storeRepository.findById(5L)).thenReturn(Optional.of(store));

        assertThrows(IllegalArgumentException.class,
                () -> service.grantSubscription(
                        new SubscriptionGrantRequest(5L, 2L, null, null, null, null), "admin"));
        IllegalArgumentException blank = assertThrows(IllegalArgumentException.class,
                () -> service.grantSubscription(
                        new SubscriptionGrantRequest(5L, 2L, "   ", null, null, null), "admin"));
        assertTrue(blank.getMessage().contains("remark 不能为空"));
        Mockito.verifyNoInteractions(billingService);
    }

    @Test
    void grantSubscription_remarkTooLong_rejected() {
        Store store = new Store();
        store.setId(5L);
        Mockito.when(storeRepository.findById(5L)).thenReturn(Optional.of(store));

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> service.grantSubscription(
                        new SubscriptionGrantRequest(5L, 2L, "x".repeat(501), null, null, null), "admin"));
        assertTrue(e.getMessage().contains("remark 长度不能超过 500"));
        Mockito.verifyNoInteractions(billingService);
    }

    @Test
    void composeOrderRemark_truncatesToOrderColumnWidth() {
        String remark = "x".repeat(500);
        String composed = AdminSubscriptionService.composeOrderRemark("admin", remark);
        assertEquals(500, composed.length());
        assertTrue(composed.startsWith("操作人:admin；"));

        // 操作人缺失时兜底 unknown，不产成 NPE/空白前缀
        assertTrue(AdminSubscriptionService.composeOrderRemark(null, "备注").startsWith("操作人:unknown；"));
    }

    // ------------------------------------------------------------------
    // D2：人工开通 uk 冲突后的幂等恢复重查（REQUIRES_NEW 由代理保证）
    // ------------------------------------------------------------------

    @Test
    void findReplaySubscriptionView_orderCommitted_mapsToView() {
        SaasSubscription winner = subscription(30L, 5L, SaasSubscriptionStatus.ACTIVE);
        Mockito.when(billingReplayService.findReplaySubscription(5L, "key-1"))
                .thenReturn(Optional.of(winner));

        Optional<SubscriptionView> view = service.findReplaySubscriptionView(5L, "key-1");

        assertTrue(view.isPresent());
        assertEquals(30L, view.get().id());
        assertEquals(5L, view.get().storeId());
        assertEquals(SaasSubscriptionStatus.ACTIVE, view.get().status());
    }

    @Test
    void findReplaySubscriptionView_noCommittedOrder_returnsEmpty() {
        Mockito.when(billingReplayService.findReplaySubscription(5L, "key-x"))
                .thenReturn(Optional.empty());

        Optional<SubscriptionView> view = service.findReplaySubscriptionView(5L, "key-x");

        assertTrue(view.isEmpty());
    }
}
