package server.demo.service.admin;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import server.demo.dto.admin.AdminDtos.FeatureUpdateRequest;
import server.demo.dto.admin.AdminDtos.PackageFeatureItem;
import server.demo.dto.admin.AdminDtos.PackageUpsertRequest;
import server.demo.entity.saas.SaasFeature;
import server.demo.entity.saas.SaasPackage;
import server.demo.entity.saas.SaasPackageFeature;
import server.demo.enums.SaasFeatureType;
import server.demo.enums.SaasPackagePeriod;
import server.demo.enums.SaasPackageStatus;
import server.demo.enums.SaasQuotaResetCycle;
import server.demo.repository.saas.SaasFeatureRepository;
import server.demo.repository.saas.SaasPackageFeatureRepository;
import server.demo.repository.saas.SaasPackageRepository;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;

/**
 * 平台管理端套餐模板维护：新建默认下架、编辑、上下架状态机、
 * 功能字典更新（feature_code 不可改）、套餐权益整体替换（校验失败不动旧权益）。
 */
class AdminPackageServiceTest {

    private SaasPackageRepository packageRepository;
    private SaasPackageFeatureRepository packageFeatureRepository;
    private SaasFeatureRepository featureRepository;
    private AdminPackageService service;

    @BeforeEach
    void setUp() {
        packageRepository = Mockito.mock(SaasPackageRepository.class);
        packageFeatureRepository = Mockito.mock(SaasPackageFeatureRepository.class);
        featureRepository = Mockito.mock(SaasFeatureRepository.class);
        service = new AdminPackageService(packageRepository, packageFeatureRepository, featureRepository);

        lenient().when(packageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(featureRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(packageFeatureRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private PackageUpsertRequest upsertRequest(String name, Integer version) {
        return new PackageUpsertRequest(
                name, version, new BigDecimal("199.00"), SaasPackagePeriod.YEAR, "套餐描述", null);
    }

    private PackageUpsertRequest upsertRequestWithSystemFlag(String name, Integer version, Boolean isSystem) {
        return new PackageUpsertRequest(
                name, version, new BigDecimal("199.00"), SaasPackagePeriod.YEAR, "套餐描述", isSystem);
    }

    private SaasPackage existingPackage(Long id, SaasPackageStatus status) {
        SaasPackage pkg = new SaasPackage();
        pkg.setId(id);
        pkg.setName("标准版");
        pkg.setVersion(1);
        pkg.setPrice(new BigDecimal("99.00"));
        pkg.setPeriod(SaasPackagePeriod.MONTH);
        pkg.setStatus(status);
        pkg.setDescription("old");
        return pkg;
    }

    private void stubPackage(Long id) {
        Mockito.when(packageRepository.findById(id))
                .thenReturn(Optional.of(existingPackage(id, SaasPackageStatus.ON_SHELF)));
    }

    private void stubFeature(String featureCode, SaasFeatureType type) {
        SaasFeature feature = new SaasFeature();
        feature.setFeatureCode(featureCode);
        feature.setName(featureCode + "-name");
        feature.setType(type);
        Mockito.when(featureRepository.findByFeatureCode(featureCode))
                .thenReturn(Optional.of(feature));
    }

    // ------------------------------------------------------------------
    // 套餐新建 / 编辑
    // ------------------------------------------------------------------

    @Test
    void createPackage_defaultsToOffShelf() {
        SaasPackage created = service.createPackage(upsertRequest(" 旗舰版 ", 3));

        // 新建强制下架，配好权益后再上架，避免裸套餐被购买
        assertEquals(SaasPackageStatus.OFF_SHELF, created.getStatus());
        assertEquals("旗舰版", created.getName());
        assertEquals(3, created.getVersion());
        assertEquals(0, created.getPrice().compareTo(new BigDecimal("199.00")));
        assertEquals(SaasPackagePeriod.YEAR, created.getPeriod());
        assertEquals("套餐描述", created.getDescription());
        Mockito.verify(packageRepository).save(any(SaasPackage.class));
    }

    @Test
    void createPackage_nullVersion_rejected() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> service.createPackage(upsertRequest("旗舰版", null)));
        assertTrue(e.getMessage().contains("版本号必须为正整数"));
        Mockito.verify(packageRepository, never()).save(any());
    }

    @Test
    void createPackage_nonPositiveVersion_rejected() {
        assertThrows(IllegalArgumentException.class,
                () -> service.createPackage(upsertRequest("旗舰版", 0)));
        Mockito.verify(packageRepository, never()).save(any());
    }

    @Test
    void updatePackage_appliesNamePricePeriodAndKeepsStatus() {
        SaasPackage pkg = existingPackage(1L, SaasPackageStatus.ON_SHELF);
        Mockito.when(packageRepository.findById(1L)).thenReturn(Optional.of(pkg));

        SaasPackage updated = service.updatePackage(1L, upsertRequest("标准版Pro", 2));

        assertEquals("标准版Pro", updated.getName());
        assertEquals(2, updated.getVersion());
        assertEquals(0, updated.getPrice().compareTo(new BigDecimal("199.00")));
        assertEquals(SaasPackagePeriod.YEAR, updated.getPeriod());
        assertEquals("套餐描述", updated.getDescription());
        // 编辑不触碰上下架状态（由 updatePackageStatus 独立管理）
        assertEquals(SaasPackageStatus.ON_SHELF, updated.getStatus());
        Mockito.verify(packageRepository).save(pkg);
    }

    @Test
    void updatePackage_missingPackage_rejected() {
        Mockito.when(packageRepository.findById(99L)).thenReturn(Optional.empty());

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> service.updatePackage(99L, upsertRequest("x", 1)));
        assertTrue(e.getMessage().contains("套餐不存在"));
        Mockito.verify(packageRepository, never()).save(any());
    }

    // ------------------------------------------------------------------
    // 上下架状态机
    // ------------------------------------------------------------------

    @Test
    void updatePackageStatus_switchesShelfState() {
        SaasPackage pkg = existingPackage(1L, SaasPackageStatus.OFF_SHELF);
        Mockito.when(packageRepository.findById(1L)).thenReturn(Optional.of(pkg));

        SaasPackage updated = service.updatePackageStatus(1L, SaasPackageStatus.ON_SHELF);

        assertEquals(SaasPackageStatus.ON_SHELF, updated.getStatus());
        Mockito.verify(packageRepository).save(pkg);
    }

    @Test
    void updatePackageStatus_missingPackage_rejected() {
        Mockito.when(packageRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> service.updatePackageStatus(99L, SaasPackageStatus.OFF_SHELF));
        Mockito.verify(packageRepository, never()).save(any());
    }

    // ------------------------------------------------------------------
    // 系统兜底套餐守卫（P9 / V065）：is_system=1 不可上架；标记不可经接口授予
    // ------------------------------------------------------------------

    @Test
    void updatePackageStatus_systemPackage_onShelfRejected() {
        SaasPackage pkg = existingPackage(4L, SaasPackageStatus.OFF_SHELF);
        pkg.setName("默认版");
        pkg.setIsSystem(true);
        Mockito.when(packageRepository.findById(4L)).thenReturn(Optional.of(pkg));

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> service.updatePackageStatus(4L, SaasPackageStatus.ON_SHELF));
        assertEquals("系统兜底套餐不可上架", e.getMessage());
        Mockito.verify(packageRepository, never()).save(any());
        // 状态保持下架
        assertEquals(SaasPackageStatus.OFF_SHELF, pkg.getStatus());
    }

