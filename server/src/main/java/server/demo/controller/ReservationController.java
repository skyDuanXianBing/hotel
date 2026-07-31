package server.demo.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import server.demo.annotation.RequirePermission;
import server.demo.annotation.StoreScoped;
import server.demo.dto.ApiResponse;
import server.demo.dto.AssignReservationRoomRequest;
import server.demo.dto.AssignableRoomsResponse;
import server.demo.dto.BatchCreateReservationRequest;
import server.demo.dto.BatchCreateReservationResponse;
import server.demo.dto.CreateReservationRequest;
import server.demo.dto.PagedReservationResponse;
import server.demo.dto.ReservationChannelInfoDTO;
import server.demo.dto.ReservationDTO;
import server.demo.dto.ReservationHoverSummaryRequest;
import server.demo.dto.ReservationHoverSummaryResponseDTO;
import server.demo.dto.ReservationStatistics;
import server.demo.dto.UpdateReservationSettlementStatusRequest;
import server.demo.enums.PermissionAction;
import server.demo.enums.PermissionModule;
import server.demo.service.ReservationService;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import server.demo.i18n.ApiMessages;
@RestController
@RequestMapping("/api/v1/reservations")
@StoreScoped
public class ReservationController extends BaseStoreController {

    @Autowired
    private ReservationService reservationService;

