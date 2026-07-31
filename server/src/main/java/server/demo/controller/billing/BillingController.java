package server.demo.controller.billing;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import server.demo.annotation.StoreScoped;
import server.demo.dto.ApiResponse;
import server.demo.dto.saas.SaasDtos;
import server.demo.entity.saas.SaasPackage;
import server.demo.entity.saas.SaasSubscription;
import server.demo.service.saas.CapacityUsageService;
import server.demo.service.saas.EntitlementService;
import server.demo.service.saas.EntitlementSnapshot;
import server.demo.service.saas.QuotaUsage;
import server.demo.service.saas.SaasBillingIdempotentGateway;
import server.demo.service.saas.SaasBillingService;
import server.demo.util.StoreContextUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * SaaS 套餐与订阅（/api/v1/billing，@StoreScoped）：
 * 在售套餐查询、我的订阅、直连购买。认证由 /api/v1/** 的 JwtInterceptor + StoreContextInterceptor 覆盖。
 */
@RestController
@RequestMapping("/api/v1/billing")
@StoreScoped
public class BillingController {

    private final SaasBillingService billingService;
    private final SaasBillingIdempotentGateway billingGateway;
    private final EntitlementService entitlementService;
    private final CapacityUsageService capacityUsageService;

    public BillingController(SaasBillingService billingService, SaasBillingIdempotentGateway billingGateway,
                             EntitlementService entitlementService,
                             CapacityUsageService capacityUsageService) {
        this.billingService = billingService;
        this.billingGateway = billingGateway;
        this.entitlementService = entitlementService;
        this.capacityUsageService = capacityUsageService;
    }

    /**
     * 在售套餐 + 权益明细。
     */
    @GetMapping("/packages")
    public ResponseEntity<ApiResponse<List<SaasDtos.PackageView>>> listPackages() {
        List<SaasDtos.PackageView> packages = billingService.listOnShelfPackages().stream()
                .map(this::toPackageView)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("获取套餐列表成功", packages));
    }

    /**
     * 当前订阅（套餐 + 权益快照 + 各 QUOTA 用量 + 各 CAPACITY 实时用量）。无订阅时 data 为 null。
     */
    @GetMapping("/my-subscription")
    public ResponseEntity<ApiResponse<SaasDtos.SubscriptionView>> mySubscription() {
        Long storeId = StoreContextUtils.requireStoreId();
        SaasDtos.SubscriptionView view = entitlementService.findActiveSubscription(storeId)
                .map(subscription -> toSubscriptionView(storeId, subscription))
                .orElse(null);
        return ResponseEntity.ok(ApiResponse.success("获取当前订阅成功", view));
    }

    /**
     * 直连购买：生成 PAID 订单 → 激活/切换订阅（冻结权益快照）→ 初始化/叠加配额账户（单事务）。
     * idempotencyKey 由客户端生成：同 key 的重试/双击幂等重放，不产生重复订单。
     * 经幂等安全包装层调用：真并发同 key 的败者 uk 冲突时在新事务重放先到者已成交订阅（D2）。
     */
    @PostMapping("/subscribe")
    public ResponseEntity<ApiResponse<SaasDtos.SubscriptionView>> subscribe(
            @Valid @RequestBody SaasDtos.SubscribeRequest request
    ) {
        Long storeId = StoreContextUtils.requireStoreId();
        Long userId = StoreContextUtils.requireContext().getUserId();
        SaasSubscription subscription = billingGateway.subscribe(
                storeId, request.packageId(), userId != null ? "user:" + userId : null,
                request.idempotencyKey());
        return ResponseEntity.ok(ApiResponse.success("订阅成功", toSubscriptionView(storeId, subscription)));
    }

    private SaasDtos.PackageView toPackageView(SaasPackage pkg) {
        List<SaasDtos.PackageFeatureView> features = billingService.listPackageFeatures(pkg.getId()).stream()
                .map(packageFeature -> {
                    var feature = billingService.findFeature(packageFeature.getFeatureCode()).orElse(null);
                    return new SaasDtos.PackageFeatureView(
                            packageFeature.getFeatureCode(),
                            feature != null ? feature.getName() : packageFeature.getFeatureCode(),
                            feature != null ? feature.getType() : null,
                            feature != null ? feature.getUnit() : null,
                            packageFeature.getQuotaLimit()
                    );
                })
                .collect(Collectors.toList());
        return new SaasDtos.PackageView(
                pkg.getId(),
                pkg.getName(),
                pkg.getVersion(),
                pkg.getPrice(),
                pkg.getPeriod(),
                pkg.getDescription(),
                Boolean.TRUE.equals(pkg.getIsSystem()),
                features
        );
    }

    private SaasDtos.SubscriptionView toSubscriptionView(Long storeId, SaasSubscription subscription) {
        EntitlementSnapshot snapshot = entitlementService.parseSnapshot(subscription);
        boolean systemPackage = billingService.findPackage(subscription.getPackageId())
                .map(pkg -> Boolean.TRUE.equals(pkg.getIsSystem()))
                .orElse(false);
        Map<String, QuotaUsage> usageByFeature = entitlementService.listQuotaUsages(storeId).stream()
                .collect(Collectors.toMap(QuotaUsage::featureCode, u -> u));

        List<SaasDtos.EntitlementView> entitlements = snapshot.features() == null
                ? List.of()
                : snapshot.features().stream()
                        .map(entry -> new SaasDtos.EntitlementView(entry.featureCode(), entry.type(), entry.limit()))
                        .collect(Collectors.toList());

        List<SaasDtos.QuotaUsageView> quotas = usageByFeature.values().stream()
                .map(usage -> new SaasDtos.QuotaUsageView(
                        usage.featureCode(),
                        billingService.findFeature(usage.featureCode())
                                .map(feature -> feature.getName())
                                .orElse(usage.featureCode()),
                        usage.totalQuota(),
                        usage.usedQuota(),
                        usage.remaining(),
                        usage.periodStart(),
                        usage.periodEnd()
                ))
                .collect(Collectors.toList());

        List<SaasDtos.CapacityUsageView> capacityUsages = capacityUsageService
                .listCapacityUsages(storeId, snapshot).stream()
                .map(usage -> new SaasDtos.CapacityUsageView(
                        usage.featureCode(),
                        billingService.findFeature(usage.featureCode())
                                .map(feature -> feature.getName())
                                .orElse(usage.featureCode()),
                        usage.limit(),
                        usage.used()
                ))
                .collect(Collectors.toList());

        return new SaasDtos.SubscriptionView(
                subscription.getId(),
                subscription.getPackageId(),
                subscription.getPackageName(),
                subscription.getPricePaid(),
                subscription.getStartTime(),
                subscription.getEndTime(),
                subscription.getStatus(),
                systemPackage,
                entitlements,
                quotas,
                capacityUsages
        );
    }
}
