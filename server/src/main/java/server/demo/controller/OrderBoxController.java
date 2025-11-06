package server.demo.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import server.demo.dto.ApiResponse;
import server.demo.dto.MoveToOrderBoxRequest;
import server.demo.dto.MoveOutOrderBoxRequest;
import server.demo.dto.OrderBoxDTO;
import server.demo.service.OrderBoxService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/order-box")
@CrossOrigin
public class OrderBoxController {

    @Autowired
    private OrderBoxService orderBoxService;

    /**
     * 获取订单盒子列表
     */
    @GetMapping
    public ApiResponse<List<OrderBoxDTO>> getOrderBoxList() {
        try {
            List<OrderBoxDTO> orderBoxList = orderBoxService.getOrderBoxList();
            return ApiResponse.success(orderBoxList);
        } catch (Exception e) {
            return ApiResponse.error("获取订单盒子列表失败: " + e.getMessage());
        }
    }

    /**
     * 移入订单盒子
     */
    @PostMapping("/move-in")
    public ApiResponse<OrderBoxDTO> moveToOrderBox(@Valid @RequestBody MoveToOrderBoxRequest request) {
        try {
            OrderBoxDTO orderBox = orderBoxService.moveToOrderBox(request);
            return ApiResponse.success("已移入订单盒子", orderBox);
        } catch (Exception e) {
            return ApiResponse.error("移入订单盒子失败: " + e.getMessage());
        }
    }

    /**
     * 移出订单盒子
     */
    @PostMapping("/move-out")
    public ApiResponse<Void> moveOutOrderBox(@Valid @RequestBody MoveOutOrderBoxRequest request) {
        try {
            orderBoxService.moveOutOrderBox(request);
            return ApiResponse.success("已移出订单盒子", null);
        } catch (Exception e) {
            return ApiResponse.error("移出订单盒子失败: " + e.getMessage());
        }
    }

    /**
     * 获取订单盒子统计信息
     */
    @GetMapping("/statistics")
    public ApiResponse<Map<String, Object>> getOrderBoxStatistics() {
        try {
            Map<String, Object> statistics = orderBoxService.getOrderBoxStatistics();
            return ApiResponse.success(statistics);
        } catch (Exception e) {
            return ApiResponse.error("获取订单盒子统计失败: " + e.getMessage());
        }
    }

    /**
     * 检查订单是否可以移入订单盒子
     */
    @GetMapping("/check/{reservationId}")
    public ApiResponse<Map<String, Object>> checkCanMoveToOrderBox(@PathVariable Long reservationId) {
        try {
            Map<String, Object> result = orderBoxService.checkCanMoveToOrderBox(reservationId);
            return ApiResponse.success(result);
        } catch (Exception e) {
            return ApiResponse.error("检查失败: " + e.getMessage());
        }
    }
}