    @PostMapping
    @RequirePermission(module = PermissionModule.ORDER, action = PermissionAction.MODIFY_ORDER)
    public ResponseEntity<ApiResponse<ReservationDTO>> createReservation(
            @Valid @RequestBody CreateReservationRequest request) {
        try {
            ReservationDTO reservation = reservationService.createReservation(request);
            return ResponseEntity.ok(ApiResponse.success(ApiMessages.get("api.t.c6db3bc677ed"), reservation));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(ApiMessages.get("api.t.3819387947d3") + e.getMessage()));
        }
    }

    @PostMapping("/batch")
    @RequirePermission(module = PermissionModule.ORDER, action = PermissionAction.MODIFY_ORDER)
    public ResponseEntity<ApiResponse<BatchCreateReservationResponse>> createBatchReservation(
            @Valid @RequestBody BatchCreateReservationRequest request) {
        try {
            BatchCreateReservationResponse reservations = reservationService.createBatchReservations(request);
            return ResponseEntity.ok(ApiResponse.success(ApiMessages.get("api.t.784c468df935"), reservations));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(ApiMessages.get("api.t.e4e557bf645c") + e.getMessage()));
        }
    }

    @PostMapping("/hover-summaries")
    @RequirePermission(module = PermissionModule.ACCOMMODATION, action = PermissionAction.VIEW_ROOM_STATUS)
    public ResponseEntity<ApiResponse<ReservationHoverSummaryResponseDTO>> getHoverSummaries(
            @Valid @RequestBody ReservationHoverSummaryRequest request) {
        ReservationHoverSummaryResponseDTO response = reservationService.getHoverSummaries(request.getReservationIds());
        return ResponseEntity.ok(ApiResponse.success(ApiMessages.get("api.t.e68fad36a044"), response));
    }

    @PostMapping("/{id}/check-in")
    @RequirePermission(module = PermissionModule.ORDER, action = PermissionAction.MODIFY_ORDER)
    public ResponseEntity<ApiResponse<ReservationDTO>> checkIn(@PathVariable Long id) {
        try {
            ReservationDTO reservation = reservationService.checkIn(id);
            return ResponseEntity.ok(ApiResponse.success(ApiMessages.get("api.t.a21d69f36b0b"), reservation));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(ApiMessages.get("api.t.3ea361822773") + e.getMessage()));
        }
    }

    @PostMapping("/{id}/check-out")
    @RequirePermission(module = PermissionModule.ORDER, action = PermissionAction.MODIFY_ORDER)
    public ResponseEntity<ApiResponse<ReservationDTO>> checkOut(@PathVariable Long id) {
        try {
            ReservationDTO reservation = reservationService.checkOut(id);
            return ResponseEntity.ok(ApiResponse.success(ApiMessages.get("api.t.f32827c48ff7"), reservation));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(ApiMessages.get("api.t.d27110e63e96") + e.getMessage()));
        }
    }

    @PostMapping("/{id}/cancel")
    @RequirePermission(module = PermissionModule.ORDER, action = PermissionAction.CANCEL_ORDER)
    public ResponseEntity<ApiResponse<ReservationDTO>> cancelReservation(@PathVariable Long id) {
        try {
            ReservationDTO reservation = reservationService.cancelReservation(id);
            return ResponseEntity.ok(ApiResponse.success(ApiMessages.get("api.t.7596874072b6"), reservation));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(ApiMessages.get("api.t.407b32fbf214") + e.getMessage()));
        }
    }

    @PostMapping("/{id}/settlement-status")
    @RequirePermission(module = PermissionModule.ORDER, action = PermissionAction.MODIFY_ORDER)
    public ResponseEntity<ApiResponse<ReservationDTO>> updateSettlementStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateReservationSettlementStatusRequest request
    ) {
        try {
            ReservationDTO reservation = reservationService.updateSettlementStatus(id, Boolean.TRUE.equals(request.getSettled()));
            return ResponseEntity.ok(ApiResponse.success(ApiMessages.get("api.t.564dc3734725"), reservation));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(ApiMessages.get("api.t.400425083502") + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    @RequirePermission(module = PermissionModule.ORDER, action = PermissionAction.VIEW_ORDERS)
    public ResponseEntity<ApiResponse<ReservationDTO>> getReservation(@PathVariable Long id) {
        Optional<ReservationDTO> reservation = reservationService.getReservationById(id);
        return reservation.map(res ->
                        ResponseEntity.ok(ApiResponse.success(ApiMessages.get("api.t.20f167d846d2"), res)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/order/{orderNumber}")
    @RequirePermission(module = PermissionModule.ORDER, action = PermissionAction.VIEW_ORDERS)
    public ResponseEntity<ApiResponse<ReservationDTO>> getReservationByOrderNumber(@PathVariable String orderNumber) {
        Optional<ReservationDTO> reservation = reservationService.getReservationByOrderNumber(orderNumber);
        return reservation.map(res ->
                        ResponseEntity.ok(ApiResponse.success(ApiMessages.get("api.t.20f167d846d2"), res)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/date-range")
    @RequirePermission(module = PermissionModule.ORDER, action = PermissionAction.VIEW_ORDERS)
    public ResponseEntity<ApiResponse<List<ReservationDTO>>> getReservationsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<ReservationDTO> reservations = reservationService.getReservationsByDateRange(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(ApiMessages.get("api.t.ec8dd8556676"), reservations));
    }

    @GetMapping("/room/{roomId}")
    @RequirePermission(module = PermissionModule.ORDER, action = PermissionAction.VIEW_ORDERS)
    public ResponseEntity<ApiResponse<List<ReservationDTO>>> getReservationsByRoomId(@PathVariable Long roomId) {
        List<ReservationDTO> reservations = reservationService.getReservationsByRoomId(roomId);
        return ResponseEntity.ok(ApiResponse.success(ApiMessages.get("api.t.cd5ba9d086c4"), reservations));
    }

    @GetMapping("/room/{roomId}/date/{date}")
    @RequirePermission(module = PermissionModule.ORDER, action = PermissionAction.VIEW_ORDERS)
    public ResponseEntity<ApiResponse<ReservationDTO>> getReservationByRoomAndDate(
            @PathVariable Long roomId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        Optional<ReservationDTO> reservation = reservationService.getReservationByRoomAndDate(roomId, date);
        return reservation.map(res ->
                        ResponseEntity.ok(ApiResponse.success(ApiMessages.get("api.t.20f167d846d2"), res)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/search")
    @RequirePermission(module = PermissionModule.ORDER, action = PermissionAction.VIEW_ORDERS)
    public ResponseEntity<ApiResponse<List<ReservationDTO>>> searchReservations(
            @RequestParam String keyword) {
        List<ReservationDTO> reservations = reservationService.searchReservationsByGuestInfo(keyword);
        return ResponseEntity.ok(ApiResponse.success(ApiMessages.get("api.t.9757c0e8355f"), reservations));
    }

    @GetMapping("/today/check-in")
    @RequirePermission(module = PermissionModule.ORDER, action = PermissionAction.VIEW_ORDERS)
    public ResponseEntity<ApiResponse<List<ReservationDTO>>> getTodayCheckIns() {
        List<ReservationDTO> reservations = reservationService.getTodayCheckIns();
        return ResponseEntity.ok(ApiResponse.success(ApiMessages.get("api.t.dbb6f29a20b8"), reservations));
    }

    @GetMapping("/today/check-out")
    @RequirePermission(module = PermissionModule.ORDER, action = PermissionAction.VIEW_ORDERS)
    public ResponseEntity<ApiResponse<List<ReservationDTO>>> getTodayCheckOuts() {
        List<ReservationDTO> reservations = reservationService.getTodayCheckOuts();
        return ResponseEntity.ok(ApiResponse.success(ApiMessages.get("api.t.19fff61a457f"), reservations));
    }

    @PostMapping("/{id}/update")
    @RequirePermission(module = PermissionModule.ORDER, action = PermissionAction.MODIFY_ORDER)
    public ResponseEntity<ApiResponse<ReservationDTO>> updateReservation(
            @PathVariable Long id,
            @Valid @RequestBody CreateReservationRequest request) {
        try {
            ReservationDTO reservation = reservationService.updateReservation(id, request);
            return ResponseEntity.ok(ApiResponse.success(ApiMessages.get("api.t.7a741993b8de"), reservation));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(ApiMessages.get("api.t.483e861c2c64") + e.getMessage()));
        }
    }

    @GetMapping
    @RequirePermission(module = PermissionModule.ORDER, action = PermissionAction.VIEW_ORDERS)
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
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate operationDate) {

        PagedReservationResponse response = reservationService.getReservationsWithFilters(
                page, size, searchKeyword, channel, roomType, checkinType,
                status, paymentStatus, isPackage, startDate, endDate, orderType, operationDate);

        return ResponseEntity.ok(ApiResponse.success(ApiMessages.get("api.t.ec8dd8556676"), response));
    }

    @GetMapping("/statistics")
    @RequirePermission(module = PermissionModule.ORDER, action = PermissionAction.VIEW_ORDERS)
    public ResponseEntity<ApiResponse<ReservationStatistics>> getReservationStatistics() {
        ReservationStatistics statistics = reservationService.getReservationStatistics();
        return ResponseEntity.ok(ApiResponse.success(ApiMessages.get("api.t.06f9a038c21c"), statistics));
    }

    @GetMapping("/today/new")
    @RequirePermission(module = PermissionModule.ORDER, action = PermissionAction.VIEW_ORDERS)
    public ResponseEntity<ApiResponse<List<ReservationDTO>>> getTodayNewReservations() {
        List<ReservationDTO> reservations = reservationService.getTodayNewReservations();
        return ResponseEntity.ok(ApiResponse.success(ApiMessages.get("api.t.216f4edcd0c6"), reservations));
    }

    @GetMapping("/unassigned")
    @RequirePermission(module = PermissionModule.ORDER, action = PermissionAction.VIEW_ORDERS)
    public ResponseEntity<ApiResponse<List<ReservationDTO>>> getUnassignedReservations() {
        List<ReservationDTO> reservations = reservationService.getUnassignedReservations();
        return ResponseEntity.ok(ApiResponse.success(ApiMessages.get("api.t.f4a558b80c4e"), reservations));
    }

    @GetMapping("/pending")
    @RequirePermission(module = PermissionModule.ORDER, action = PermissionAction.VIEW_ORDERS)
    public ResponseEntity<ApiResponse<List<ReservationDTO>>> getPendingReservations() {
        List<ReservationDTO> reservations = reservationService.getPendingReservations();
        return ResponseEntity.ok(ApiResponse.success(ApiMessages.get("api.t.04fb48eb817f"), reservations));
    }

    @GetMapping("/by-type")
    @RequirePermission(module = PermissionModule.ORDER, action = PermissionAction.VIEW_ORDERS)
    public ResponseEntity<ApiResponse<List<ReservationDTO>>> getReservationsByType(@RequestParam String type) {
        try {
            List<ReservationDTO> reservations = reservationService.getReservationsByType(type);
            return ResponseEntity.ok(ApiResponse.success(ApiMessages.get("api.t.5f49c1c38dc8") + getTypeDisplayName(type) + ApiMessages.get("api.t.3b8efb5fe786"), reservations));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(ApiMessages.get("api.t.8d74f8143362") + e.getMessage()));
        }
    }

    @GetMapping("/{id}/channel-info")
    @RequirePermission(module = PermissionModule.ORDER, action = PermissionAction.VIEW_ORDERS)
    public ResponseEntity<ApiResponse<ReservationChannelInfoDTO>> getReservationChannelInfo(@PathVariable Long id) {
        try {
            ReservationChannelInfoDTO info = reservationService.getChannelInfo(id);
            return ResponseEntity.ok(ApiResponse.success(info));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/{id}/assignable-rooms")
    @RequirePermission(module = PermissionModule.ORDER, action = PermissionAction.VIEW_ORDERS)
    public ResponseEntity<ApiResponse<AssignableRoomsResponse>> getAssignableRooms(
            @PathVariable Long id,
            @RequestParam(required = false) Long roomTypeId
    ) {
        try {
            AssignableRoomsResponse resp = reservationService.getAssignableRooms(id, roomTypeId);
            return ResponseEntity.ok(ApiResponse.success(ApiMessages.get("api.t.bcd21218690a"), resp));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{id}/assign-room")
    @RequirePermission(module = PermissionModule.ORDER, action = PermissionAction.MODIFY_ORDER)
    public ResponseEntity<ApiResponse<ReservationDTO>> assignRoom(
            @PathVariable Long id,
            @Valid @RequestBody AssignReservationRoomRequest request
    ) {
        try {
            ReservationDTO updated = reservationService.assignRoom(id, request.getRoomId());
            return ResponseEntity.ok(ApiResponse.success(ApiMessages.get("api.t.2714c04ce05f"), updated));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    private String getTypeDisplayName(String type) {
        switch (type) {
            case "today-arrivals":
                return ApiMessages.get("api.t.5f0c535868b8");
            case "today-departures":
                return ApiMessages.get("api.t.5c105e3d9403");
            case "today-new":
                return ApiMessages.get("api.t.77726319686c");
            case "unassigned":
                return ApiMessages.get("api.t.1269a36fb4ee");
            case "assigned":
                return ApiMessages.get("api.t.b4eeb5d0f990");
            case "pending":
                return ApiMessages.get("api.t.59a9eb4e6574");
            default:
                return ApiMessages.get("api.t.81b3798bebc6");
        }
    }
}
