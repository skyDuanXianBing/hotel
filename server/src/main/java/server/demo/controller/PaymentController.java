package server.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import server.demo.dto.ApiResponse;
import server.demo.dto.PaymentDTO;
import server.demo.service.PaymentService;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/payments")
@CrossOrigin
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    /**
     * 创建收款记录
     */
    @PostMapping
    public ApiResponse<PaymentDTO> createPayment(@RequestBody PaymentDTO dto) {
        try {
            if (dto.getReservationId() == null) {
                return ApiResponse.error("预订ID不能为空");
            }
            if (dto.getType() == null || dto.getType().isEmpty()) {
                return ApiResponse.error("收款类型不能为空");
            }
            if (dto.getPaymentMethod() == null || dto.getPaymentMethod().isEmpty()) {
                return ApiResponse.error("支付方式不能为空");
            }
            if (dto.getAmount() == null || dto.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                return ApiResponse.error("收款金额必须大于0");
            }

            PaymentDTO result = paymentService.createPayment(dto);
            return ApiResponse.success("收款记录创建成功", result);
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.error("创建收款记录失败: " + e.getMessage());
        }
    }

    /**
     * 根据预订ID获取收款记录列表
     */
    @GetMapping("/reservation/{reservationId}")
    public ApiResponse<List<PaymentDTO>> getPaymentsByReservationId(@PathVariable Long reservationId) {
        try {
            List<PaymentDTO> payments = paymentService.getPaymentsByReservationId(reservationId);
            return ApiResponse.success("获取收款记录成功", payments);
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.error("获取收款记录失败: " + e.getMessage());
        }
    }

    /**
     * 删除收款记录
     */
    @DeleteMapping("/{id}")
    public ApiResponse<String> deletePayment(@PathVariable Long id) {
        try {
            paymentService.deletePayment(id);
            return ApiResponse.success("删除收款记录成功");
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.error("删除收款记录失败: " + e.getMessage());
        }
    }

    /**
     * 根据预订ID获取总收款金额
     */
    @GetMapping("/reservation/{reservationId}/total")
    public ApiResponse<BigDecimal> getTotalPayment(@PathVariable Long reservationId) {
        try {
            BigDecimal total = paymentService.getTotalPaymentByReservationId(reservationId);
            return ApiResponse.success("获取总收款金额成功", total);
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.error("获取总收款金额失败: " + e.getMessage());
        }
    }
}