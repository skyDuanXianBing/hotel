package server.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import server.demo.annotation.StoreScoped;
import server.demo.dto.ApiResponse;
import server.demo.dto.ConsumptionDTO;
import server.demo.service.ConsumptionService;

import java.math.BigDecimal;
import java.util.List;

import server.demo.i18n.ApiMessages;
@RestController
@RequestMapping("/api/v1/consumptions")
@StoreScoped
public class ConsumptionController {

    @Autowired
    private ConsumptionService consumptionService;

    /**
     * 创建消费记录
     */
    @PostMapping
    public ApiResponse<ConsumptionDTO> createConsumption(@RequestBody ConsumptionDTO dto) {
        try {
            if (dto.getReservationId() == null) {
                return ApiResponse.error(ApiMessages.get("api.t.436e6977b967"));
            }
            if (dto.getItem() == null || dto.getItem().isEmpty()) {
                return ApiResponse.error(ApiMessages.get("api.t.e7b916e7db4f"));
            }
            if (dto.getAmount() == null || dto.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                return ApiResponse.error(ApiMessages.get("api.t.295a980a2a40"));
            }

            ConsumptionDTO result = consumptionService.createConsumption(dto);
            return ApiResponse.success(ApiMessages.get("api.t.79b1bc1a99bf"), result);
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.error(ApiMessages.get("api.t.36c01d4ad0fd") + e.getMessage());
        }
    }

    /**
     * 根据预订ID获取消费记录列表
     */
    @GetMapping("/reservation/{reservationId}")
    public ApiResponse<List<ConsumptionDTO>> getConsumptionsByReservationId(@PathVariable Long reservationId) {
        try {
            List<ConsumptionDTO> consumptions = consumptionService.getConsumptionsByReservationId(reservationId);
            return ApiResponse.success(ApiMessages.get("api.t.0340ed9cc5b0"), consumptions);
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.error(ApiMessages.get("api.t.124fc576e55a") + e.getMessage());
        }
    }

    /**
     * 删除消费记录
     */
    @DeleteMapping("/{id}")
    public ApiResponse<String> deleteConsumption(@PathVariable Long id) {
        try {
            consumptionService.deleteConsumption(id);
            return ApiResponse.success(ApiMessages.get("api.t.146f4326f568"));
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.error(ApiMessages.get("api.t.a42d6e528e60") + e.getMessage());
        }
    }

    /**
     * 根据预订ID获取总消费金额
     */
    @GetMapping("/reservation/{reservationId}/total")
    public ApiResponse<BigDecimal> getTotalConsumption(@PathVariable Long reservationId) {
        try {
            BigDecimal total = consumptionService.getTotalConsumptionByReservationId(reservationId);
            return ApiResponse.success(ApiMessages.get("api.t.3a1596a8fc26"), total);
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.error(ApiMessages.get("api.t.6a9b9a32eadb") + e.getMessage());
        }
    }
}
