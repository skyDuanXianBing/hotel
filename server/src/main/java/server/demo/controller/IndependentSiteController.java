package server.demo.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import server.demo.annotation.RequireFeature;
import server.demo.annotation.RequirePermission;
import server.demo.annotation.StoreScoped;
import server.demo.constants.SaasFeatureCodes;
import server.demo.context.StoreContext;
import server.demo.dto.ApiResponse;
import server.demo.dto.IndependentSiteDtos;
import server.demo.enums.PermissionAction;
import server.demo.enums.PermissionModule;
import server.demo.exception.PermissionDeniedException;
import server.demo.service.IndependentSiteBookingService;
import server.demo.service.IndependentSiteManagementService;
import server.demo.service.IndependentSitePageSchemaGenerationService;
import server.demo.service.IndependentSiteStripeSettingsService;
import server.demo.service.PermissionService;
import server.demo.util.StoreContextUtils;

import java.util.List;

@RestController
@RequestMapping("/api/v1/independent-sites")
@StoreScoped
@RequireFeature(SaasFeatureCodes.INDEPENDENT_WEBSITE) // 类级 BOOLEAN 模块门禁：无独立站权益的套餐整体 402
public class IndependentSiteController {

    private final IndependentSiteManagementService managementService;
    private final IndependentSitePageSchemaGenerationService pageSchemaGenerationService;
    private final IndependentSiteBookingService bookingService;
    private final IndependentSiteStripeSettingsService stripeSettingsService;
    private final PermissionService permissionService;

    public IndependentSiteController(
            IndependentSiteManagementService managementService,
            IndependentSitePageSchemaGenerationService pageSchemaGenerationService,
            IndependentSiteBookingService bookingService,
            IndependentSiteStripeSettingsService stripeSettingsService,
            PermissionService permissionService
    ) {
        this.managementService = managementService;
        this.pageSchemaGenerationService = pageSchemaGenerationService;
        this.bookingService = bookingService;
        this.stripeSettingsService = stripeSettingsService;
        this.permissionService = permissionService;
    }

    // ------------------------------------------------------------------
    // 门店 Stripe 设置（一店一套，该店所有站点共享；sk/whsec 加密落库，明文不回传）
    // ------------------------------------------------------------------

    @GetMapping("/stripe-settings")
    @RequirePermission(module = PermissionModule.CHANNEL, action = PermissionAction.VIEW_CHANNELS)
    public ResponseEntity<ApiResponse<IndependentSiteDtos.StripeSettingsResponse>> getStripeSettings() {
        return ResponseEntity.ok(ApiResponse.success(
                "获取 Stripe 设置成功",
                stripeSettingsService.getSettings(StoreContextUtils.requireStoreId())
        ));
    }

