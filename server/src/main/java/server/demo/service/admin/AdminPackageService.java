package server.demo.service.admin;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.demo.dto.admin.AdminDtos.FeatureUpdateRequest;
import server.demo.dto.admin.AdminDtos.PackageFeatureItem;
import server.demo.dto.admin.AdminDtos.PackageUpsertRequest;
import server.demo.entity.saas.SaasFeature;
import server.demo.entity.saas.SaasPackage;
import server.demo.entity.saas.SaasPackageFeature;
import server.demo.enums.SaasFeatureType;
import server.demo.enums.SaasPackageStatus;
import server.demo.repository.saas.SaasFeatureRepository;
import server.demo.repository.saas.SaasPackageFeatureRepository;
import server.demo.repository.saas.SaasPackageRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import server.demo.i18n.ApiMessages;
/**
 * 平台管理端：套餐模板 / 功能字典 / 套餐权益模板维护。
 * 说明：存量订阅按成交时冻结的权益快照运行，修改模板不影响已售订阅。
 */
@Service
public class AdminPackageService {

    private final SaasPackageRepository packageRepository;
    private final SaasPackageFeatureRepository packageFeatureRepository;
    private final SaasFeatureRepository featureRepository;

    public AdminPackageService(
            SaasPackageRepository packageRepository,
            SaasPackageFeatureRepository packageFeatureRepository,
            SaasFeatureRepository featureRepository
    ) {
        this.packageRepository = packageRepository;
        this.packageFeatureRepository = packageFeatureRepository;
        this.featureRepository = featureRepository;
    }

    // ------------------------------------------------------------------
    // 套餐模板
    // ------------------------------------------------------------------

    /** 全部套餐（含已下架），按 id 升序。 */
    @Transactional(readOnly = true)
    public List<SaasPackage> listPackages() {
        return packageRepository.findAll();
    }

    @Transactional
    public SaasPackage createPackage(PackageUpsertRequest request) {
        rejectSystemFlagGrant(request);
        SaasPackage pkg = new SaasPackage();
        applyUpsert(pkg, request);
        // 新建默认下架，配好权益后再上架，避免裸套餐被购买
        pkg.setStatus(SaasPackageStatus.OFF_SHELF);
        // 系统兜底标记仅迁移/种子可置位：接口创建一律 false（实体默认已是 false，显式兜底）
        pkg.setIsSystem(false);
        return packageRepository.save(pkg);
    }

