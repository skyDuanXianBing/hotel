package server.demo.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import server.demo.dto.ApiResponse;
import server.demo.dto.BulkPriceChangeRequest;
import server.demo.dto.RoomPriceDTO;
import server.demo.dto.RoomPriceManagementDTO;
import server.demo.dto.UpdatePriceByPlanRequest;
import server.demo.dto.UpdateRoomPriceRequest;
import server.demo.service.RoomPriceService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/room-prices")
@CrossOrigin(origins = {"http://localhost:8091", "http://127.0.0.1:8091"}, allowCredentials = "true")
public class RoomPriceController {

    @Autowired
    private RoomPriceService roomPriceService;

    /**
     * 获取指定日期范围内的房价数据
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<RoomPriceDTO>>> getRoomPrices(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) List<Long> roomTypeIds) {
        try {
            List<RoomPriceDTO> roomPrices;
            if (roomTypeIds != null && !roomTypeIds.isEmpty()) {
                roomPrices = roomPriceService.getRoomPricesByRoomTypesAndDateRange(roomTypeIds, startDate, endDate);
            } else {
                roomPrices = roomPriceService.getRoomPricesByDateRange(startDate, endDate);
            }
            return ResponseEntity.ok(ApiResponse.success("获取房价数据成功", roomPrices));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("获取房价数据失败: " + e.getMessage()));
        }
    }

    /**
     * 获取特定房型和日期的价格
     */
    @GetMapping("/{roomTypeId}")
    public ResponseEntity<ApiResponse<RoomPriceDTO>> getRoomPrice(
            @PathVariable Long roomTypeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        try {
            Optional<RoomPriceDTO> roomPrice = roomPriceService.getRoomPrice(roomTypeId, date);
            if (roomPrice.isPresent()) {
                return ResponseEntity.ok(ApiResponse.success("获取房价成功", roomPrice.get()));
            } else {
                return ResponseEntity.status(404)
                        .body(ApiResponse.error("该日期没有设置特定价格"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("获取房价失败: " + e.getMessage()));
        }
    }

    /**
     * 获取房型的有效价格（包含默认价格逻辑）
     */
    @GetMapping("/{roomTypeId}/effective")
    public ResponseEntity<ApiResponse<BigDecimal>> getEffectivePrice(
            @PathVariable Long roomTypeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        try {
            BigDecimal price = roomPriceService.getEffectivePrice(roomTypeId, date);
            return ResponseEntity.ok(ApiResponse.success("获取有效房价成功", price));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("获取有效房价失败: " + e.getMessage()));
        }
    }

    /**
     * 获取房型在指定日期范围内的价格
     */
    @GetMapping("/{roomTypeId}/range")
    public ResponseEntity<ApiResponse<List<RoomPriceDTO>>> getRoomPricesByRange(
            @PathVariable Long roomTypeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            List<RoomPriceDTO> roomPrices = roomPriceService.getRoomPricesByRoomTypeAndDateRange(roomTypeId, startDate, endDate);
            return ResponseEntity.ok(ApiResponse.success("获取房价数据成功", roomPrices));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("获取房价数据失败: " + e.getMessage()));
        }
    }

    /**
     * 更新房价
     */
    @PostMapping
    public ResponseEntity<ApiResponse<List<RoomPriceDTO>>> updateRoomPrice(
            @Valid @RequestBody UpdateRoomPriceRequest request) {
        try {
            List<RoomPriceDTO> updatedPrices = roomPriceService.updateRoomPrice(request);
            return ResponseEntity.ok(ApiResponse.success("房价更新成功", updatedPrices));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400)
                    .body(ApiResponse.error("参数错误: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("房价更新失败: " + e.getMessage()));
        }
    }

    /**
     * 批量更新房价
     */
    @PostMapping("/batch")
    public ResponseEntity<ApiResponse<List<RoomPriceDTO>>> batchUpdateRoomPrices(
            @Valid @RequestBody List<UpdateRoomPriceRequest> requests) {
        try {
            List<RoomPriceDTO> updatedPrices = roomPriceService.batchUpdateRoomPrices(requests);
            return ResponseEntity.ok(ApiResponse.success("批量房价更新成功", updatedPrices));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("批量房价更新失败: " + e.getMessage()));
        }
    }

    /**
     * 删除房价设置
     */
    @DeleteMapping("/{roomTypeId}")
    public ResponseEntity<ApiResponse<String>> deleteRoomPrice(
            @PathVariable Long roomTypeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        try {
            boolean deleted = roomPriceService.deleteRoomPrice(roomTypeId, date);
            if (deleted) {
                return ResponseEntity.ok(ApiResponse.success("房价删除成功"));
            } else {
                return ResponseEntity.status(404)
                        .body(ApiResponse.error("未找到要删除的房价设置"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("房价删除失败: " + e.getMessage()));
        }
    }

    /**
     * 删除房价设置（日期范围）
     */
    @DeleteMapping("/{roomTypeId}/range")
    public ResponseEntity<ApiResponse<String>> deleteRoomPriceRange(
            @PathVariable Long roomTypeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            int deletedCount = roomPriceService.deleteRoomPriceRange(roomTypeId, startDate, endDate);
            return ResponseEntity.ok(ApiResponse.success("成功删除 " + deletedCount + " 条房价设置"));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("房价删除失败: " + e.getMessage()));
        }
    }

    /**
     * 批量改价（支持多房型、多日期范围、星期筛选、平日周末价）
     */
    @PostMapping("/bulk-change")
    public ResponseEntity<ApiResponse<List<RoomPriceDTO>>> bulkPriceChange(
            @Valid @RequestBody BulkPriceChangeRequest request) {
        try {
            List<RoomPriceDTO> updatedPrices = roomPriceService.bulkPriceChange(request);
            return ResponseEntity.ok(ApiResponse.success(
                "批量改价成功，共更新 " + updatedPrices.size() + " 条房价记录",
                updatedPrices
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400)
                    .body(ApiResponse.error("参数错误: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("批量改价失败: " + e.getMessage()));
        }
    }

    /**
     * 获取房价管理数据(包含价格计划)
     */
    @GetMapping("/management")
    public ResponseEntity<ApiResponse<List<RoomPriceManagementDTO>>> getRoomPriceManagementData(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long roomTypeId,
            @RequestParam(required = false) Long userId) {
        try {
            List<RoomPriceManagementDTO> data = roomPriceService.getRoomPriceManagementData(
                    startDate, endDate, roomTypeId, userId);
            return ResponseEntity.ok(ApiResponse.success("获取房价管理数据成功", data));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("获取房价管理数据失败: " + e.getMessage()));
        }
    }

    /**
     * 按价格计划更新价格
     */
    @PostMapping("/update-by-plan")
    public ResponseEntity<ApiResponse<List<RoomPriceManagementDTO>>> updatePriceByPlan(
            @Valid @RequestBody UpdatePriceByPlanRequest request,
            @RequestParam Long userId,
            @RequestParam String operator) {
        try {
            List<RoomPriceManagementDTO> updatedPrices = roomPriceService.updatePriceByPlan(
                    request, userId, operator);
            return ResponseEntity.ok(ApiResponse.success(
                    "价格更新成功，共更新 " + updatedPrices.size() + " 条记录",
                    updatedPrices
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400)
                    .body(ApiResponse.error("参数错误: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("价格更新失败: " + e.getMessage()));
        }
    }
}