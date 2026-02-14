package server.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import server.demo.annotation.RequirePermission;
import server.demo.annotation.StoreScoped;
import server.demo.dto.ApiResponse;
import server.demo.dto.FutureRoomTableResponse;
import server.demo.enums.PermissionAction;
import server.demo.enums.PermissionModule;
import server.demo.service.FutureRoomTableService;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/future-room-table")
@StoreScoped
public class FutureRoomTableController {

    @Autowired
    private FutureRoomTableService futureRoomTableService;

    /**
     * 获取远期房情表数据
     * @param startDate 开始日期
     * @param days 天数（默认7天）
     * @return 远期房情表数据
     */
    @GetMapping
    @RequirePermission(module = PermissionModule.ACCOMMODATION, action = PermissionAction.VIEW_ROOM_INFO)
    public ResponseEntity<ApiResponse<FutureRoomTableResponse>> getFutureRoomTable(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(defaultValue = "7") int days
    ) {
        try {
            FutureRoomTableResponse data = futureRoomTableService.getFutureRoomTableData(startDate, days);
            return ResponseEntity.ok(ApiResponse.success("获取远期房情表数据成功", data));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("获取远期房情表数据失败: " + e.getMessage()));
        }
    }
}