    @Transactional
    public SaasPackage updatePackage(Long id, PackageUpsertRequest request) {
        SaasPackage pkg = packageRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(ApiMessages.get("api.t.a9f29406da52") + id));
        rejectSystemFlagGrant(request);
        applyUpsert(pkg, request);
        return packageRepository.save(pkg);
    }

    /**
     * 系统兜底套餐标记（is_system）只读保护：仅 V065 迁移/种子可置位，
     * 管理端接口不得授予（true 一律拒绝）；null/false 不修改现有标记（更新不触碰该字段）。
     */
    private void rejectSystemFlagGrant(PackageUpsertRequest request) {
        if (Boolean.TRUE.equals(request.isSystem())) {
            throw new IllegalArgumentException(ApiMessages.get("api.t.3234c10a12b0"));
        }
    }

    private void applyUpsert(SaasPackage pkg, PackageUpsertRequest request) {
        if (request.version() == null || request.version() < 1) {
            throw new IllegalArgumentException(ApiMessages.get("api.t.02259e1a1993"));
        }
        pkg.setName(request.name().trim());
        pkg.setVersion(request.version());
        pkg.setPrice(request.price());
        pkg.setPeriod(request.period());
        pkg.setDescription(request.description());
    }

    /**
     * 上下架。下架不影响存量订阅（快照冻结）。
     * 系统兜底套餐（is_system=1）不可上架：它仅供订阅到期后自动回退，上架会对 C 端暴露免费无限权益。
     */
    @Transactional
    public SaasPackage updatePackageStatus(Long id, SaasPackageStatus status) {
        SaasPackage pkg = packageRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(ApiMessages.get("api.t.a9f29406da52") + id));
        if (Boolean.TRUE.equals(pkg.getIsSystem()) && status == SaasPackageStatus.ON_SHELF) {
            throw new IllegalArgumentException(ApiMessages.get("api.t.e3aae052243c"));
        }
        pkg.setStatus(status);
        return packageRepository.save(pkg);
    }

    // ------------------------------------------------------------------
    // 功能字典
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<SaasFeature> listFeatures() {
        return featureRepository.findAll();
    }

    /**
     * 名称/单位/默认重置周期/描述可改；feature_code 一经创建不可改。
     * type 修改杠杆（审查 E5）：切面按字典实时 type 分发校验，存量订阅按冻结快照 type 判定，
     * 二者分叉会把存量付费用户误判为 402——因此 feature 已被任何套餐权益引用时禁止修改 type
     * （其余字段仍可改），未被引用时可自由修改。
     */
    @Transactional
    public SaasFeature updateFeature(Long id, FeatureUpdateRequest request) {
        SaasFeature feature = featureRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(ApiMessages.get("api.t.d185eb962894") + id));
        if (request.type() != feature.getType()
                && packageFeatureRepository.existsByFeatureCode(feature.getFeatureCode())) {
            throw new IllegalArgumentException(
                    ApiMessages.get("api.t.e6a14cff3bb2") + feature.getFeatureCode() + ApiMessages.get("api.t.fd3462f662ba") + feature.getType()
                            + " → " + request.type() + ApiMessages.get("api.t.a720c3f9bede")
                            + ApiMessages.get("api.t.5c293b86b093"));
        }
        feature.setName(request.name().trim());
        feature.setType(request.type());
        feature.setUnit(request.unit());
        feature.setDefaultResetCycle(request.defaultResetCycle());
        feature.setDescription(request.description());
        return featureRepository.save(feature);
    }

    // ------------------------------------------------------------------
    // 套餐权益模板
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<SaasPackageFeature> listPackageFeatures(Long packageId) {
        requirePackage(packageId);
        return packageFeatureRepository.findByPackageId(packageId);
    }

    /**
     * 整体替换套餐权益：校验 feature_code 均在字典中、BOOLEAN 不得带额度、无重复项，
     * 然后删除旧行并写入新行。一个事务完成，避免中间态被购买。
     * 删除走批量 DML（deleteByPackageId）：单条 DELETE 立即执行、不进 ActionQueue，
     * 保证先于下方 INSERT 落库；若逐实体 deleteAll，Hibernate insert-before-delete
     * 排序会在新旧行 feature_code 重叠时撞 uk_saas_package_feature 唯一键（500）。
     */
    @Transactional
    public List<SaasPackageFeature> replacePackageFeatures(Long packageId, List<PackageFeatureItem> items) {
        requirePackage(packageId);

        Set<String> seen = new HashSet<>();
        for (PackageFeatureItem item : items) {
            if (!seen.add(item.featureCode())) {
                throw new IllegalArgumentException(ApiMessages.get("api.t.41b79f44d745") + item.featureCode());
            }
            SaasFeature feature = featureRepository.findByFeatureCode(item.featureCode())
                    .orElseThrow(() -> new IllegalArgumentException(ApiMessages.get("api.t.4a8671ebbd4b") + item.featureCode()));
            if (feature.getType() == SaasFeatureType.BOOLEAN && item.quotaLimit() != null) {
                throw new IllegalArgumentException(ApiMessages.get("api.t.109167916d7d") + item.featureCode());
            }
            if (item.quotaLimit() != null && item.quotaLimit() < 0) {
                throw new IllegalArgumentException(ApiMessages.get("api.t.cb335b5fb6ec") + item.featureCode());
            }
        }

        packageFeatureRepository.deleteByPackageId(packageId);

        List<SaasPackageFeature> rows = items.stream().map(item -> {
            SaasPackageFeature row = new SaasPackageFeature();
            row.setPackageId(packageId);
            row.setFeatureCode(item.featureCode());
            row.setQuotaLimit(item.quotaLimit());
            return row;
        }).toList();
        return packageFeatureRepository.saveAll(rows);
    }

    private SaasPackage requirePackage(Long packageId) {
        return packageRepository.findById(packageId)
                .orElseThrow(() -> new IllegalArgumentException(ApiMessages.get("api.t.a9f29406da52") + packageId));
    }
}
