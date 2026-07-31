package server.demo.controller.admin;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import server.demo.dto.ApiResponse;
import server.demo.dto.admin.AdminDtos.QuotaAdjustRequest;
import server.demo.enums.SaasFeatureType;
import server.demo.interceptor.AdminAuthInterceptor;
import server.demo.service.saas.EntitlementService;
import server.demo.service.saas.EntitlementSnapshot;
import server.demo.service.saas.QuotaUsage;

import server.demo.i18n.ApiMessages;
/**
 * 平台管理端：配额人工调整（写 ADJUST 流水，operator = 管理员账号）与配额用量只读查询。
 */
@RestController
@RequestMapping("/api/admin/quota")
public class AdminQuotaController {

    private final EntitlementService entitlementService;

    public AdminQuotaController(EntitlementService entitlementService) {
        this.entitlementService = entitlementService;
    }

    /**
     * 当前配额用量只读查询（管理端配额调整页"调整前先看用量"）。
     * 门店无有效订阅、或订阅不含该 QUOTA 权益时 success=true 但 data=null，
     * message 说明空态原因，由前端展示友好空态而非报错。
     */
    @GetMapping("/usage")
    public ApiResponse<QuotaUsage> getQuotaUsage(
            @RequestParam Long storeId,
            @RequestParam String featureCode
    ) {
        EntitlementSnapshot snapshot = entitlementService.getSnapshot(storeId);
        if (snapshot == null) {
            return ApiResponse.success(ApiMessages.get("api.t.c8e441bed2c5"), null);
        }
        EntitlementSnapshot.Entry entry = snapshot.find(featureCode);
        if (entry == null || entry.type() != SaasFeatureType.QUOTA) {
            return ApiResponse.success(ApiMessages.get("api.t.b3b318f98033"), null);
        }
        return ApiResponse.success(entitlementService.getQuotaUsage(storeId, featureCode));
    }

    @PostMapping("/adjust")
    public ApiResponse<QuotaUsage> adjustQuota(
            @Valid @RequestBody QuotaAdjustRequest request,
            HttpServletRequest httpRequest
    ) {
        String operator = (String) httpRequest.getAttribute(AdminAuthInterceptor.ATTR_ADMIN_USERNAME);
        QuotaUsage usage = entitlementService.adjustQuota(
                request.storeId(),
                request.featureCode(),
                request.delta(),
                request.remark(),
                operator);
        return ApiResponse.success(ApiMessages.get("api.t.49d3750c12e6"), usage);
    }
}
