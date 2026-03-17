package server.demo.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import server.demo.annotation.RequirePermission;
import server.demo.annotation.StoreScoped;
import server.demo.dto.ApiResponse;
import server.demo.dto.PaymentMethodDTO;
import server.demo.dto.PaymentMethodOrderRequest;
import server.demo.dto.UpsertPaymentMethodRequest;
import server.demo.enums.PermissionAction;
import server.demo.enums.PermissionModule;
import server.demo.service.PaymentMethodService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/payment-methods")
@StoreScoped
public class PaymentMethodController {

    private final PaymentMethodService paymentMethodService;

    public PaymentMethodController(PaymentMethodService paymentMethodService) {
        this.paymentMethodService = paymentMethodService;
    }

    @GetMapping
    @RequirePermission(module = PermissionModule.SETTINGS, action = PermissionAction.VIEW_SETTINGS)
    public ResponseEntity<ApiResponse<List<PaymentMethodDTO>>> getAll() {
        try {
            return ResponseEntity.ok(ApiResponse.success("获取收款方式成功", paymentMethodService.getAll()));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("获取收款方式失败: " + e.getMessage()));
        }
    }

    @GetMapping("/enabled")
    @RequirePermission(module = PermissionModule.SETTINGS, action = PermissionAction.VIEW_SETTINGS)
    public ResponseEntity<ApiResponse<List<PaymentMethodDTO>>> getEnabled() {
        try {
            return ResponseEntity.ok(ApiResponse.success("获取可用收款方式成功", paymentMethodService.getEnabled()));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("获取可用收款方式失败: " + e.getMessage()));
        }
    }

    @PostMapping
    @RequirePermission(module = PermissionModule.SETTINGS, action = PermissionAction.MANAGE_PAYMENT_METHODS)
    public ResponseEntity<ApiResponse<PaymentMethodDTO>> create(
            @Valid @RequestBody UpsertPaymentMethodRequest request) {
        try {
            return ResponseEntity.ok(ApiResponse.success("创建收款方式成功", paymentMethodService.create(request)));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("创建收款方式失败: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @RequirePermission(module = PermissionModule.SETTINGS, action = PermissionAction.MANAGE_PAYMENT_METHODS)
    public ResponseEntity<ApiResponse<PaymentMethodDTO>> update(
            @PathVariable Long id,
            @Valid @RequestBody UpsertPaymentMethodRequest request) {
        try {
            return ResponseEntity.ok(ApiResponse.success("更新收款方式成功", paymentMethodService.update(id, request)));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("更新收款方式失败: " + e.getMessage()));
        }
    }

    @PatchMapping("/{id}/enabled")
    @RequirePermission(module = PermissionModule.SETTINGS, action = PermissionAction.MANAGE_PAYMENT_METHODS)
    public ResponseEntity<ApiResponse<PaymentMethodDTO>> updateEnabled(
            @PathVariable Long id,
            @RequestParam Boolean enabled) {
        try {
            return ResponseEntity.ok(ApiResponse.success("更新收款方式状态成功", paymentMethodService.updateEnabled(id, enabled)));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("更新收款方式状态失败: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @RequirePermission(module = PermissionModule.SETTINGS, action = PermissionAction.MANAGE_PAYMENT_METHODS)
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        try {
            paymentMethodService.delete(id);
            return ResponseEntity.ok(ApiResponse.success("删除收款方式成功", null));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("删除收款方式失败: " + e.getMessage()));
        }
    }

    @PutMapping("/order")
    @RequirePermission(module = PermissionModule.SETTINGS, action = PermissionAction.MANAGE_PAYMENT_METHODS)
    public ResponseEntity<ApiResponse<List<PaymentMethodDTO>>> updateOrder(
            @RequestBody List<PaymentMethodOrderRequest> requests) {
        try {
            return ResponseEntity.ok(ApiResponse.success("更新收款方式排序成功", paymentMethodService.updateOrder(requests)));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("更新收款方式排序失败: " + e.getMessage()));
        }
    }
}
