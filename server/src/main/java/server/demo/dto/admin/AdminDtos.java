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
            @NotBlank(message = "用户名不能为空") String username,
            @NotBlank(message = "密码不能为空") String password
    ) {}

    public record LoginResponse(String token, String username, String role) {}

    public record ChangePasswordRequest(
            @NotBlank(message = "原密码不能为空") String oldPassword,
            @NotBlank(message = "新密码不能为空")
            @Size(min = 8, max = 64, message = "新密码长度需 8-64 位") String newPassword
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
            @NotBlank(message = "套餐名称不能为空") String name,
            @NotNull(message = "版本号不能为空") Integer version,
            @NotNull(message = "价格不能为空") @DecimalMin(value = "0", message = "价格不能为负") BigDecimal price,
            @NotNull(message = "计费周期不能为空") SaasPackagePeriod period,
            String description,
            Boolean isSystem
    ) {}

    public record PackageStatusRequest(
            @NotNull(message = "状态不能为空") SaasPackageStatus status
    ) {}

    // ------------------------------------------------------------------
    // 功能字典
    // ------------------------------------------------------------------

    /** feature_code 不可改；名称/类型/单位/默认重置周期/描述可改。 */
    public record FeatureUpdateRequest(
            @NotBlank(message = "功能名称不能为空") String name,
            @NotNull(message = "功能类型不能为空") SaasFeatureType type,
            String unit,
            SaasQuotaResetCycle defaultResetCycle,
            String description
    ) {}

    // ------------------------------------------------------------------
    // 套餐权益模板
    // ------------------------------------------------------------------

    public record PackageFeatureItem(
            @NotBlank(message = "featureCode 不能为空") String featureCode,
            Long quotaLimit
    ) {}

    /** 整体替换该套餐的权益列表。 */
    public record ReplacePackageFeaturesRequest(
            @NotEmpty(message = "权益列表不能为空") @Valid List<PackageFeatureItem> features
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
            @NotNull(message = "storeId 不能为空") Long storeId,
            @NotNull(message = "packageId 不能为空") Long packageId,
            @NotBlank(message = "remark 不能为空") @Size(max = 500, message = "remark 长度不能超过 500") String remark,
            String idempotencyKey,
            @Min(value = 1, message = "durationDays 需在 1-36500 之间")
            @Max(value = 36500, message = "durationDays 需在 1-36500 之间") Integer durationDays,
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
            @NotNull(message = "storeId 不能为空") Long storeId,
            @NotBlank(message = "featureCode 不能为空") String featureCode,
            @NotNull(message = "调整量不能为空") Long delta,
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
