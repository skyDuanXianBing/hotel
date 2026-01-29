package server.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import server.demo.annotation.StoreScoped;
import server.demo.dto.ApiResponse;
import server.demo.dto.ConsumptionDTO;
import server.demo.service.ConsumptionService;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/consumptions")
@CrossOrigin
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
                return ApiResponse.error("预订ID不能为空");
            }
            if (dto.getItem() == null || dto.getItem().isEmpty()) {
                return ApiResponse.error("消费项目不能为空");
            }
            if (dto.getAmount() == null || dto.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                return ApiResponse.error("消费金额必须大于0");
            }

            ConsumptionDTO result = consumptionService.createConsumption(dto);
            return ApiResponse.success("消费记录创建成功", result);
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.error("创建消费记录失败: " + e.getMessage());
        }
    }

    /**
     * 根据预订ID获取消费记录列表
     */
    @GetMapping("/reservation/{reservationId}")
    public ApiResponse<List<ConsumptionDTO>> getConsumptionsByReservationId(@PathVariable Long reservationId) {
        try {
            List<ConsumptionDTO> consumptions = consumptionService.getConsumptionsByReservationId(reservationId);
            return ApiResponse.success("获取消费记录成功", consumptions);
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.error("获取消费记录失败: " + e.getMessage());
        }
    }

    /**
     * 删除消费记录
     */
    @DeleteMapping("/{id}")
    public ApiResponse<String> deleteConsumption(@PathVariable Long id) {
        try {
            consumptionService.deleteConsumption(id);
            return ApiResponse.success("删除消费记录成功");
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.error("删除消费记录失败: " + e.getMessage());
        }
    }

    /**
     * 根据预订ID获取总消费金额
     */
    @GetMapping("/reservation/{reservationId}/total")
    public ApiResponse<BigDecimal> getTotalConsumption(@PathVariable Long reservationId) {
        try {
            BigDecimal total = consumptionService.getTotalConsumptionByReservationId(reservationId);
            return ApiResponse.success("获取总消费金额成功", total);
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.error("获取总消费金额失败: " + e.getMessage());
        }
    }
}
