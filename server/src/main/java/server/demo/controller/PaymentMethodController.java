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

import server.demo.i18n.ApiMessages;
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
            return ResponseEntity.ok(ApiResponse.success(ApiMessages.get("api.t.acb4a61677db"), paymentMethodService.getAll()));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(ApiMessages.get("api.t.584292ea54f8") + e.getMessage()));
        }
    }

    @GetMapping("/enabled")
    @RequirePermission(module = PermissionModule.SETTINGS, action = PermissionAction.VIEW_SETTINGS)
    public ResponseEntity<ApiResponse<List<PaymentMethodDTO>>> getEnabled() {
        try {
            return ResponseEntity.ok(ApiResponse.success(ApiMessages.get("api.t.83b7bc5a0752"), paymentMethodService.getEnabled()));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(ApiMessages.get("api.t.a3e7733950af") + e.getMessage()));
        }
    }

    @PostMapping
    @RequirePermission(module = PermissionModule.SETTINGS, action = PermissionAction.MANAGE_PAYMENT_METHODS)
    public ResponseEntity<ApiResponse<PaymentMethodDTO>> create(
            @Valid @RequestBody UpsertPaymentMethodRequest request) {
        try {
            return ResponseEntity.ok(ApiResponse.success(ApiMessages.get("api.t.09d8d063687e"), paymentMethodService.create(request)));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(ApiMessages.get("api.t.71d998480b38") + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @RequirePermission(module = PermissionModule.SETTINGS, action = PermissionAction.MANAGE_PAYMENT_METHODS)
    public ResponseEntity<ApiResponse<PaymentMethodDTO>> update(
            @PathVariable Long id,
            @Valid @RequestBody UpsertPaymentMethodRequest request) {
        try {
            return ResponseEntity.ok(ApiResponse.success(ApiMessages.get("api.t.2c7dae6b22b9"), paymentMethodService.update(id, request)));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(ApiMessages.get("api.t.eb82ac238d96") + e.getMessage()));
        }
    }

    @PatchMapping("/{id}/enabled")
    @RequirePermission(module = PermissionModule.SETTINGS, action = PermissionAction.MANAGE_PAYMENT_METHODS)
    public ResponseEntity<ApiResponse<PaymentMethodDTO>> updateEnabled(
            @PathVariable Long id,
            @RequestParam Boolean enabled) {
        try {
            return ResponseEntity.ok(ApiResponse.success(ApiMessages.get("api.t.db5fe6a8e243"), paymentMethodService.updateEnabled(id, enabled)));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(ApiMessages.get("api.t.12655db5c3d4") + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @RequirePermission(module = PermissionModule.SETTINGS, action = PermissionAction.MANAGE_PAYMENT_METHODS)
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        try {
            paymentMethodService.delete(id);
            return ResponseEntity.ok(ApiResponse.success(ApiMessages.get("api.t.46e5ad870d1e"), null));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(ApiMessages.get("api.t.00ccfff93367") + e.getMessage()));
        }
    }

    @PutMapping("/order")
    @RequirePermission(module = PermissionModule.SETTINGS, action = PermissionAction.MANAGE_PAYMENT_METHODS)
    public ResponseEntity<ApiResponse<List<PaymentMethodDTO>>> updateOrder(
            @RequestBody List<PaymentMethodOrderRequest> requests) {
        try {
            return ResponseEntity.ok(ApiResponse.success(ApiMessages.get("api.t.3cfe4df1f003"), paymentMethodService.updateOrder(requests)));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(ApiMessages.get("api.t.ac6bfb631f39") + e.getMessage()));
        }
    }
}
