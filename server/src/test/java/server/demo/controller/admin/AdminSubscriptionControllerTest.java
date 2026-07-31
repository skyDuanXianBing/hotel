package server.demo.controller.admin;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.UnexpectedRollbackException;
import server.demo.dto.ApiResponse;
import server.demo.dto.admin.AdminDtos.SubscriptionGrantRequest;
import server.demo.dto.admin.AdminDtos.SubscriptionView;
import server.demo.enums.SaasSubscriptionStatus;
import server.demo.interceptor.AdminAuthInterceptor;
import server.demo.service.admin.AdminSubscriptionService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;

/**
 * D2 修复：人工开通并发同幂等键败者的 uk 冲突在 Controller/Service 事务边界外捕获，
 * 经独立新事务重查先到者已成交订单并幂等重放其订阅视图；重查未命中时原样抛出原始异常。
 */
class AdminSubscriptionControllerTest {

    private AdminSubscriptionService adminSubscriptionService;
    private AdminSubscriptionController controller;
    private HttpServletRequest httpRequest;

    @BeforeEach
    void setUp() {
        adminSubscriptionService = Mockito.mock(AdminSubscriptionService.class);
        controller = new AdminSubscriptionController(adminSubscriptionService);
        httpRequest = Mockito.mock(HttpServletRequest.class);
        Mockito.when(httpRequest.getAttribute(AdminAuthInterceptor.ATTR_ADMIN_USERNAME)).thenReturn("admin");
    }

    private SubscriptionView view(Long id) {
        return new SubscriptionView(
                id, 5L, "门店五", 2L, "豪华版", BigDecimal.ZERO,
                LocalDateTime.now(), LocalDateTime.now().plusMonths(1),
                SaasSubscriptionStatus.ACTIVE, LocalDateTime.now());
    }

    @Test
    void grant_success_passesThrough() {
        SubscriptionGrantRequest request = new SubscriptionGrantRequest(5L, 2L, "客服开通", "key-1", null, null);
        SubscriptionView granted = view(20L);
        Mockito.when(adminSubscriptionService.grantSubscription(request, "admin")).thenReturn(granted);

        ApiResponse<SubscriptionView> response = controller.grantSubscription(request, httpRequest);

        assertTrue(response.isSuccess());
        assertSame(granted, response.getData());
        Mockito.verify(adminSubscriptionService, never()).findReplaySubscriptionView(any(), any());
    }

    @Test
    void grant_ukConflict_replaysWinnerSubscriptionView() {
        SubscriptionGrantRequest request = new SubscriptionGrantRequest(5L, 2L, "客服开通", "key-1", null, null);
        SubscriptionView replayed = view(20L);
        Mockito.when(adminSubscriptionService.grantSubscription(request, "admin"))
                .thenThrow(new DataIntegrityViolationException(
                        "Duplicate entry '5-key-1' for key 'uk_saas_billing_order_idempotency'"));
        Mockito.when(adminSubscriptionService.findReplaySubscriptionView(5L, "key-1"))
                .thenReturn(Optional.of(replayed));

        ApiResponse<SubscriptionView> response = controller.grantSubscription(request, httpRequest);

        // 败者不再 500：幂等重放先到者已成交订阅
        assertTrue(response.isSuccess());
        assertSame(replayed, response.getData());
        assertEquals("订阅已开通", response.getMessage());
    }

    @Test
    void grant_unexpectedRollback_replaysWinnerSubscriptionView() {
        SubscriptionGrantRequest request = new SubscriptionGrantRequest(5L, 2L, "客服开通", "key-1", null, null);
        SubscriptionView replayed = view(20L);
        Mockito.when(adminSubscriptionService.grantSubscription(request, "admin"))
                .thenThrow(new UnexpectedRollbackException("Transaction rolled back because it has been marked as rollback-only"));
        Mockito.when(adminSubscriptionService.findReplaySubscriptionView(5L, "key-1"))
                .thenReturn(Optional.of(replayed));

        ApiResponse<SubscriptionView> response = controller.grantSubscription(request, httpRequest);

        assertTrue(response.isSuccess());
        assertSame(replayed, response.getData());
    }

    @Test
    void grant_conflictWithoutCommittedOrder_rethrowsOriginalException() {
        SubscriptionGrantRequest request = new SubscriptionGrantRequest(5L, 2L, "客服开通", "key-1", null, null);
        DataIntegrityViolationException conflict = new DataIntegrityViolationException("some other constraint");
        Mockito.when(adminSubscriptionService.grantSubscription(request, "admin")).thenThrow(conflict);
        Mockito.when(adminSubscriptionService.findReplaySubscriptionView(5L, "key-1"))
                .thenReturn(Optional.empty());

        DataIntegrityViolationException thrown = assertThrows(DataIntegrityViolationException.class,
                () -> controller.grantSubscription(request, httpRequest));
        assertSame(conflict, thrown);
    }

    @Test
    void grant_conflictWithNullKey_replayMissesAndRethrows() {
        SubscriptionGrantRequest request = new SubscriptionGrantRequest(5L, 2L, "客服开通", null, null, null);
        DataIntegrityViolationException conflict = new DataIntegrityViolationException("some other constraint");
        Mockito.when(adminSubscriptionService.grantSubscription(request, "admin")).thenThrow(conflict);
        Mockito.when(adminSubscriptionService.findReplaySubscriptionView(eq(5L), eq(null)))
                .thenReturn(Optional.empty());

        DataIntegrityViolationException thrown = assertThrows(DataIntegrityViolationException.class,
                () -> controller.grantSubscription(request, httpRequest));
        assertSame(conflict, thrown);
    }
}