    @Test
    void updatePackageStatus_systemPackage_offShelfAllowed() {
        SaasPackage pkg = existingPackage(4L, SaasPackageStatus.ON_SHELF);
        pkg.setIsSystem(true);
        Mockito.when(packageRepository.findById(4L)).thenReturn(Optional.of(pkg));

        SaasPackage updated = service.updatePackageStatus(4L, SaasPackageStatus.OFF_SHELF);

        assertEquals(SaasPackageStatus.OFF_SHELF, updated.getStatus());
        Mockito.verify(packageRepository).save(pkg);
    }

    @Test
    void createPackage_systemFlagTrue_rejected() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> service.createPackage(upsertRequestWithSystemFlag("伪默认版", 1, true)));
        assertTrue(e.getMessage().contains("系统兜底套餐标记不可通过接口设置"));
        Mockito.verify(packageRepository, never()).save(any());
    }

    @Test
    void createPackage_systemFlagFalseOrNull_alwaysCreatedAsNonSystem() {
        SaasPackage created = service.createPackage(upsertRequestWithSystemFlag("新版", 1, false));
        assertEquals(false, created.getIsSystem());
        Mockito.verify(packageRepository).save(any(SaasPackage.class));
    }

    @Test
    void updatePackage_systemFlagTrue_rejected() {
        SaasPackage pkg = existingPackage(1L, SaasPackageStatus.ON_SHELF);
        Mockito.when(packageRepository.findById(1L)).thenReturn(Optional.of(pkg));

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> service.updatePackage(1L, upsertRequestWithSystemFlag("标准版Pro", 2, true)));
        assertTrue(e.getMessage().contains("系统兜底套餐标记不可通过接口设置"));
        Mockito.verify(packageRepository, never()).save(any());
    }

    @Test
    void updatePackage_systemFlagOmitted_keepsExistingFlag() {
        SaasPackage pkg = existingPackage(4L, SaasPackageStatus.OFF_SHELF);
        pkg.setName("默认版");
        pkg.setIsSystem(true);
        Mockito.when(packageRepository.findById(4L)).thenReturn(Optional.of(pkg));

        // 编辑其他字段不触碰 is_system（系统标记不会被接口意外剥除）
        SaasPackage updated = service.updatePackage(4L, upsertRequest("默认版", 2));

        assertEquals(true, updated.getIsSystem());
        assertEquals("默认版", updated.getName());
        Mockito.verify(packageRepository).save(pkg);
    }

    // ------------------------------------------------------------------
    // 功能字典
    // ------------------------------------------------------------------

    @Test
    void updateFeature_updatesMutableFieldsAndKeepsFeatureCode() {
        SaasFeature feature = new SaasFeature();
        feature.setId(2L);
        feature.setFeatureCode("ai_website_gen");
        feature.setName("AI 建站");
        feature.setType(SaasFeatureType.QUOTA);
        feature.setUnit("次");
        feature.setDefaultResetCycle(SaasQuotaResetCycle.MONTHLY);
        Mockito.when(featureRepository.findById(2L)).thenReturn(Optional.of(feature));

        FeatureUpdateRequest request = new FeatureUpdateRequest(
                " AI 生成额度 ", SaasFeatureType.QUOTA, "tokens", SaasQuotaResetCycle.NONE, "按生成动作计次");
        SaasFeature updated = service.updateFeature(2L, request);

        assertEquals("AI 生成额度", updated.getName());
        assertEquals(SaasFeatureType.QUOTA, updated.getType());
        assertEquals("tokens", updated.getUnit());
        assertEquals(SaasQuotaResetCycle.NONE, updated.getDefaultResetCycle());
        assertEquals("按生成动作计次", updated.getDescription());
        // feature_code 一经创建不可改（请求体亦无该字段）
        assertEquals("ai_website_gen", updated.getFeatureCode());
        Mockito.verify(featureRepository).save(feature);
    }

    @Test
    void updateFeature_missingFeature_rejected() {
        Mockito.when(featureRepository.findById(99L)).thenReturn(Optional.empty());

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> service.updateFeature(99L, new FeatureUpdateRequest(
                        "x", SaasFeatureType.QUOTA, null, null, null)));
        assertTrue(e.getMessage().contains("功能不存在"));
        Mockito.verify(featureRepository, never()).save(any());
    }

    // ------------------------------------------------------------------
    // 功能字典 type 修改杠杆（审查 E5）：被套餐权益引用时禁止改 type
    // ------------------------------------------------------------------

    @Test
    void updateFeature_typeChange_rejectedWhenReferencedByAnyPackage() {
        SaasFeature feature = new SaasFeature();
        feature.setId(2L);
        feature.setFeatureCode("ai_website_gen");
        feature.setName("AI 建站");
        feature.setType(SaasFeatureType.QUOTA);
        Mockito.when(featureRepository.findById(2L)).thenReturn(Optional.of(feature));
        Mockito.when(packageFeatureRepository.existsByFeatureCode("ai_website_gen")).thenReturn(true);

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> service.updateFeature(2L, new FeatureUpdateRequest(
                        "AI 建站", SaasFeatureType.BOOLEAN, null, null, null)));
        assertTrue(e.getMessage().contains("禁止修改类型"));
        assertTrue(e.getMessage().contains("ai_website_gen"));
        // 拒绝后不落库，字典行保持原 type
        Mockito.verify(featureRepository, never()).save(any());
        assertEquals(SaasFeatureType.QUOTA, feature.getType());
    }

    @Test
    void updateFeature_typeChange_allowedWhenNotReferenced() {
        SaasFeature feature = new SaasFeature();
        feature.setId(2L);
        feature.setFeatureCode("ai_website_gen");
        feature.setName("AI 建站");
        feature.setType(SaasFeatureType.QUOTA);
        Mockito.when(featureRepository.findById(2L)).thenReturn(Optional.of(feature));
        Mockito.when(packageFeatureRepository.existsByFeatureCode("ai_website_gen")).thenReturn(false);

        SaasFeature updated = service.updateFeature(2L, new FeatureUpdateRequest(
                "AI 建站", SaasFeatureType.BOOLEAN, null, null, null));

        assertEquals(SaasFeatureType.BOOLEAN, updated.getType());
        Mockito.verify(featureRepository).save(feature);
    }

    @Test
    void updateFeature_sameType_otherFieldsMutableEvenWhenReferenced() {
        SaasFeature feature = new SaasFeature();
        feature.setId(2L);
        feature.setFeatureCode("ai_website_gen");
        feature.setName("AI 建站");
        feature.setType(SaasFeatureType.QUOTA);
        feature.setUnit("次");
        Mockito.when(featureRepository.findById(2L)).thenReturn(Optional.of(feature));
        Mockito.when(packageFeatureRepository.existsByFeatureCode("ai_website_gen")).thenReturn(true);

        // type 未变（QUOTA→QUOTA）：name/unit/description/resetCycle 仍可改
        SaasFeature updated = service.updateFeature(2L, new FeatureUpdateRequest(
                "AI 生成", SaasFeatureType.QUOTA, "tokens", SaasQuotaResetCycle.NONE, "按次计费"));

        assertEquals("AI 生成", updated.getName());
        assertEquals("tokens", updated.getUnit());
        assertEquals(SaasQuotaResetCycle.NONE, updated.getDefaultResetCycle());
        assertEquals("按次计费", updated.getDescription());
        Mockito.verify(featureRepository).save(feature);
    }

    // ------------------------------------------------------------------
    // 套餐权益整体替换
    // ------------------------------------------------------------------

    @Test
    void replacePackageFeatures_success_deletesOldRowsAndInsertsNew() {
        stubPackage(1L);
        stubFeature("room_count", SaasFeatureType.CAPACITY);
        stubFeature("independent_website", SaasFeatureType.BOOLEAN);

        List<SaasPackageFeature> saved = service.replacePackageFeatures(1L, List.of(
                new PackageFeatureItem("room_count", 10L),
                new PackageFeatureItem("independent_website", null)));

        // 旧权益行按 packageId 批量删除（bulk DML，立即执行）
        Mockito.verify(packageFeatureRepository).deleteByPackageId(1L);

        // 新权益行写入：packageId/featureCode/quotaLimit 完整
        assertEquals(2, saved.size());
        SaasPackageFeature roomCount = saved.get(0);
        assertEquals(1L, roomCount.getPackageId());
        assertEquals("room_count", roomCount.getFeatureCode());
        assertEquals(10L, roomCount.getQuotaLimit());
        SaasPackageFeature website = saved.get(1);
        assertEquals("independent_website", website.getFeatureCode());
        assertNull(website.getQuotaLimit());
    }

    /**
     * 回归（生产 500）：uk_saas_package_feature(package_id, feature_code) 下，
     * 同事务逐实体 deleteAll 会被 ActionQueue 排到 INSERT 之后执行，新旧行
     * feature_code 重叠即撞唯一键。修复要求：批量删除且删除先于 saveAll。
     */
    @Test
    void replacePackageFeatures_bulkDeleteRunsBeforeSaveAll() {
        stubPackage(1L);
        stubFeature("ai_website_gen", SaasFeatureType.QUOTA);

        service.replacePackageFeatures(1L, List.of(new PackageFeatureItem("ai_website_gen", 5L)));

        InOrder order = inOrder(packageFeatureRepository);
        order.verify(packageFeatureRepository).deleteByPackageId(1L);
        order.verify(packageFeatureRepository).saveAll(anyList());
        // 不得回退为逐实体删除（deleteAll 会进 ActionQueue，重新引入撞键风险）
        Mockito.verify(packageFeatureRepository, never()).deleteAll(anyList());
    }

    /**
     * 仓储层静态审查：deleteByPackageId 必须是 @Modifying 的 JPQL 批量 DELETE
     * （而非派生 delete 查询——派生 deleteByXxx 会先 SELECT 再逐实体 remove，
     * 同样受 ActionQueue insert-before-delete 排序影响）。
     */
    @Test
    void repository_deleteByPackageId_isBulkModifyingDelete() throws Exception {
        Method method = SaasPackageFeatureRepository.class
                .getMethod("deleteByPackageId", Long.class);

        Modifying modifying = method.getAnnotation(Modifying.class);
        assertNotNull(modifying, "deleteByPackageId 必须是 @Modifying 批量 DML，否则删除会进 ActionQueue");
        assertTrue(modifying.clearAutomatically(), "批量删除后应清空持久化上下文，避免残留旧权益实体");
        assertTrue(modifying.flushAutomatically(), "批量删除前应先 flush，保证删除先于后续 INSERT 落库");

        Query query = method.getAnnotation(Query.class);
        assertNotNull(query, "deleteByPackageId 必须显式声明 JPQL DELETE，而非派生查询");
        String jpql = query.value().replaceAll("\\s+", " ").toUpperCase();
        assertTrue(jpql.startsWith("DELETE FROM SAASPACKAGEFEATURE"),
                "JPQL 必须是批量 DELETE，实际: " + query.value());
        assertTrue(jpql.contains("PACKAGEID"), "JPQL 必须按 packageId 过滤，实际: " + query.value());
    }

    @Test
    void replacePackageFeatures_booleanWithQuotaLimit_rejected() {
        stubPackage(1L);
        stubFeature("independent_website", SaasFeatureType.BOOLEAN);

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> service.replacePackageFeatures(1L,
                        List.of(new PackageFeatureItem("independent_website", 10L))));
        assertTrue(e.getMessage().contains("BOOLEAN 权益不可设置额度"));
        Mockito.verify(packageFeatureRepository, never()).deleteByPackageId(anyLong());
        Mockito.verify(packageFeatureRepository, never()).saveAll(anyList());
    }

    @Test
    void replacePackageFeatures_negativeQuotaLimitOnQuotaFeature_rejected() {
        stubPackage(1L);
        stubFeature("ai_website_gen", SaasFeatureType.QUOTA);

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> service.replacePackageFeatures(1L,
                        List.of(new PackageFeatureItem("ai_website_gen", -1L))));
        assertTrue(e.getMessage().contains("额度不能为负"));
        Mockito.verify(packageFeatureRepository, never()).saveAll(anyList());
    }

    @Test
    void replacePackageFeatures_negativeQuotaLimitOnCapacityFeature_rejected() {
        stubPackage(1L);
        stubFeature("room_count", SaasFeatureType.CAPACITY);

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> service.replacePackageFeatures(1L,
                        List.of(new PackageFeatureItem("room_count", -5L))));
        assertTrue(e.getMessage().contains("额度不能为负"));
        Mockito.verify(packageFeatureRepository, never()).saveAll(anyList());
    }

    @Test
    void replacePackageFeatures_unknownFeatureCode_rejected() {
        stubPackage(1L);
        Mockito.when(featureRepository.findByFeatureCode("ghost_feature"))
                .thenReturn(Optional.empty());

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> service.replacePackageFeatures(1L,
                        List.of(new PackageFeatureItem("ghost_feature", 1L))));
        assertTrue(e.getMessage().contains("功能字典缺失"));
        Mockito.verify(packageFeatureRepository, never()).saveAll(anyList());
    }

    @Test
    void replacePackageFeatures_duplicateFeatureCode_rejected() {
        stubPackage(1L);
        stubFeature("ai_website_gen", SaasFeatureType.QUOTA);

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> service.replacePackageFeatures(1L, List.of(
                        new PackageFeatureItem("ai_website_gen", 5L),
                        new PackageFeatureItem("ai_website_gen", 10L))));
        assertTrue(e.getMessage().contains("重复 featureCode"));
        Mockito.verify(packageFeatureRepository, never()).saveAll(anyList());
    }

    @Test
    void replacePackageFeatures_anyItemInvalid_keepsOldFeaturesUntouched() {
        stubPackage(1L);
        stubFeature("room_count", SaasFeatureType.CAPACITY);
        Mockito.when(featureRepository.findByFeatureCode("ghost_feature"))
                .thenReturn(Optional.empty());

        // 第一项合法、第二项字典缺失 → 整体校验失败
        assertThrows(IllegalArgumentException.class,
                () -> service.replacePackageFeatures(1L, List.of(
                        new PackageFeatureItem("room_count", 10L),
                        new PackageFeatureItem("ghost_feature", 1L))));

        // 全部校验先于删旧插新：失败时旧权益一行不动（"校验失败整体回滚"的单元级表达，
        // 单事务由 service 层 @Transactional 保证）
        Mockito.verify(packageFeatureRepository, never()).findByPackageId(any());
        Mockito.verify(packageFeatureRepository, never()).deleteByPackageId(anyLong());
        Mockito.verify(packageFeatureRepository, never()).saveAll(anyList());
    }

    @Test
    void listPackageFeatures_missingPackage_rejected() {
        Mockito.when(packageRepository.findById(99L)).thenReturn(Optional.empty());

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> service.listPackageFeatures(99L));
        assertTrue(e.getMessage().contains("套餐不存在"));
    }
}
