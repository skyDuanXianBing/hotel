package server.demo.service.saas;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.UnexpectedRollbackException;
import server.demo.entity.saas.SaasBillingOrder;
import server.demo.entity.saas.SaasSubscription;
import server.demo.enums.SaasSubscriptionStatus;
import server.demo.repository.saas.SaasBillingOrderRepository;
import server.demo.repository.saas.SaasSubscriptionRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;

/**
 * D2 修复：真并发同 idempotencyKey 购买的败者 uk 冲突恢复。
 * 包装层（事务边界之外）捕获 DataIntegrityViolationException / UnexpectedRollbackException 后，
 * 经 SaasBillingReplayService 的独立新事务方法按幂等键重查先到者已提交订单，幂等重放其订阅；
 * 重查未命中（冲突与幂等键无关）时原样抛出原始异常。
 * （本测试以真实 SaasBillingReplayService + mock 仓储验证"包装层新事务重放"的解析链路；
 *  REQUIRES_NEW 传播由 Spring 事务代理在集成层保证。）
 */
class SaasBillingIdempotentGatewayTest {

    private static final long STORE_ID = 9L;
    private static final String KEY = "6f9c2f6e-9b1a-4c7e-9f2d-2f6f9d2a0001";

    private SaasBillingService billingService;
    private SaasBillingOrderRepository billingOrderRepository;
    private SaasSubscriptionRepository subscriptionRepository;
    private SaasBillingIdempotentGateway gateway;

    @BeforeEach
    void setUp() {
        billingService = Mockito.mock(SaasBillingService.class);
        billingOrderRepository = Mockito.mock(SaasBillingOrderRepository.class);
        subscriptionRepository = Mockito.mock(SaasSubscriptionRepository.class);
        gateway = new SaasBillingIdempotentGateway(
                billingService,
                new SaasBillingReplayService(billingOrderRepository, subscriptionRepository));
    }

    private SaasSubscription winnerSubscription() {
        SaasSubscription subscription = new SaasSubscription();
        subscription.setId(100L);
        subscription.setStoreId(STORE_ID);
        subscription.setPackageId(1L);
        subscription.setPackageName("标准版");
        subscription.setStatus(SaasSubscriptionStatus.ACTIVE);
        subscription.setStartTime(LocalDateTime.now().minusDays(1));
        subscription.setEndTime(LocalDateTime.now().plusDays(29));
        return subscription;
    }

    private SaasBillingOrder winnerOrder() {
        SaasBillingOrder order = new SaasBillingOrder();
        order.setId(55L);
        order.setStoreId(STORE_ID);
        order.setPackageId(1L);
        order.setAmount(new BigDecimal("99.00"));
        order.setIdempotencyKey(KEY);
        return order;
    }

    private void stubReplayHit(SaasSubscription winner) {
        Mockito.when(billingOrderRepository.findByStoreIdAndIdempotencyKey(STORE_ID, KEY))
                .thenReturn(Optional.of(winnerOrder()));
        Mockito.when(subscriptionRepository.findFirstByStoreIdAndStatusOrderByEndTimeDesc(
                        STORE_ID, SaasSubscriptionStatus.ACTIVE))
                .thenReturn(Optional.of(winner));
    }

    // ------------------------------------------------------------------
    // 冲突恢复为幂等重放
    // ------------------------------------------------------------------

    @Test
    void subscribe_ukConflict_replaysWinnerSubscriptionCommittedBeforeLoser() {
        SaasSubscription winner = winnerSubscription();
        Mockito.when(billingService.subscribe(STORE_ID, 1L, "user:1", KEY))
                .thenThrow(new DataIntegrityViolationException(
                        "Duplicate entry '9-6f9c2f6e' for key 'uk_saas_billing_order_idempotency'"));
        stubReplayHit(winner);

        SaasSubscription result = gateway.subscribe(STORE_ID, 1L, "user:1", KEY);

        // 败者不再 500：新事务重查到先到者已提交订单，重放其订阅
        assertSame(winner, result);
        Mockito.verify(billingOrderRepository).findByStoreIdAndIdempotencyKey(STORE_ID, KEY);
    }

