package server.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import server.demo.annotation.RequirePermission;
import server.demo.annotation.StoreScoped;
import server.demo.dto.ApiResponse;
import server.demo.dto.PaymentDTO;
import server.demo.enums.PermissionAction;
import server.demo.enums.PermissionModule;
import server.demo.service.PaymentService;

import java.math.BigDecimal;
import java.util.List;

import server.demo.i18n.ApiMessages;
@RestController
@RequestMapping("/api/v1/payments")
@StoreScoped
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    /**
     * 创建收款记录
     */
    @PostMapping
    @RequirePermission(module = PermissionModule.SENSITIVE, action = PermissionAction.VIEW_FINANCIAL_DATA)
    public ApiResponse<PaymentDTO> createPayment(@RequestBody PaymentDTO dto) {
        try {
            if (dto.getReservationId() == null) {
                return ApiResponse.error(ApiMessages.get("api.t.436e6977b967"));
            }
            if (dto.getType() == null || dto.getType().isEmpty()) {
                return ApiResponse.error(ApiMessages.get("api.t.43eb4bfee277"));
            }
            if (dto.getPaymentMethod() == null || dto.getPaymentMethod().isEmpty()) {
                return ApiResponse.error(ApiMessages.get("api.t.82918ab17733"));
            }
            if (dto.getAmount() == null || dto.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                return ApiResponse.error(ApiMessages.get("api.t.68d1ad8a7cb7"));
            }

            PaymentDTO result = paymentService.createPayment(dto);
            return ApiResponse.success(ApiMessages.get("api.t.764764a4d385"), result);
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.error(ApiMessages.get("api.t.ac33d9198946") + e.getMessage());
        }
    }

    /**
     * 根据预订ID获取收款记录列表
     */
    @GetMapping("/reservation/{reservationId}")
    @RequirePermission(module = PermissionModule.SENSITIVE, action = PermissionAction.VIEW_FINANCIAL_DATA)
    public ApiResponse<List<PaymentDTO>> getPaymentsByReservationId(@PathVariable Long reservationId) {
        try {
            List<PaymentDTO> payments = paymentService.getPaymentsByReservationId(reservationId);
            return ApiResponse.success(ApiMessages.get("api.t.2c19567cebb1"), payments);
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.error(ApiMessages.get("api.t.7aea0126697e") + e.getMessage());
        }
    }

    /**
     * 删除收款记录
     */
    @DeleteMapping("/{id}")
    @RequirePermission(module = PermissionModule.SENSITIVE, action = PermissionAction.DELETE_IMPORTANT_DATA)
    public ApiResponse<String> deletePayment(@PathVariable Long id) {
        try {
            paymentService.deletePayment(id);
            return ApiResponse.success(ApiMessages.get("api.t.0ba6fc845cdf"));
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.error(ApiMessages.get("api.t.d65e19e746d8") + e.getMessage());
        }
    }

    /**
     * 根据预订ID获取总收款金额
     */
    @GetMapping("/reservation/{reservationId}/total")
    @RequirePermission(module = PermissionModule.SENSITIVE, action = PermissionAction.VIEW_FINANCIAL_DATA)
    public ApiResponse<BigDecimal> getTotalPayment(@PathVariable Long reservationId) {
        try {
            BigDecimal total = paymentService.getTotalPaymentByReservationId(reservationId);
            return ApiResponse.success(ApiMessages.get("api.t.ddcb73f6d2d8"), total);
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.error(ApiMessages.get("api.t.1e908da431b1") + e.getMessage());
        }
    }
}
