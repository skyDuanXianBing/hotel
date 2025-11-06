package server.demo.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
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
public class ReservationController {

    @Autowired
    private ReservationService reservationService;

    @PostMapping
    public ResponseEntity<ApiResponse<ReservationDTO>> createReservation(@Valid @RequestBody CreateReservationRequest request, HttpServletRequest httpRequest) {
        try {
            Long userId = (Long) httpRequest.getAttribute("userId");
            ReservationDTO reservation = reservationService.createReservation(userId, request);
            return ResponseEntity.ok(ApiResponse.success("预订创建成功", reservation));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("预订创建失败: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/check-in")
    public ResponseEntity<ApiResponse<ReservationDTO>> checkIn(@PathVariable Long id, HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            ReservationDTO reservation = reservationService.checkIn(userId, id);
            return ResponseEntity.ok(ApiResponse.success("入住办理成功", reservation));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("入住办理失败: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/check-out")
    public ResponseEntity<ApiResponse<ReservationDTO>> checkOut(@PathVariable Long id, HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            ReservationDTO reservation = reservationService.checkOut(userId, id);
            return ResponseEntity.ok(ApiResponse.success("退房办理成功", reservation));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("退房办理失败: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<ReservationDTO>> cancelReservation(@PathVariable Long id, HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            ReservationDTO reservation = reservationService.cancelReservation(userId, id);
            return ResponseEntity.ok(ApiResponse.success("预订取消成功", reservation));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("预订取消失败: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ReservationDTO>> getReservation(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Optional<ReservationDTO> reservation = reservationService.getReservationById(userId, id);
        if (reservation.isPresent()) {
            return ResponseEntity.ok(ApiResponse.success("获取预订信息成功", reservation.get()));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/order/{orderNumber}")
    public ResponseEntity<ApiResponse<ReservationDTO>> getReservationByOrderNumber(@PathVariable String orderNumber, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Optional<ReservationDTO> reservation = reservationService.getReservationByOrderNumber(userId, orderNumber);
        if (reservation.isPresent()) {
            return ResponseEntity.ok(ApiResponse.success("获取预订信息成功", reservation.get()));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/date-range")
    public ResponseEntity<ApiResponse<List<ReservationDTO>>> getReservationsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        List<ReservationDTO> reservations = reservationService.getReservationsByDateRange(userId, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success("获取预订列表成功", reservations));
    }

    @GetMapping("/room/{roomId}")
    public ResponseEntity<ApiResponse<List<ReservationDTO>>> getReservationsByRoom(@PathVariable Long roomId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        List<ReservationDTO> reservations = reservationService.getReservationsByRoomId(userId, roomId);
        return ResponseEntity.ok(ApiResponse.success("获取预订列表成功", reservations));
    }

    @GetMapping("/room/{roomId}/date/{date}")
    public ResponseEntity<ApiResponse<ReservationDTO>> getReservationByRoomAndDate(
            @PathVariable Long roomId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Optional<ReservationDTO> reservation = reservationService.getReservationByRoomAndDate(userId, roomId, date);
        if (reservation.isPresent()) {
            return ResponseEntity.ok(ApiResponse.success("获取预订信息成功", reservation.get()));
        } else {
            return ResponseEntity.ok(ApiResponse.success("该房间在指定日期无预订", null));
        }
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<ReservationDTO>>> searchReservations(@RequestParam String keyword, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        List<ReservationDTO> reservations = reservationService.searchReservationsByGuestInfo(userId, keyword);
        return ResponseEntity.ok(ApiResponse.success("搜索完成", reservations));
    }

    @GetMapping("/today/check-ins")
    public ResponseEntity<ApiResponse<List<ReservationDTO>>> getTodayCheckIns(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        List<ReservationDTO> reservations = reservationService.getTodayCheckIns(userId);
        return ResponseEntity.ok(ApiResponse.success("获取今日入住列表成功", reservations));
    }

    @GetMapping("/today/check-outs")
    public ResponseEntity<ApiResponse<List<ReservationDTO>>> getTodayCheckOuts(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        List<ReservationDTO> reservations = reservationService.getTodayCheckOuts(userId);
        return ResponseEntity.ok(ApiResponse.success("获取今日退房列表成功", reservations));
    }

    @PostMapping("/{id}/update")
    public ResponseEntity<ApiResponse<ReservationDTO>> updateReservation(
            @PathVariable Long id,
            @Valid @RequestBody CreateReservationRequest request,
            HttpServletRequest httpRequest) {
        try {
            Long userId = (Long) httpRequest.getAttribute("userId");
            ReservationDTO reservation = reservationService.updateReservation(userId, id, request);
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
            @RequestParam(required = false) String orderType,
            HttpServletRequest request) {

        Long userId = (Long) request.getAttribute("userId");
        PagedReservationResponse response = reservationService.getReservationsWithFilters(
            userId, page, size, searchKeyword, channel, roomType, checkinType,
            status, paymentStatus, isPackage, startDate, endDate, orderType);

        return ResponseEntity.ok(ApiResponse.success("获取订单列表成功", response));
    }

    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<ReservationStatistics>> getReservationStatistics(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        ReservationStatistics statistics = reservationService.getReservationStatistics(userId);
        return ResponseEntity.ok(ApiResponse.success("获取订单统计成功", statistics));
    }

    @GetMapping("/today/new")
    public ResponseEntity<ApiResponse<List<ReservationDTO>>> getTodayNewReservations(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        List<ReservationDTO> reservations = reservationService.getTodayNewReservations(userId);
        return ResponseEntity.ok(ApiResponse.success("获取今日新增订单成功", reservations));
    }

    @GetMapping("/unassigned")
    public ResponseEntity<ApiResponse<List<ReservationDTO>>> getUnassignedReservations(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        List<ReservationDTO> reservations = reservationService.getUnassignedReservations(userId);
        return ResponseEntity.ok(ApiResponse.success("获取未排房订单成功", reservations));
    }

    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<List<ReservationDTO>>> getPendingReservations(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        List<ReservationDTO> reservations = reservationService.getPendingReservations(userId);
        return ResponseEntity.ok(ApiResponse.success("获取待处理订单成功", reservations));
    }

    @GetMapping("/by-type")
    public ResponseEntity<ApiResponse<List<ReservationDTO>>> getReservationsByType(@RequestParam String type, HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            List<ReservationDTO> reservations = reservationService.getReservationsByType(userId, type);
            return ResponseEntity.ok(ApiResponse.success("获取" + getTypeDisplayName(type) + "订单成功", reservations));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("获取订单失败: " + e.getMessage()));
        }
    }

    private String getTypeDisplayName(String type) {
        switch (type) {
            case "today-arrivals": return "今日预抵";
            case "today-departures": return "今日预离";
            case "today-new": return "今日新办";
            case "unassigned": return "未排房";
            case "pending": return "待处理";
            default: return "订单";
        }
    }
}