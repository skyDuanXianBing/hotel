package server.demo.controller;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import server.demo.annotation.StoreScoped;
import server.demo.context.StoreContextHolder;
import server.demo.dto.ApiResponse;
import server.demo.dto.RequestBookingActionRequest;
import server.demo.service.RequestBookingService;

import server.demo.i18n.ApiMessages;
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
            return ResponseEntity.ok(ApiResponse.success(ApiMessages.get("api.t.5e7f73601f90"), response));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error(ApiMessages.get("api.t.427c48be7922") + e.getMessage()));
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
            return ResponseEntity.ok(ApiResponse.success(ApiMessages.get("api.t.2d599df8ab10"), response));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error(ApiMessages.get("api.t.f9a6d96d434b") + e.getMessage()));
        }
    }
}

