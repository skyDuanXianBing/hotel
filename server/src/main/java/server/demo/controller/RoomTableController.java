package server.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import server.demo.annotation.RequirePermission;
import server.demo.annotation.StoreScoped;
import server.demo.dto.ApiResponse;
import server.demo.dto.RoomTableDataDTO;
import server.demo.dto.RoomTableMonthlyResponse;
import server.demo.enums.PermissionAction;
import server.demo.enums.PermissionModule;
import server.demo.service.RoomTableService;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/room-table")
@StoreScoped
public class RoomTableController {

    @Autowired
    private RoomTableService roomTableService;

    /**
     * 获取房情表统计数据
     * @param date 统计日期
     * @return 房情表数据
     */
    @GetMapping("/statistics")
    @RequirePermission(module = PermissionModule.ACCOMMODATION, action = PermissionAction.VIEW_ROOM_INFO)
    public ApiResponse<RoomTableDataDTO> getRoomTableStatistics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        try {
            RoomTableDataDTO roomTableData = roomTableService.getRoomTableStatistics(date);
            return ApiResponse.success(roomTableData);
        } catch (Exception e) {
            return ApiResponse.error("获取房情表数据失败: " + e.getMessage());
        }
    }

    @GetMapping("/monthly")
    @RequirePermission(module = PermissionModule.ACCOMMODATION, action = PermissionAction.VIEW_ROOM_INFO)
    public ApiResponse<RoomTableMonthlyResponse> getMonthlyRoomTable(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long roomTypeId) {
        try {
            RoomTableMonthlyResponse roomTableData =
                    roomTableService.getMonthlyRoomTable(startDate, endDate, roomTypeId);
            return ApiResponse.success(roomTableData);
        } catch (Exception e) {
            return ApiResponse.error("获取月度房情表数据失败: " + e.getMessage());
        }
    }
}
