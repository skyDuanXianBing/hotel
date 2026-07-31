package server.demo.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import server.demo.annotation.RequireFeature;
import server.demo.constants.SaasFeatureCodes;
import server.demo.context.StoreContext;
import server.demo.context.StoreContextHolder;
import server.demo.controller.IndependentSiteController;
import server.demo.controller.advice.IndependentSiteApiExceptionHandler;
import server.demo.controller.advice.SaasEntitlementExceptionHandler;
import server.demo.entity.saas.SaasFeature;
import server.demo.enums.SaasFeatureType;
import server.demo.repository.saas.SaasFeatureRepository;
import server.demo.service.saas.EntitlementService;

import java.lang.reflect.Method;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * SaaS 计量/门禁契约回归（QA 缺陷 D1/D2/D3 的防回归测试）：
 * D1: NeedUpgradeException 的 402 处理器必须优先于业务域 catch-all advice；
 * D2: import-url AI 端点必须带计量注解；
 * D3: IndependentSiteController 必须有类级 BOOLEAN 模块门禁，且类级切面先于方法级执行。
 */
class SaasEnforcementContractTest {

    private static final long STORE_ID = 3L;

    private EntitlementService entitlementService;
    private SaasFeatureRepository featureRepository;
    private RequireFeatureClassAspect classAspect;

    @BeforeEach
    void setUp() {
        StoreContextHolder.setContext(new StoreContext(1L, STORE_ID, "ADMIN"));
        entitlementService = Mockito.mock(EntitlementService.class);
        featureRepository = Mockito.mock(SaasFeatureRepository.class);
        classAspect = new RequireFeatureClassAspect(entitlementService, featureRepository);
    }

    @AfterEach
    void tearDown() {
        StoreContextHolder.clear();
    }

    // ---------- D1：402 契约不被 catch-all 遮蔽 ----------

    @Test
    void d1_saasEntitlementHandler_hasHighestPrecedenceOrder() {
        Order order = SaasEntitlementExceptionHandler.class.getAnnotation(Order.class);
        assertNotNull(order, "SaasEntitlementExceptionHandler 必须显式 @Order，否则会被业务域 catch-all 截胡成 500");
        assertEquals(Ordered.HIGHEST_PRECEDENCE, order.value());

        Order scopedOrder = IndependentSiteApiExceptionHandler.class.getAnnotation(Order.class);
        assertTrue(scopedOrder == null || scopedOrder.value() > Ordered.HIGHEST_PRECEDENCE,
                "业务域 catch-all advice 不得拥有更高优先级");
    }

    // ---------- D2：import-url 必须计量 ----------

    @Test
    void d2_importPageFromUrl_hasQuotaAnnotation() throws NoSuchMethodException {
        Method method = IndependentSiteController.class.getMethod(
                "importPageFromUrl", Long.class,
                server.demo.dto.IndependentSiteDtos.ImportPageFromUrlRequest.class);
        RequireFeature requireFeature = method.getAnnotation(RequireFeature.class);
        assertNotNull(requireFeature, "import-url 是 AI 生成入口，必须带 @RequireFeature 计量");
        assertEquals(SaasFeatureCodes.AI_WEBSITE_GEN, requireFeature.value());
    }

    // ---------- D3：类级模块门禁 ----------

    @Test
    void d3_independentSiteController_hasClassLevelBooleanGate() {
        RequireFeature gate = IndependentSiteController.class.getAnnotation(RequireFeature.class);
        assertNotNull(gate, "IndependentSiteController 需要类级 @RequireFeature 模块门禁");
        assertEquals(SaasFeatureCodes.INDEPENDENT_WEBSITE, gate.value());
    }

    @Test
    void d3_classAspect_runsBeforeMethodAspect() {
        Order classOrder = RequireFeatureClassAspect.class.getAnnotation(Order.class);
        Order methodOrder = RequireFeatureAspect.class.getAnnotation(Order.class);
        assertNotNull(classOrder);
        assertNotNull(methodOrder);
        assertTrue(classOrder.value() < methodOrder.value(),
                "类级模块门禁必须先于方法级配额扣减，避免无权益时先扣后返还");
    }

    @Test
    void d3_classAspect_boolean_passesAndProceeds() throws Throwable {
        SaasFeature feature = Mockito.mock(SaasFeature.class);
        Mockito.when(feature.getType()).thenReturn(SaasFeatureType.BOOLEAN);
        Mockito.when(featureRepository.findByFeatureCode(SaasFeatureCodes.INDEPENDENT_WEBSITE))
                .thenReturn(Optional.of(feature));

        ProceedingJoinPoint joinPoint = Mockito.mock(ProceedingJoinPoint.class);
        org.aspectj.lang.Signature signature = Mockito.mock(org.aspectj.lang.Signature.class);
        Mockito.when(signature.getDeclaringType()).thenReturn((Class) IndependentSiteController.class);
        Mockito.when(joinPoint.getSignature()).thenReturn(signature);
        Object expected = new Object();
        Mockito.when(joinPoint.proceed()).thenReturn(expected);

        Object result = classAspect.around(joinPoint);

        assertEquals(expected, result);
        verify(entitlementService).requireBooleanFeature(STORE_ID, SaasFeatureCodes.INDEPENDENT_WEBSITE);
        verify(entitlementService, never()).deductQuota(Mockito.anyLong(), Mockito.anyString(),
                Mockito.anyLong(), Mockito.anyString());
    }

    /** 测试用：类级错误标注 QUOTA 的哑类。 */
    @RequireFeature(SaasFeatureCodes.AI_WEBSITE_GEN)
    static class QuotaAnnotatedDummy {
        void call() {}
    }

    @Test
    void d3_classAspect_rejectsNonBooleanOnClass() throws Throwable {
        SaasFeature feature = Mockito.mock(SaasFeature.class);
        Mockito.when(feature.getType()).thenReturn(SaasFeatureType.QUOTA);
        Mockito.when(featureRepository.findByFeatureCode(SaasFeatureCodes.AI_WEBSITE_GEN))
                .thenReturn(Optional.of(feature));

        ProceedingJoinPoint joinPoint = Mockito.mock(ProceedingJoinPoint.class);
        org.aspectj.lang.Signature signature = Mockito.mock(org.aspectj.lang.Signature.class);
        Mockito.when(signature.getDeclaringType()).thenReturn((Class) QuotaAnnotatedDummy.class);
        Mockito.when(joinPoint.getSignature()).thenReturn(signature);

        assertThrows(IllegalStateException.class, () -> classAspect.around(joinPoint));
    }
}
