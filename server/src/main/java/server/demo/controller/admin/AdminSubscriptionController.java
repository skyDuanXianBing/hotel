package server.demo.controller.admin;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import server.demo.dto.ApiResponse;
import server.demo.dto.admin.AdminDtos.PagedResponse;
import server.demo.dto.admin.AdminDtos.SubscriptionGrantRequest;
import server.demo.dto.admin.AdminDtos.SubscriptionView;
import server.demo.interceptor.AdminAuthInterceptor;
import server.demo.service.admin.AdminSubscriptionService;

/**
 * 平台管理端：租户订阅查询、人工开通/切换、人工取消。
 */
@RestController
@RequestMapping("/api/admin/subscriptions")
public class AdminSubscriptionController {

    private final AdminSubscriptionService adminSubscriptionService;

    public AdminSubscriptionController(AdminSubscriptionService adminSubscriptionService) {
        this.adminSubscriptionService = adminSubscriptionService;
    }

    @GetMapping
    public ApiResponse<PagedResponse<SubscriptionView>> listSubscriptions(
            @RequestParam(required = false) Long storeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(adminSubscriptionService.listSubscriptions(storeId, page, size));
    }

    /**
     * 人工为门店开通/切换套餐（生成 provider=DIRECT、amount=0 的人工订单）。
     * D2：并发同幂等键的败者 uk 冲突（事务已回滚）时，在本边界（事务之外）捕获，
     * 经独立新事务重查先到者已成交订单并幂等重放其订阅视图，而不是裸 500。
     */
    @PostMapping
    public ApiResponse<SubscriptionView> grantSubscription(
            @Valid @RequestBody SubscriptionGrantRequest request,
            HttpServletRequest httpRequest
    ) {
        String operator = (String) httpRequest.getAttribute(AdminAuthInterceptor.ATTR_ADMIN_USERNAME);
        SubscriptionView view;
        try {
            view = adminSubscriptionService.grantSubscription(request, operator);
        } catch (DataIntegrityViolationException | UnexpectedRollbackException conflict) {
            view = adminSubscriptionService
                    .findReplaySubscriptionView(request.storeId(), request.idempotencyKey())
                    .orElseThrow(() -> conflict);
        }
        return ApiResponse.success("订阅已开通", view);
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<SubscriptionView> cancelSubscription(
            @PathVariable Long id,
            HttpServletRequest httpRequest
    ) {
        String operator = (String) httpRequest.getAttribute(AdminAuthInterceptor.ATTR_ADMIN_USERNAME);
        return ApiResponse.success("订阅已取消",
                adminSubscriptionService.cancelSubscription(id, operator));
    }
}
