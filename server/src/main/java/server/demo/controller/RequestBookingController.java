package server.demo.controller;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import server.demo.annotation.StoreScoped;
import server.demo.context.StoreContextHolder;
import server.demo.dto.ApiResponse;
import server.demo.dto.RequestBookingActionRequest;
import server.demo.service.RequestBookingService;

/**
 * Request Booking（REQUEST 状态订单）在 PMS 内的确认/拒绝闭环
 */
@RestController
@RequestMapping("/api/v1/request-bookings")
@StoreScoped
public class RequestBookingController {

    private final RequestBookingService requestBookingService;

    public RequestBookingController(RequestBookingService requestBookingService) {
        this.requestBookingService = requestBookingService;
    }

    @PostMapping("/confirm")
    public ResponseEntity<ApiResponse<JsonNode>> confirm(@RequestBody RequestBookingActionRequest request) {
        try {
            Long storeId = StoreContextHolder.getContext().getStoreId();
            JsonNode response = requestBookingService.confirm(storeId, request.getBookingId());
            return ResponseEntity.ok(ApiResponse.success("确认请求预订成功", response));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error("确认请求预订失败: " + e.getMessage()));
        }
    }

    @PostMapping("/deny")
    public ResponseEntity<ApiResponse<JsonNode>> deny(@RequestBody RequestBookingActionRequest request) {
        try {
            Long storeId = StoreContextHolder.getContext().getStoreId();
            JsonNode response = requestBookingService.deny(
                    storeId,
                    request.getBookingId(),
                    request.getDeclineReason(),
                    request.getMessageGuest(),
                    request.getMessageAirbnb()
            );
            return ResponseEntity.ok(ApiResponse.success("拒绝/取消请求预订成功", response));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error("拒绝/取消请求预订失败: " + e.getMessage()));
        }
    }
}

