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
            return ApiResponse.error("获取房态日历数据失败: " + e.getMessage());
        }
    }

    @PutMapping("/{roomId}")
    @RequirePermission(module = PermissionModule.ACCOMMODATION, action = PermissionAction.EDIT_ROOM_STATUS)
    public ApiResponse<String> updateRoomStatus(
            @PathVariable Long roomId,
            @Valid @RequestBody UpdateRoomStatusRequest request) {
        try {
            roomStatusService.updateRoomStatus(roomId, request.getDate(), request.getStatus(), request.getReason());
            return ApiResponse.success("房间状态更新成功");
        } catch (Exception e) {
            return ApiResponse.error("房间状态更新失败: " + e.getMessage());
        }
    }

    @PostMapping("/blockouts/close")
    @RequirePermission(module = PermissionModule.ACCOMMODATION, action = PermissionAction.EDIT_ROOM_STATUS)
    public ApiResponse<RoomBlockoutSummaryDTO> closeRooms(@Valid @RequestBody UpsertRoomBlockoutRequest request) {
        try {
            return ApiResponse.success(roomStatusService.closeRooms(request));
        } catch (Exception e) {
            return ApiResponse.error("关房失败: " + e.getMessage());
        }
    }

    @PostMapping("/blockouts/open")
    @RequirePermission(module = PermissionModule.ACCOMMODATION, action = PermissionAction.EDIT_ROOM_STATUS)
    public ApiResponse<RoomBlockoutSummaryDTO> openRooms(@Valid @RequestBody OpenRoomBlockoutRequest request) {
        try {
            return ApiResponse.success(roomStatusService.openRooms(request));
        } catch (Exception e) {
            return ApiResponse.error("开房失败: " + e.getMessage());
        }
    }

    @GetMapping("/statistics")
    @RequirePermission(module = PermissionModule.ACCOMMODATION, action = PermissionAction.VIEW_ROOM_STATUS)
    public ApiResponse<RoomStatusStatisticsDTO> getRoomStatusStatistics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        try {
            return ApiResponse.success(roomStatusService.getRoomStatusStatistics(date));
        } catch (Exception e) {
            return ApiResponse.error("获取房态统计数据失败: " + e.getMessage());
        }
    }
}
