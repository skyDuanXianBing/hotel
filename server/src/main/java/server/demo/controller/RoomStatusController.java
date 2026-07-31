package server.demo.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import server.demo.annotation.RequirePermission;
import server.demo.annotation.StoreScoped;
import server.demo.dto.ApiResponse;
import server.demo.dto.OpenRoomBlockoutRequest;
import server.demo.dto.RoomBlockoutSummaryDTO;
import server.demo.dto.RoomStatusCalendarDTO;
import server.demo.dto.RoomStatusStatisticsDTO;
import server.demo.dto.UpsertRoomBlockoutRequest;
import server.demo.dto.UpdateRoomStatusRequest;
import server.demo.enums.PermissionAction;
import server.demo.enums.PermissionModule;
import server.demo.service.RoomStatusService;

import java.time.LocalDate;

import server.demo.i18n.ApiMessages;
@RestController
@RequestMapping("/api/v1/room-status")
@StoreScoped
public class RoomStatusController extends BaseStoreController {

    @Autowired
    private RoomStatusService roomStatusService;

    @GetMapping("/calendar")
    @RequirePermission(module = PermissionModule.ACCOMMODATION, action = PermissionAction.VIEW_ROOM_STATUS)
    public ApiResponse<RoomStatusCalendarDTO> getRoomStatusCalendar(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            return ApiResponse.success(roomStatusService.getRoomStatusCalendar(startDate, endDate));
        } catch (Exception e) {
            return ApiResponse.error(ApiMessages.get("api.t.5e2febc662bc") + e.getMessage());
        }
    }

    @PutMapping("/{roomId}")
    @RequirePermission(module = PermissionModule.ACCOMMODATION, action = PermissionAction.EDIT_ROOM_STATUS)
    public ApiResponse<String> updateRoomStatus(
            @PathVariable Long roomId,
            @Valid @RequestBody UpdateRoomStatusRequest request) {
        try {
            roomStatusService.updateRoomStatus(roomId, request.getDate(), request.getStatus(), request.getReason());
            return ApiResponse.success(ApiMessages.get("api.t.b478a9660b3f"));
        } catch (Exception e) {
            return ApiResponse.error(ApiMessages.get("api.t.d4101749fa94") + e.getMessage());
        }
    }

    @PostMapping("/blockouts/close")
    @RequirePermission(module = PermissionModule.ACCOMMODATION, action = PermissionAction.EDIT_ROOM_STATUS)
    public ApiResponse<RoomBlockoutSummaryDTO> closeRooms(@Valid @RequestBody UpsertRoomBlockoutRequest request) {
        try {
            return ApiResponse.success(roomStatusService.closeRooms(request));
        } catch (Exception e) {
            return ApiResponse.error(ApiMessages.get("api.t.4ed8b06c3c24") + e.getMessage());
        }
    }

    @PostMapping("/blockouts/open")
    @RequirePermission(module = PermissionModule.ACCOMMODATION, action = PermissionAction.EDIT_ROOM_STATUS)
    public ApiResponse<RoomBlockoutSummaryDTO> openRooms(@Valid @RequestBody OpenRoomBlockoutRequest request) {
        try {
            return ApiResponse.success(roomStatusService.openRooms(request));
        } catch (Exception e) {
            return ApiResponse.error(ApiMessages.get("api.t.ee25eefd21ee") + e.getMessage());
        }
    }

    @GetMapping("/statistics")
    @RequirePermission(module = PermissionModule.ACCOMMODATION, action = PermissionAction.VIEW_ROOM_STATUS)
    public ApiResponse<RoomStatusStatisticsDTO> getRoomStatusStatistics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        try {
            return ApiResponse.success(roomStatusService.getRoomStatusStatistics(date));
        } catch (Exception e) {
            return ApiResponse.error(ApiMessages.get("api.t.0cf79290dc22") + e.getMessage());
        }
    }
}
