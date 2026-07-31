package server.demo.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import server.demo.annotation.RequireFeature;
import server.demo.context.StoreContext;
import server.demo.context.StoreContextHolder;
import server.demo.entity.saas.SaasFeature;
import server.demo.enums.SaasFeatureType;
import server.demo.repository.saas.SaasFeatureRepository;
import server.demo.service.saas.EntitlementService;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * 入口层权益切面：BOOLEAN 校验；QUOTA 预扣 + 异常返还（方法内部重试不重复扣）。
 */
class RequireFeatureAspectTest {

    private static final long STORE_ID = 3L;

    private EntitlementService entitlementService;
    private SaasFeatureRepository featureRepository;
    private RequireFeatureAspect aspect;

    @BeforeEach
    void setUp() {
        StoreContextHolder.setContext(new StoreContext(1L, STORE_ID, "ADMIN"));
        entitlementService = Mockito.mock(EntitlementService.class);
        featureRepository = Mockito.mock(SaasFeatureRepository.class);
        aspect = new RequireFeatureAspect(entitlementService, featureRepository);
    }

    @AfterEach
    void tearDown() {
        StoreContextHolder.clear();
    }

    private RequireFeature annotation(String featureCode, long cost) {
        RequireFeature requireFeature = Mockito.mock(RequireFeature.class);
        Mockito.when(requireFeature.value()).thenReturn(featureCode);
        Mockito.when(requireFeature.cost()).thenReturn(cost);
        return requireFeature;
    }

    private ProceedingJoinPoint joinPoint() {
        ProceedingJoinPoint joinPoint = Mockito.mock(ProceedingJoinPoint.class);
        Signature signature = Mockito.mock(Signature.class);
        Mockito.when(signature.toShortString()).thenReturn("TestController.generate(..)");
        Mockito.when(joinPoint.getSignature()).thenReturn(signature);
        return joinPoint;
    }

    private void stubFeatureType(String featureCode, SaasFeatureType type) {
        SaasFeature feature = new SaasFeature();
        feature.setFeatureCode(featureCode);
        feature.setType(type);
        Mockito.when(featureRepository.findByFeatureCode(featureCode))
                .thenReturn(Optional.of(feature));
    }

    @Test
    void quota_success_deductsOnceBeforeProceed() throws Throwable {
        stubFeatureType("ai_website_gen", SaasFeatureType.QUOTA);
        RequireFeature requireFeature = annotation("ai_website_gen", 1);
        ProceedingJoinPoint joinPoint = joinPoint();
        Mockito.when(joinPoint.proceed()).thenReturn("ok");

        Object result = aspect.around(joinPoint, requireFeature);

        assertEquals("ok", result);
        var order = inOrder(entitlementService);
        order.verify(entitlementService).deductQuota(
                STORE_ID, "ai_website_gen", 1L, "TestController.generate(..)");
        verify(entitlementService, never()).refundQuota(
                STORE_ID, "ai_website_gen", 1L, "TestController.generate(..)");
    }

    @Test
    void quota_methodThrows_refundsAndRethrows() throws Throwable {
        stubFeatureType("ai_website_gen", SaasFeatureType.QUOTA);
        RequireFeature requireFeature = annotation("ai_website_gen", 1);
        ProceedingJoinPoint joinPoint = joinPoint();
        RuntimeException businessFailure = new RuntimeException("AI 生成失败");
        Mockito.when(joinPoint.proceed()).thenThrow(businessFailure);

        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> aspect.around(joinPoint, requireFeature));

        assertEquals(businessFailure, thrown);
        verify(entitlementService).refundQuota(
                STORE_ID, "ai_website_gen", 1L, "TestController.generate(..)");
    }

    @Test
    void boolean_checksFeatureWithoutQuotaAccounting() throws Throwable {
        stubFeatureType("independent_website", SaasFeatureType.BOOLEAN);
        RequireFeature requireFeature = annotation("independent_website", 1);
        ProceedingJoinPoint joinPoint = joinPoint();
        Mockito.when(joinPoint.proceed()).thenReturn("ok");

        aspect.around(joinPoint, requireFeature);

        verify(entitlementService).requireBooleanFeature(STORE_ID, "independent_website");
        verify(entitlementService, never()).deductQuota(
                STORE_ID, "independent_website", 1L, "TestController.generate(..)");
    }

    @Test
    void capacity_rejectedAtAnnotationUsage() {
        stubFeatureType("room_count", SaasFeatureType.CAPACITY);
        RequireFeature requireFeature = annotation("room_count", 1);

        assertThrows(IllegalStateException.class,
                () -> aspect.around(joinPoint(), requireFeature));
    }
}
