package server.demo.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import server.demo.annotation.StoreScoped;
import server.demo.dto.ApiResponse;
import server.demo.dto.CreateReservationRequest;
import server.demo.dto.PagedReservationResponse;
import server.demo.dto.ReservationDTO;
import server.demo.dto.ReservationStatistics;
import server.demo.service.ReservationService;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/reservations")
@CrossOrigin
@StoreScoped
public class ReservationController extends BaseStoreController {

    @Autowired
    private ReservationService reservationService;

    @PostMapping
    public ResponseEntity<ApiResponse<ReservationDTO>> createReservation(
            @Valid @RequestBody CreateReservationRequest request) {
        try {
            ReservationDTO reservation = reservationService.createReservation(request);
            return ResponseEntity.ok(ApiResponse.success("预订创建成功", reservation));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("预订创建失败: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/check-in")
    public ResponseEntity<ApiResponse<ReservationDTO>> checkIn(@PathVariable Long id) {
        try {
            ReservationDTO reservation = reservationService.checkIn(id);
            return ResponseEntity.ok(ApiResponse.success("入住办理成功", reservation));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("入住办理失败: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/check-out")
    public ResponseEntity<ApiResponse<ReservationDTO>> checkOut(@PathVariable Long id) {
        try {
            ReservationDTO reservation = reservationService.checkOut(id);
            return ResponseEntity.ok(ApiResponse.success("退房办理成功", reservation));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("退房办理失败: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<ReservationDTO>> cancelReservation(@PathVariable Long id) {
        try {
            ReservationDTO reservation = reservationService.cancelReservation(id);
            return ResponseEntity.ok(ApiResponse.success("预订取消成功", reservation));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("预订取消失败: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ReservationDTO>> getReservation(@PathVariable Long id) {
        Optional<ReservationDTO> reservation = reservationService.getReservationById(id);
        return reservation.map(res ->
                        ResponseEntity.ok(ApiResponse.success("获取预订信息成功", res)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/order/{orderNumber}")
    public ResponseEntity<ApiResponse<ReservationDTO>> getReservationByOrderNumber(@PathVariable String orderNumber) {
        Optional<ReservationDTO> reservation = reservationService.getReservationByOrderNumber(orderNumber);
        return reservation.map(res ->
                        ResponseEntity.ok(ApiResponse.success("获取预订信息成功", res)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/date-range")
    public ResponseEntity<ApiResponse<List<ReservationDTO>>> getReservationsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<ReservationDTO> reservations = reservationService.getReservationsByDateRange(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success("获取订单列表成功", reservations));
    }

    @GetMapping("/room/{roomId}")
    public ResponseEntity<ApiResponse<List<ReservationDTO>>> getReservationsByRoomId(@PathVariable Long roomId) {
        List<ReservationDTO> reservations = reservationService.getReservationsByRoomId(roomId);
        return ResponseEntity.ok(ApiResponse.success("获取房间订单成功", reservations));
    }

    @GetMapping("/room/{roomId}/date/{date}")
    public ResponseEntity<ApiResponse<ReservationDTO>> getReservationByRoomAndDate(
            @PathVariable Long roomId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        Optional<ReservationDTO> reservation = reservationService.getReservationByRoomAndDate(roomId, date);
        return reservation.map(res ->
                        ResponseEntity.ok(ApiResponse.success("获取预订信息成功", res)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<ReservationDTO>>> searchReservations(
            @RequestParam String keyword) {
        List<ReservationDTO> reservations = reservationService.searchReservationsByGuestInfo(keyword);
        return ResponseEntity.ok(ApiResponse.success("搜索订单成功", reservations));
    }

    @GetMapping("/today/check-in")
    public ResponseEntity<ApiResponse<List<ReservationDTO>>> getTodayCheckIns() {
        List<ReservationDTO> reservations = reservationService.getTodayCheckIns();
        return ResponseEntity.ok(ApiResponse.success("获取今日入住列表成功", reservations));
    }

    @GetMapping("/today/check-out")
    public ResponseEntity<ApiResponse<List<ReservationDTO>>> getTodayCheckOuts() {
        List<ReservationDTO> reservations = reservationService.getTodayCheckOuts();
        return ResponseEntity.ok(ApiResponse.success("获取今日退房列表成功", reservations));
    }

    @PostMapping("/{id}/update")
    public ResponseEntity<ApiResponse<ReservationDTO>> updateReservation(
            @PathVariable Long id,
            @Valid @RequestBody CreateReservationRequest request) {
        try {
            ReservationDTO reservation = reservationService.updateReservation(id, request);
            return ResponseEntity.ok(ApiResponse.success("预订更新成功", reservation));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("预订更新失败: " + e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedReservationResponse>> getReservationsWithPagination(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            @RequestParam(required = false) String searchKeyword,
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) String roomType,
            @RequestParam(required = false) String checkinType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String paymentStatus,
            @RequestParam(required = false) String isPackage,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String orderType) {

        PagedReservationResponse response = reservationService.getReservationsWithFilters(
                page, size, searchKeyword, channel, roomType, checkinType,
                status, paymentStatus, isPackage, startDate, endDate, orderType);

        return ResponseEntity.ok(ApiResponse.success("获取订单列表成功", response));
    }

    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<ReservationStatistics>> getReservationStatistics() {
        ReservationStatistics statistics = reservationService.getReservationStatistics();
        return ResponseEntity.ok(ApiResponse.success("获取订单统计成功", statistics));
    }

    @GetMapping("/today/new")
    public ResponseEntity<ApiResponse<List<ReservationDTO>>> getTodayNewReservations() {
        List<ReservationDTO> reservations = reservationService.getTodayNewReservations();
        return ResponseEntity.ok(ApiResponse.success("获取今日新增订单成功", reservations));
    }

    @GetMapping("/unassigned")
    public ResponseEntity<ApiResponse<List<ReservationDTO>>> getUnassignedReservations() {
        List<ReservationDTO> reservations = reservationService.getUnassignedReservations();
        return ResponseEntity.ok(ApiResponse.success("获取未排房订单成功", reservations));
    }

    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<List<ReservationDTO>>> getPendingReservations() {
        List<ReservationDTO> reservations = reservationService.getPendingReservations();
        return ResponseEntity.ok(ApiResponse.success("获取待处理订单成功", reservations));
    }

    @GetMapping("/by-type")
    public ResponseEntity<ApiResponse<List<ReservationDTO>>> getReservationsByType(@RequestParam String type) {
        try {
            List<ReservationDTO> reservations = reservationService.getReservationsByType(type);
            return ResponseEntity.ok(ApiResponse.success("获取" + getTypeDisplayName(type) + "订单成功", reservations));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("获取订单失败: " + e.getMessage()));
        }
    }

    private String getTypeDisplayName(String type) {
        switch (type) {
            case "today-arrivals":
                return "今日预抵";
            case "today-departures":
                return "今日预离";
            case "today-new":
                return "今日新办";
            case "unassigned":
                return "未排房";
            case "pending":
                return "待处理";
            default:
                return "订单";
        }
    }
}