    @PutMapping("/stripe-settings")
    @RequirePermission(module = PermissionModule.CHANNEL, action = PermissionAction.MANAGE_CHANNELS)
    public ResponseEntity<ApiResponse<IndependentSiteDtos.StripeSettingsResponse>> updateStripeSettings(
            @Valid @RequestBody IndependentSiteDtos.StripeSettingsUpdateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Stripe 设置已保存",
                stripeSettingsService.updateSettings(StoreContextUtils.requireStoreId(), request)
        ));
    }

    // ------------------------------------------------------------------
    // 站点 CRUD（一店多站）
    // ------------------------------------------------------------------

    @GetMapping
    @RequirePermission(module = PermissionModule.CHANNEL, action = PermissionAction.VIEW_CHANNELS)
    public ResponseEntity<ApiResponse<List<IndependentSiteDtos.SiteSummaryResponse>>> listSites() {
        return ResponseEntity.ok(ApiResponse.success(
                "获取独立站列表成功",
                managementService.listSites(StoreContextUtils.requireStoreId())
        ));
    }

    @PostMapping
    @RequirePermission(module = PermissionModule.CHANNEL, action = PermissionAction.MANAGE_CHANNELS)
    public ResponseEntity<ApiResponse<IndependentSiteDtos.SiteDetailResponse>> createSite(
            @Valid @RequestBody IndependentSiteDtos.SiteCreateRequest request
    ) {
        IndependentSiteDtos.SiteDetailResponse response =
                managementService.createSite(StoreContextUtils.requireStoreId(), request);
        return ResponseEntity.ok(ApiResponse.success("独立站已创建", response));
    }

    @GetMapping("/{id}")
    @RequirePermission(module = PermissionModule.CHANNEL, action = PermissionAction.VIEW_CHANNELS)
    public ResponseEntity<ApiResponse<IndependentSiteDtos.SiteDetailResponse>> getSite(
            @PathVariable Long id
    ) {
        IndependentSiteDtos.SiteDetailResponse response =
                managementService.getSite(StoreContextUtils.requireStoreId(), id);
        return ResponseEntity.ok(ApiResponse.success("获取独立站详情成功", response));
    }

    @PutMapping("/{id}")
    @RequirePermission(module = PermissionModule.CHANNEL, action = PermissionAction.MANAGE_CHANNELS)
    public ResponseEntity<ApiResponse<IndependentSiteDtos.SiteDetailResponse>> updateSite(
            @PathVariable Long id,
            @Valid @RequestBody IndependentSiteDtos.SiteUpdateRequest request
    ) {
        IndependentSiteDtos.SiteDetailResponse response =
                managementService.updateSite(StoreContextUtils.requireStoreId(), id, request);
        return ResponseEntity.ok(ApiResponse.success("独立站配置已保存", response));
    }

    @DeleteMapping("/{id}")
    @RequirePermission(module = PermissionModule.CHANNEL, action = PermissionAction.MANAGE_CHANNELS)
    public ResponseEntity<ApiResponse<Void>> deleteSite(@PathVariable Long id) {
        managementService.deleteSite(StoreContextUtils.requireStoreId(), id);
        return ResponseEntity.ok(ApiResponse.success("独立站已删除", null));
    }

    // ------------------------------------------------------------------
    // 页面 CRUD（一站多页面）
    // ------------------------------------------------------------------

    @GetMapping("/{id}/pages")
    @RequirePermission(module = PermissionModule.CHANNEL, action = PermissionAction.VIEW_CHANNELS)
    public ResponseEntity<ApiResponse<List<IndependentSiteDtos.PageSummaryResponse>>> listPages(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "获取页面列表成功",
                managementService.listPages(StoreContextUtils.requireStoreId(), id)
        ));
    }

    @PostMapping("/{id}/pages")
    @RequirePermission(module = PermissionModule.CHANNEL, action = PermissionAction.MANAGE_CHANNELS)
    public ResponseEntity<ApiResponse<IndependentSiteDtos.PageDetailResponse>> createPage(
            @PathVariable Long id,
            @Valid @RequestBody IndependentSiteDtos.PageCreateRequest request
    ) {
        IndependentSiteDtos.PageDetailResponse response =
                managementService.createPage(StoreContextUtils.requireStoreId(), id, request);
        return ResponseEntity.ok(ApiResponse.success("页面已创建", response));
    }

    @GetMapping("/{id}/pages/{pageId}")
    @RequirePermission(module = PermissionModule.CHANNEL, action = PermissionAction.VIEW_CHANNELS)
    public ResponseEntity<ApiResponse<IndependentSiteDtos.PageDetailResponse>> getPage(
            @PathVariable Long id,
            @PathVariable Long pageId
    ) {
        IndependentSiteDtos.PageDetailResponse response =
                managementService.getPage(StoreContextUtils.requireStoreId(), id, pageId);
        return ResponseEntity.ok(ApiResponse.success("获取页面详情成功", response));
    }

    @PutMapping("/{id}/pages/{pageId}")
    @RequirePermission(module = PermissionModule.CHANNEL, action = PermissionAction.MANAGE_CHANNELS)
    public ResponseEntity<ApiResponse<IndependentSiteDtos.PageDetailResponse>> updatePage(
            @PathVariable Long id,
            @PathVariable Long pageId,
            @Valid @RequestBody IndependentSiteDtos.PageUpdateRequest request
    ) {
        IndependentSiteDtos.PageDetailResponse response =
                managementService.updatePage(StoreContextUtils.requireStoreId(), id, pageId, request);
        return ResponseEntity.ok(ApiResponse.success("页面已保存", response));
    }

    @DeleteMapping("/{id}/pages/{pageId}")
    @RequirePermission(module = PermissionModule.CHANNEL, action = PermissionAction.MANAGE_CHANNELS)
    public ResponseEntity<ApiResponse<Void>> deletePage(
            @PathVariable Long id,
            @PathVariable Long pageId
    ) {
        managementService.deletePage(StoreContextUtils.requireStoreId(), id, pageId);
        return ResponseEntity.ok(ApiResponse.success("页面已删除", null));
    }

    @PostMapping("/{id}/pages/generate-room-pages")
    @RequirePermission(module = PermissionModule.CHANNEL, action = PermissionAction.MANAGE_CHANNELS)
    public ResponseEntity<ApiResponse<IndependentSiteDtos.GenerateRoomPagesResponse>> generateRoomPages(
            @PathVariable Long id
    ) {
        IndependentSiteDtos.GenerateRoomPagesResponse response =
                managementService.generateRoomTypePages(StoreContextUtils.requireStoreId(), id);
        return ResponseEntity.ok(ApiResponse.success("房型页面已生成", response));
    }

    @PostMapping("/{id}/pages/import-url")
    @RequireFeature(SaasFeatureCodes.AI_WEBSITE_GEN)
    @RequirePermission(module = PermissionModule.CHANNEL, action = PermissionAction.MANAGE_CHANNELS)
    public ResponseEntity<ApiResponse<IndependentSiteDtos.PageDetailResponse>> importPageFromUrl(
            @PathVariable Long id,
            @Valid @RequestBody IndependentSiteDtos.ImportPageFromUrlRequest request
    ) {
        IndependentSiteDtos.PageDetailResponse response =
                managementService.importPageFromUrl(StoreContextUtils.requireStoreId(), id, request);
        return ResponseEntity.ok(ApiResponse.success("页面已从 URL 导入", response));
    }

    @PostMapping("/{id}/pages/{pageId}/publish")
    @RequirePermission(module = PermissionModule.CHANNEL, action = PermissionAction.MANAGE_CHANNELS)
    public ResponseEntity<ApiResponse<IndependentSiteDtos.PageDetailResponse>> publishPage(
            @PathVariable Long id,
            @PathVariable Long pageId,
            @Valid @RequestBody IndependentSiteDtos.PublishPageDraftRequest request
    ) {
        IndependentSiteDtos.PageDetailResponse response =
                managementService.publishPage(StoreContextUtils.requireStoreId(), id, pageId, request);
        return ResponseEntity.ok(ApiResponse.success("页面已发布", response));
    }

    @PostMapping("/{id}/pages/{pageId}/generate")
    @RequirePermission(module = PermissionModule.CHANNEL, action = PermissionAction.MANAGE_CHANNELS)
    @RequireFeature(SaasFeatureCodes.AI_WEBSITE_GEN)
    public ResponseEntity<ApiResponse<IndependentSiteDtos.PageDraftResponse>> generatePageDraftForPage(
            @PathVariable Long id,
            @PathVariable Long pageId,
            @Valid @RequestBody IndependentSiteDtos.PageDraftRequest request
    ) {
        IndependentSiteDtos.PageDraftResponse response = pageSchemaGenerationService.generateForPage(
                StoreContextUtils.requireStoreId(),
                id,
                pageId,
                request
        );
        return ResponseEntity.ok(ApiResponse.success("独立站页面草稿生成成功", response));
    }

    @PostMapping("/{id}/pages/{pageId}/ai-edit")
    @RequirePermission(module = PermissionModule.CHANNEL, action = PermissionAction.MANAGE_CHANNELS)
    @RequireFeature(SaasFeatureCodes.AI_WEBSITE_GEN)
    public ResponseEntity<ApiResponse<IndependentSiteDtos.PageDetailResponse>> aiEditPage(
            @PathVariable Long id,
            @PathVariable Long pageId,
            @Valid @RequestBody IndependentSiteDtos.AiEditPageRequest request
    ) {
        IndependentSiteDtos.PageDetailResponse response = pageSchemaGenerationService.aiEdit(
                StoreContextUtils.requireStoreId(),
                id,
                pageId,
                request.instruction()
        );
        return ResponseEntity.ok(ApiResponse.success("AI 修改已应用", response));
    }

    @PostMapping("/{id}/pages/{pageId}/ai-edit/undo")
    @RequirePermission(module = PermissionModule.CHANNEL, action = PermissionAction.MANAGE_CHANNELS)
    public ResponseEntity<ApiResponse<IndependentSiteDtos.PageDetailResponse>> undoAiEditPage(
            @PathVariable Long id,
            @PathVariable Long pageId
    ) {
        IndependentSiteDtos.PageDetailResponse response =
                managementService.undoAiEdit(StoreContextUtils.requireStoreId(), id, pageId);
        return ResponseEntity.ok(ApiResponse.success("已撤销最近一次 AI 修改", response));
    }

    // ------------------------------------------------------------------
    // 旧端点：默认站（该店最早创建的站点）别名，保留兼容
    // ------------------------------------------------------------------

    @Deprecated
    @GetMapping("/current")
    @RequirePermission(module = PermissionModule.CHANNEL, action = PermissionAction.VIEW_CHANNELS)
    public ResponseEntity<ApiResponse<IndependentSiteDtos.ConfigResponse>> getCurrent() {
        IndependentSiteDtos.ConfigResponse response =
                managementService.getCurrent(StoreContextUtils.requireStoreId());
        return ResponseEntity.ok(ApiResponse.success("获取独立站配置成功", response));
    }

    @Deprecated
    @PutMapping("/current")
    @RequirePermission(module = PermissionModule.CHANNEL, action = PermissionAction.MANAGE_CHANNELS)
    public ResponseEntity<ApiResponse<IndependentSiteDtos.ConfigResponse>> updateCurrent(
            @Valid @RequestBody IndependentSiteDtos.ConfigUpdateRequest request
    ) {
        IndependentSiteDtos.ConfigResponse response =
                managementService.updateCurrent(StoreContextUtils.requireStoreId(), request);
        return ResponseEntity.ok(ApiResponse.success("独立站配置已保存", response));
    }

    @Deprecated
    @PostMapping("/{id}/page-drafts/generate")
    @RequirePermission(module = PermissionModule.CHANNEL, action = PermissionAction.MANAGE_CHANNELS)
    @RequireFeature(SaasFeatureCodes.AI_WEBSITE_GEN)
    public ResponseEntity<ApiResponse<IndependentSiteDtos.PageDraftResponse>> generatePageDraft(
            @PathVariable Long id,
            @Valid @RequestBody IndependentSiteDtos.PageDraftRequest request
    ) {
        IndependentSiteDtos.PageDraftResponse response = pageSchemaGenerationService.generate(
                StoreContextUtils.requireStoreId(),
                id,
                request
        );
        return ResponseEntity.ok(ApiResponse.success("独立站页面草稿生成成功", response));
    }

    @Deprecated
    @PutMapping("/current/page-draft")
    @RequirePermission(module = PermissionModule.CHANNEL, action = PermissionAction.MANAGE_CHANNELS)
    public ResponseEntity<ApiResponse<IndependentSiteDtos.PageDraftStateResponse>> savePageDraft(
            @Valid @RequestBody IndependentSiteDtos.PageDraftSaveRequest request
    ) {
        IndependentSiteDtos.PageDraftStateResponse response = managementService.savePageDraft(
                StoreContextUtils.requireStoreId(),
                request
        );
        return ResponseEntity.ok(ApiResponse.success("独立站页面草稿已保存", response));
    }

    @Deprecated
    @PostMapping("/current/page-draft/publish")
    @RequirePermission(module = PermissionModule.CHANNEL, action = PermissionAction.MANAGE_CHANNELS)
    public ResponseEntity<ApiResponse<IndependentSiteDtos.ConfigResponse>> publishPageDraft(
            @Valid @RequestBody IndependentSiteDtos.PublishPageDraftRequest request
    ) {
        IndependentSiteDtos.ConfigResponse response = managementService.publishPageDraft(
                StoreContextUtils.requireStoreId(),
                request
        );
        return ResponseEntity.ok(ApiResponse.success("独立站页面草稿已发布", response));
    }

    @Deprecated
    @PostMapping("/current/payments/{paymentAttemptId}/simulate")
    @StoreScoped(warmupChannelPrices = false)
    @RequirePermission(module = PermissionModule.CHANNEL, action = PermissionAction.MANAGE_CHANNELS)
    public ResponseEntity<ApiResponse<IndependentSiteDtos.PaymentAttemptResponse>>
            confirmSimulatedPayment(@PathVariable String paymentAttemptId) {
        Long storeId = requireChannelManagementStoreId("您没有权限确认模拟支付");
        IndependentSiteDtos.PaymentAttemptResponse response =
                bookingService.confirmSimulatedPayment(
                        storeId,
                        paymentAttemptId
                );
        return ResponseEntity.ok(ApiResponse.success("模拟支付已确认", response));
    }

    @Deprecated
    @PostMapping("/{slug}/preview-holds")
    @StoreScoped(warmupChannelPrices = false)
    @RequirePermission(module = PermissionModule.CHANNEL, action = PermissionAction.MANAGE_CHANNELS)
    public ResponseEntity<ApiResponse<IndependentSiteDtos.PaymentAttemptResponse>> createPreviewHold(
            @PathVariable String slug,
            @Valid @RequestBody IndependentSiteDtos.HoldRequest request
    ) {
        Long storeId = requireChannelManagementStoreId("您没有权限创建管理预览订单");
        IndependentSiteDtos.PaymentAttemptResponse response = bookingService.createPreviewHold(
                storeId,
                slug,
                request
        );
        return ResponseEntity.ok(ApiResponse.success("房量已保留，等待受控支付确认", response));
    }

    private Long requireChannelManagementStoreId(String deniedMessage) {
        StoreContext context = StoreContextUtils.requireContext();
        if (context.getUserId() == null || !permissionService.hasPermission(
                context.getStoreId(),
                context.getUserId(),
                PermissionModule.CHANNEL,
                PermissionAction.MANAGE_CHANNELS
        )) {
            throw new PermissionDeniedException(deniedMessage);
        }
        return context.getStoreId();
    }
}
