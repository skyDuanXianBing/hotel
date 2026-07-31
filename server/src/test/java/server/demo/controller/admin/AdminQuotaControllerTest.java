package server.demo.controller.admin;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import server.demo.controller.advice.SaasEntitlementExceptionHandler;
import server.demo.dto.ApiResponse;
import server.demo.dto.admin.AdminDtos.QuotaAdjustRequest;
import server.demo.enums.SaasFeatureType;
import server.demo.exception.NeedUpgradeException;
import server.demo.service.saas.EntitlementService;
import server.demo.service.saas.EntitlementSnapshot;
import server.demo.service.saas.QuotaUsage;
import org.springframework.mock.web.MockHttpServletRequest;
import server.demo.interceptor.AdminAuthInterceptor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * P4 修复：GET /api/admin/quota/usage 只读端点。
 * 无有效订阅 / 订阅不含该 QUOTA 权益 → success=true、data=null、message 说明空态原因；
 * 权益存在 → 透传 EntitlementService.getQuotaUsage 的用量视图。
 */
class AdminQuotaControllerTest {

    private static final long STORE_ID = 7L;
    private static final String FEATURE = "ai_website_gen";

    private EntitlementService entitlementService;
    private AdminQuotaController controller;

    @BeforeEach
    void setUp() {
        entitlementService = Mockito.mock(EntitlementService.class);
        controller = new AdminQuotaController(entitlementService);
    }

    private EntitlementSnapshot snapshotWith(EntitlementSnapshot.Entry... entries) {
        return new EntitlementSnapshot(List.of(entries));
    }

    @Test
    void usage_noSubscription_returnsNullDataWithFriendlyMessage() {
        when(entitlementService.getSnapshot(STORE_ID)).thenReturn(null);

        ApiResponse<QuotaUsage> response = controller.getQuotaUsage(STORE_ID, FEATURE);

        assertTrue(response.isSuccess());
        assertNull(response.getData());
        assertEquals("该门店尚无有效订阅，暂无配额用量", response.getMessage());
    }

    @Test
    void usage_featureNotInSnapshot_returnsNullDataWithFriendlyMessage() {
        when(entitlementService.getSnapshot(STORE_ID))
                .thenReturn(snapshotWith(new EntitlementSnapshot.Entry("room_count", SaasFeatureType.CAPACITY, 10L)));

        ApiResponse<QuotaUsage> response = controller.getQuotaUsage(STORE_ID, FEATURE);

        assertTrue(response.isSuccess());
        assertNull(response.getData());
        assertEquals("当前订阅不包含该配额权益，暂无配额用量", response.getMessage());
    }

    @Test
    void usage_featureExistsButNotQuotaType_returnsNullData() {
        // BOOLEAN 权益无配额账户，与"不含该配额权益"同空态
        when(entitlementService.getSnapshot(STORE_ID))
                .thenReturn(snapshotWith(new EntitlementSnapshot.Entry(FEATURE, SaasFeatureType.BOOLEAN, null)));

        ApiResponse<QuotaUsage> response = controller.getQuotaUsage(STORE_ID, FEATURE);

        assertTrue(response.isSuccess());
        assertNull(response.getData());
    }

    @Test
    void usage_quotaFeature_returnsUsageView() {
        when(entitlementService.getSnapshot(STORE_ID))
                .thenReturn(snapshotWith(new EntitlementSnapshot.Entry(FEATURE, SaasFeatureType.QUOTA, 50L)));
        QuotaUsage usage = new QuotaUsage(FEATURE, 50L, 12L, 38L,
                LocalDateTime.now().minusDays(10), LocalDateTime.now().plusDays(20));
        when(entitlementService.getQuotaUsage(STORE_ID, FEATURE)).thenReturn(usage);

        ApiResponse<QuotaUsage> response = controller.getQuotaUsage(STORE_ID, FEATURE);

        assertTrue(response.isSuccess());
        assertEquals(50L, response.getData().totalQuota());
        assertEquals(12L, response.getData().usedQuota());
        assertEquals(38L, response.getData().remaining());
        verify(entitlementService).getQuotaUsage(STORE_ID, FEATURE);
    }

    @Test
    void adjust_delegatesWithOperatorFromRequestAttribute() {
        QuotaUsage usage = new QuotaUsage(FEATURE, 50L, 7L, 43L, null, null);
        when(entitlementService.adjustQuota(STORE_ID, FEATURE, 5L, "补偿", "root"))
                .thenReturn(usage);
        MockHttpServletRequest httpRequest = new MockHttpServletRequest();
        httpRequest.setAttribute(AdminAuthInterceptor.ATTR_ADMIN_USERNAME, "root");

        ApiResponse<QuotaUsage> response = controller.adjustQuota(
                new QuotaAdjustRequest(STORE_ID, FEATURE, 5L, "补偿"), httpRequest);

        assertTrue(response.isSuccess());
        assertEquals(43L, response.getData().remaining());
        verify(entitlementService).adjustQuota(STORE_ID, FEATURE, 5L, "补偿", "root");
    }

    // ------------------------------------------------------------------
    // P1/P10 修复：402 响应 data 携带 reason，供前端区分引导文案
    // ------------------------------------------------------------------

    @Test
    void needUpgradeHandler_writesReasonInto402Body() {
        SaasEntitlementExceptionHandler handler = new SaasEntitlementExceptionHandler();
        NeedUpgradeException exception = new NeedUpgradeException(
                FEATURE, null, null, "当前门店尚未开通套餐，请先购买套餐",
                NeedUpgradeException.Reason.NO_SUBSCRIPTION);

        ResponseEntity<ApiResponse<Object>> entity = handler.handleNeedUpgrade(exception);

        assertEquals(HttpStatus.PAYMENT_REQUIRED, entity.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) entity.getBody().getData();
        assertEquals("NO_SUBSCRIPTION", data.get("reason"));
        assertEquals(FEATURE, data.get("featureCode"));
        assertEquals("当前门店尚未开通套餐，请先购买套餐", entity.getBody().getMessage());
    }

    @Test
    void needUpgradeHandler_legacyConstructor_defaultsReasonToNotIncluded() {
        NeedUpgradeException legacy = new NeedUpgradeException(FEATURE, 5L, 5L, "额度已用尽，请升级套餐");

        assertEquals(NeedUpgradeException.Reason.NOT_INCLUDED, legacy.getReason());

        SaasEntitlementExceptionHandler handler = new SaasEntitlementExceptionHandler();
        ResponseEntity<ApiResponse<Object>> entity = handler.handleNeedUpgrade(legacy);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) entity.getBody().getData();
        assertEquals("NOT_INCLUDED", data.get("reason"));
        assertEquals(5L, data.get("limit"));
        assertEquals(5L, data.get("used"));
    }
}