    @Test
    void subscribe_unexpectedRollback_replaysWinnerSubscription() {
        SaasSubscription winner = winnerSubscription();
        Mockito.when(billingService.subscribe(STORE_ID, 1L, "user:1", KEY))
                .thenThrow(new UnexpectedRollbackException("Transaction rolled back because it has been marked as rollback-only"));
        stubReplayHit(winner);

        SaasSubscription result = gateway.subscribe(STORE_ID, 1L, "user:1", KEY);

        assertSame(winner, result);
        Mockito.verify(billingOrderRepository).findByStoreIdAndIdempotencyKey(STORE_ID, KEY);
    }

    @Test
    void subscribe_conflict_noActiveSubscription_replaysLatestSubscription() {
        SaasSubscription cancelled = winnerSubscription();
        cancelled.setStatus(SaasSubscriptionStatus.CANCELLED);
        Mockito.when(billingService.subscribe(STORE_ID, 1L, "user:1", KEY))
                .thenThrow(new DataIntegrityViolationException("Duplicate entry"));
        Mockito.when(billingOrderRepository.findByStoreIdAndIdempotencyKey(STORE_ID, KEY))
                .thenReturn(Optional.of(winnerOrder()));
        Mockito.when(subscriptionRepository.findFirstByStoreIdAndStatusOrderByEndTimeDesc(
                        STORE_ID, SaasSubscriptionStatus.ACTIVE))
                .thenReturn(Optional.empty());
        Mockito.when(subscriptionRepository.findFirstByStoreIdOrderByIdDesc(STORE_ID))
                .thenReturn(Optional.of(cancelled));

        SaasSubscription result = gateway.subscribe(STORE_ID, 1L, "user:1", KEY);

        assertSame(cancelled, result);
    }

    // ------------------------------------------------------------------
    // 冲突与幂等键无关：原样抛出（交由 handler 映射为结构化冲突响应）
    // ------------------------------------------------------------------

    @Test
    void subscribe_conflictWithoutCommittedOrder_rethrowsOriginalException() {
        DataIntegrityViolationException conflict = new DataIntegrityViolationException("some other constraint");
        Mockito.when(billingService.subscribe(STORE_ID, 1L, "user:1", KEY))
                .thenThrow(conflict);
        Mockito.when(billingOrderRepository.findByStoreIdAndIdempotencyKey(STORE_ID, KEY))
                .thenReturn(Optional.empty());

        DataIntegrityViolationException thrown = assertThrows(DataIntegrityViolationException.class,
                () -> gateway.subscribe(STORE_ID, 1L, "user:1", KEY));
        assertSame(conflict, thrown);
    }

    @Test
    void subscribe_conflictWithBlankKey_skipsReplayAndRethrows() {
        DataIntegrityViolationException conflict = new DataIntegrityViolationException("some other constraint");
        Mockito.when(billingService.subscribe(STORE_ID, 1L, "user:1", "   "))
                .thenThrow(conflict);

        DataIntegrityViolationException thrown = assertThrows(DataIntegrityViolationException.class,
                () -> gateway.subscribe(STORE_ID, 1L, "user:1", "   "));
        assertSame(conflict, thrown);
        // 空键订单不受幂等 uk 保护：不做重放查询
        Mockito.verify(billingOrderRepository, never()).findByStoreIdAndIdempotencyKey(any(), any());
    }

    // ------------------------------------------------------------------
    // 正常路径直通：不触发重放查询
    // ------------------------------------------------------------------

    @Test
    void subscribe_success_passesThroughWithoutReplayLookup() {
        SaasSubscription created = winnerSubscription();
        Mockito.when(billingService.subscribe(STORE_ID, 1L, "user:1", KEY)).thenReturn(created);

        SaasSubscription result = gateway.subscribe(STORE_ID, 1L, "user:1", KEY);

        assertSame(created, result);
        Mockito.verify(billingOrderRepository, never()).findByStoreIdAndIdempotencyKey(any(), any());
    }
}
