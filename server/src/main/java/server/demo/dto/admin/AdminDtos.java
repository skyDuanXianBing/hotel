package server.demo.dto.admin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import server.demo.enums.SaasFeatureType;
import server.demo.enums.SaasPackagePeriod;
import server.demo.enums.SaasPackageStatus;
import server.demo.enums.SaasQuotaResetCycle;
import server.demo.enums.SaasSubscriptionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 平台管理端（/api/admin/**）请求/响应 DTO。
 */
public final class AdminDtos {

    private AdminDtos() {}

    // ------------------------------------------------------------------
    // 认证
    // ------------------------------------------------------------------

    public record LoginRequest(
            @NotBlank(message = "{api.t.ecb38cb09941}") String username,
            @NotBlank(message = "{api.t.4d81424b0110}") String password
    ) {}

    public record LoginResponse(String token, String username, String role) {}

    public record ChangePasswordRequest(
            @NotBlank(message = "{api.t.f224d0afa6a2}") String oldPassword,
            @NotBlank(message = "{api.t.689e5a9b3225}")
            @Size(min = 8, max = 64, message = "{api.t.e66bc11e2f96}") String newPassword
    ) {}

    // ------------------------------------------------------------------
    // 套餐管理
    // ------------------------------------------------------------------

    /**
     * 套餐新建/编辑。version 语义：改价建议新建更高 version 的套餐行并下架旧行；
     * 管理端也允许直接编辑 ON_SHELF 套餐价格（存量订阅按成交快照不受影响）。
     * isSystem：系统兜底套餐标记只读保护——传 true 一律 400 拒绝（该标记仅迁移/种子可置位），
     * null/false 不修改现有标记（新建一律 false）。
     */
    public record PackageUpsertRequest(
            @NotBlank(message = "{api.t.be190a411bbe}") String name,
            @NotNull(message = "{api.t.294d24f83c6a}") Integer version,
            @NotNull(message = "{api.t.65b3b94588a8}") @DecimalMin(value = "0", message = "{api.t.a1c1f3e89bce}") BigDecimal price,
            @NotNull(message = "{api.t.e99866430e4a}") SaasPackagePeriod period,
            String description,
            Boolean isSystem
    ) {}

    public record PackageStatusRequest(
            @NotNull(message = "{api.t.1318b551d6ba}") SaasPackageStatus status
    ) {}

    // ------------------------------------------------------------------
    // 功能字典
    // ------------------------------------------------------------------

    /** feature_code 不可改；名称/类型/单位/默认重置周期/描述可改。 */
    public record FeatureUpdateRequest(
            @NotBlank(message = "{api.t.a389c3a101ec}") String name,
            @NotNull(message = "{api.t.a1f77aa8b05d}") SaasFeatureType type,
            String unit,
            SaasQuotaResetCycle defaultResetCycle,
            String description
    ) {}

    // ------------------------------------------------------------------
    // 套餐权益模板
    // ------------------------------------------------------------------

    public record PackageFeatureItem(
            @NotBlank(message = "{api.t.74942f485d10}") String featureCode,
            Long quotaLimit
    ) {}

    /** 整体替换该套餐的权益列表。 */
    public record ReplacePackageFeaturesRequest(
            @NotEmpty(message = "{api.t.50cca27595c7}") @Valid List<PackageFeatureItem> features
    ) {}

    // ------------------------------------------------------------------
    // 租户订阅
    // ------------------------------------------------------------------

    /**
     * 人工为门店开通/切换套餐（生成 provider=DIRECT、amount=0 的人工订单）。
     * durationDays：可空——null 按套餐周期（同套餐重购走续费顺延）；1..36500 时 endTime=now+该天数。
     * permanent：true 时 endTime=2099-12-31 23:59:59（与 durationDays 互斥）。
     * remark：必填（≤500），与操作人一起写入订单 remark 备审计。
     */
    public record SubscriptionGrantRequest(
            @NotNull(message = "{api.t.d7f09cedd0e8}") Long storeId,
            @NotNull(message = "{api.t.942b012cedd1}") Long packageId,
            @NotBlank(message = "{api.t.42f14a7bfa66}") @Size(max = 500, message = "{api.t.0b0e998b329b}") String remark,
            String idempotencyKey,
            @Min(value = 1, message = "{api.t.5d7af526b112}")
            @Max(value = 36500, message = "{api.t.5d7af526b112}") Integer durationDays,
            Boolean permanent
    ) {}

    public record SubscriptionView(
            Long id,
            Long storeId,
            String storeName,
            Long packageId,
            String packageName,
            BigDecimal pricePaid,
            LocalDateTime startTime,
            LocalDateTime endTime,
            SaasSubscriptionStatus status,
            LocalDateTime createdAt
    ) {}

    public record PagedResponse<T>(
            List<T> content,
            long totalElements,
            int totalPages,
            int page,
            int size
    ) {}

    // ------------------------------------------------------------------
    // 门店搜索（管理端门店选择器）
    // ------------------------------------------------------------------

    /** 门店搜索命中项：id + name，前端展示为“名称 (#id)”。 */
    public record StoreSearchItem(Long id, String name) {}

    // ------------------------------------------------------------------
    // 配额调整
    // ------------------------------------------------------------------

    public record QuotaAdjustRequest(
            @NotNull(message = "{api.t.d7f09cedd0e8}") Long storeId,
            @NotBlank(message = "{api.t.74942f485d10}") String featureCode,
            @NotNull(message = "{api.t.355bfe03f59c}") Long delta,
            String remark
    ) {}

    // ------------------------------------------------------------------
    // 概览
    // ------------------------------------------------------------------

    public record PackageSubscriptionCount(String packageName, long count) {}

    public record DashboardResponse(
            long totalStores,
            long activeSubscriptions,
            List<PackageSubscriptionCount> packageSubscriptionCounts,
            BigDecimal last30DaysOrderAmount,
            Long aiQuotaUsedTotal
    ) {}
}
