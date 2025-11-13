package server.demo.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import server.demo.dto.ApiResponse;
import server.demo.dto.RoomStatusCalendarDTO;
import server.demo.dto.RoomStatusStatisticsDTO;
import server.demo.dto.UpdateRoomStatusRequest;
import server.demo.service.RoomStatusService;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/room-status")
@CrossOrigin
public class RoomStatusController {

    @Autowired
    private RoomStatusService roomStatusService;

    /**
     * 获取当前用户ID - 从HttpServletRequest属性中获取(由JwtInterceptor注入)
     */
    private Long getCurrentUserId(HttpServletRequest request) {
        Object userIdObj = request.getAttribute("userId");
        if (userIdObj instanceof Long) {
            return (Long) userIdObj;
        }
        return null;
    }

    @GetMapping("/calendar")
    public ApiResponse<RoomStatusCalendarDTO> getRoomStatusCalendar(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            HttpServletRequest request) {

        try {
            Long userId = getCurrentUserId(request);
            if (userId == null) {
                return ApiResponse.error("无法获取用户信息");
            }
            RoomStatusCalendarDTO calendarData = roomStatusService.getRoomStatusCalendar(userId, startDate, endDate);
            return ApiResponse.success(calendarData);
        } catch (Exception e) {
            return ApiResponse.error("获取房态日历数据失败: " + e.getMessage());
        }
    }

    @PutMapping("/{roomId}")
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

    @GetMapping("/statistics")
    public ApiResponse<RoomStatusStatisticsDTO> getRoomStatusStatistics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            HttpServletRequest request) {

        try {
            Long userId = getCurrentUserId(request);
            if (userId == null) {
                return ApiResponse.error("无法获取用户信息");
            }
            LocalDate targetDate = date != null ? date : LocalDate.now();
            RoomStatusStatisticsDTO statistics = roomStatusService.getRoomStatusStatistics(userId, targetDate);
            return ApiResponse.success(statistics);
        } catch (Exception e) {
            return ApiResponse.error("获取房态统计数据失败: " + e.getMessage());
        }
    }
}