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

import server.demo.i18n.ApiMessages;
@RestController
@RequestMapping("/api/v1/order-box")
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
            return ApiResponse.error(ApiMessages.get("api.t.7c3d64895c08") + e.getMessage());
        }
    }

    /**
     * 移入订单盒子
     */
    @PostMapping("/move-in")
    public ApiResponse<OrderBoxDTO> moveToOrderBox(@Valid @RequestBody MoveToOrderBoxRequest request) {
        try {
            OrderBoxDTO orderBox = orderBoxService.moveToOrderBox(request);
            return ApiResponse.success(ApiMessages.get("api.t.5d60bd2dcb2b"), orderBox);
        } catch (Exception e) {
            return ApiResponse.error(ApiMessages.get("api.t.29307d3585b4") + e.getMessage());
        }
    }

    /**
     * 移出订单盒子
     */
    @PostMapping("/move-out")
    public ApiResponse<Void> moveOutOrderBox(@Valid @RequestBody MoveOutOrderBoxRequest request) {
        try {
            orderBoxService.moveOutOrderBox(request);
            return ApiResponse.success(ApiMessages.get("api.t.ebad0c72b67c"), null);
        } catch (Exception e) {
            return ApiResponse.error(ApiMessages.get("api.t.cae9c786dab8") + e.getMessage());
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
            return ApiResponse.error(ApiMessages.get("api.t.23873a5f8ea1") + e.getMessage());
        }
    }

    /**
     * 查询某订单对应的订单盒子记录；不存在时 data 为 null。
     * 订单详情页用它替代全量列表查询。
     */
    @GetMapping("/by-reservation/{reservationId}")
    public ApiResponse<OrderBoxDTO> getOrderBoxItemByReservation(@PathVariable Long reservationId) {
        try {
            OrderBoxDTO orderBox = orderBoxService.getOrderBoxItemByReservationId(reservationId);
            return ApiResponse.success(orderBox);
        } catch (Exception e) {
            return ApiResponse.error(ApiMessages.get("api.t.b8d730e7c065") + e.getMessage());
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
            return ApiResponse.error(ApiMessages.get("api.t.59c6009a14f4") + e.getMessage());
        }
    }
}
