package server.demo.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import server.demo.annotation.RequirePermission;
import server.demo.dto.ApiResponse;
import server.demo.dto.BulkPriceChangeRequest;
import server.demo.dto.RoomPriceDTO;
import server.demo.dto.RoomPriceManagementDTO;
import server.demo.dto.UpdatePriceByPlanRequest;
import server.demo.dto.UpdateRoomPriceRequest;
import server.demo.context.StoreContextHolder;
import server.demo.enums.PermissionAction;
import server.demo.enums.PermissionModule;
import server.demo.service.SuAriAutoSyncService;
import server.demo.service.RoomPriceService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import server.demo.i18n.ApiMessages;
@RestController
@RequestMapping("/api/v1/room-prices")
@server.demo.annotation.StoreScoped
public class RoomPriceController {

    @Autowired
    private RoomPriceService roomPriceService;

    @Autowired(required = false)
    private SuAriAutoSyncService suAriAutoSyncService;

    /**
     * 获取指定日期范围内的房价数据
     */
    @GetMapping
    @RequirePermission(module = PermissionModule.ACCOMMODATION, action = PermissionAction.VIEW_ROOM_PRICE)
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
            return ResponseEntity.ok(ApiResponse.success(ApiMessages.get("api.t.bcbb2d0f4d44"), roomPrices));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error(ApiMessages.get("api.t.8abed0e359dd") + e.getMessage()));
        }
    }

    /**
     * 获取特定房型和日期的价格
     */
    @GetMapping("/{roomTypeId}")
    @RequirePermission(module = PermissionModule.ACCOMMODATION, action = PermissionAction.VIEW_ROOM_PRICE)
    public ResponseEntity<ApiResponse<RoomPriceDTO>> getRoomPrice(
            @PathVariable Long roomTypeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        try {
            Optional<RoomPriceDTO> roomPrice = roomPriceService.getRoomPrice(roomTypeId, date);
            if (roomPrice.isPresent()) {
                return ResponseEntity.ok(ApiResponse.success(ApiMessages.get("api.t.1890ba0a1ad4"), roomPrice.get()));
            } else {
                return ResponseEntity.status(404)
                        .body(ApiResponse.error(ApiMessages.get("api.t.6ab27d35c82e")));
            }
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error(ApiMessages.get("api.t.ab636cdb2b99") + e.getMessage()));
        }
    }

    /**
     * 获取房型的有效价格（包含默认价格逻辑）
     */
    @GetMapping("/{roomTypeId}/effective")
    @RequirePermission(module = PermissionModule.ACCOMMODATION, action = PermissionAction.VIEW_ROOM_PRICE)
    public ResponseEntity<ApiResponse<BigDecimal>> getEffectivePrice(
            @PathVariable Long roomTypeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        try {
            BigDecimal price = roomPriceService.getEffectivePrice(roomTypeId, date);
            return ResponseEntity.ok(ApiResponse.success(ApiMessages.get("api.t.a1c17bbd277f"), price));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error(ApiMessages.get("api.t.6ab74473ffed") + e.getMessage()));
        }
    }

    /**
     * 获取房型在指定日期范围内的价格
     */
    @GetMapping("/{roomTypeId}/range")
    @RequirePermission(module = PermissionModule.ACCOMMODATION, action = PermissionAction.VIEW_ROOM_PRICE)
    public ResponseEntity<ApiResponse<List<RoomPriceDTO>>> getRoomPricesByRange(
            @PathVariable Long roomTypeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            List<RoomPriceDTO> roomPrices = roomPriceService.getRoomPricesByRoomTypeAndDateRange(roomTypeId, startDate, endDate);
            return ResponseEntity.ok(ApiResponse.success(ApiMessages.get("api.t.bcbb2d0f4d44"), roomPrices));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error(ApiMessages.get("api.t.8abed0e359dd") + e.getMessage()));
        }
    }

    /**
     * 更新房价
     */
    @PostMapping
    @RequirePermission(module = PermissionModule.ACCOMMODATION, action = PermissionAction.EDIT_ROOM_PRICE)
    public ResponseEntity<ApiResponse<List<RoomPriceDTO>>> updateRoomPrice(
            @Valid @RequestBody UpdateRoomPriceRequest request) {
        try {
            List<RoomPriceDTO> updatedPrices = roomPriceService.updateRoomPrice(request);
            return ResponseEntity.ok(ApiResponse.success(ApiMessages.get("api.t.c406fbbd3b0d"), updatedPrices));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400)
                    .body(ApiResponse.error(ApiMessages.get("api.t.567d311e6a3e") + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error(ApiMessages.get("api.t.4fe0a1fd8b63") + e.getMessage()));
        }
    }

    /**
     * 批量更新房价
     */
    @PostMapping("/batch")
    @RequirePermission(module = PermissionModule.ACCOMMODATION, action = PermissionAction.BATCH_CHANGE_PRICE)
    public ResponseEntity<ApiResponse<List<RoomPriceDTO>>> batchUpdateRoomPrices(
            @Valid @RequestBody List<UpdateRoomPriceRequest> requests) {
        try {
            List<RoomPriceDTO> updatedPrices = roomPriceService.batchUpdateRoomPrices(requests);
            return ResponseEntity.ok(ApiResponse.success(ApiMessages.get("api.t.79a69d2a9e81"), updatedPrices));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error(ApiMessages.get("api.t.bb8223c35479") + e.getMessage()));
        }
    }

    /**
     * 删除房价设置
     */
    @DeleteMapping("/{roomTypeId}")
    @RequirePermission(module = PermissionModule.ACCOMMODATION, action = PermissionAction.EDIT_ROOM_PRICE)
    public ResponseEntity<ApiResponse<String>> deleteRoomPrice(
            @PathVariable Long roomTypeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        try {
            boolean deleted = roomPriceService.deleteRoomPrice(roomTypeId, date);
            if (deleted) {
                return ResponseEntity.ok(ApiResponse.success(ApiMessages.get("api.t.ac6ec44c598b")));
            } else {
                return ResponseEntity.status(404)
                        .body(ApiResponse.error(ApiMessages.get("api.t.931aa77f4b54")));
            }
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error(ApiMessages.get("api.t.28754a72b121") + e.getMessage()));
        }
    }

    /**
     * 删除房价设置（日期范围）
     */
    @DeleteMapping("/{roomTypeId}/range")
    @RequirePermission(module = PermissionModule.ACCOMMODATION, action = PermissionAction.EDIT_ROOM_PRICE)
    public ResponseEntity<ApiResponse<String>> deleteRoomPriceRange(
            @PathVariable Long roomTypeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            int deletedCount = roomPriceService.deleteRoomPriceRange(roomTypeId, startDate, endDate);
            return ResponseEntity.ok(ApiResponse.success(ApiMessages.get("api.t.82121e47a5e9") + deletedCount + ApiMessages.get("api.t.ca8df759ad5b")));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error(ApiMessages.get("api.t.28754a72b121") + e.getMessage()));
        }
    }

    /**
     * 批量改价（支持多房型、多日期范围、星期筛选、平日周末价）
     */
    @PostMapping("/bulk-change")
    @RequirePermission(module = PermissionModule.ACCOMMODATION, action = PermissionAction.BATCH_CHANGE_PRICE)
    public ResponseEntity<ApiResponse<List<RoomPriceDTO>>> bulkPriceChange(
            @Valid @RequestBody BulkPriceChangeRequest request) {
        try {
            List<RoomPriceDTO> updatedPrices = roomPriceService.bulkPriceChange(request);
            return ResponseEntity.ok(ApiResponse.success(
                ApiMessages.get("api.t.3d3c6dd36274") + updatedPrices.size() + ApiMessages.get("api.t.86b04b9efd76"),
                updatedPrices
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400)
                    .body(ApiResponse.error(ApiMessages.get("api.t.567d311e6a3e") + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error(ApiMessages.get("api.t.b8aa79b26067") + e.getMessage()));
        }
    }

    /**
     * 获取房价管理数据(包含价格计划)
     */
    @GetMapping("/management")
    @RequirePermission(module = PermissionModule.ACCOMMODATION, action = PermissionAction.VIEW_ROOM_PRICE)
    public ResponseEntity<ApiResponse<List<RoomPriceManagementDTO>>> getRoomPriceManagementData(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long roomTypeId) {
        try {
            List<RoomPriceManagementDTO> data = roomPriceService.getRoomPriceManagementData(
                    startDate, endDate, roomTypeId);
            return ResponseEntity.ok(ApiResponse.success(ApiMessages.get("api.t.695ecfef40dc"), data));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error(ApiMessages.get("api.t.167010d58308") + e.getMessage()));
        }
    }

    /**
     * 按价格计划更新价格
     */
    @PostMapping("/update-by-plan")
    @RequirePermission(module = PermissionModule.ACCOMMODATION, action = PermissionAction.BATCH_CHANGE_PRICE)
    public ResponseEntity<ApiResponse<List<RoomPriceManagementDTO>>> updatePriceByPlan(
            @Valid @RequestBody UpdatePriceByPlanRequest request,
            @RequestParam String operator) {
        try {
            List<RoomPriceManagementDTO> updatedPrices = roomPriceService.updatePriceByPlan(
                    request, operator);
            if (suAriAutoSyncService != null) {
                Long storeId = StoreContextHolder.getContext().getStoreId();
                // 认证严格模式：当设置 Stop Sell（closeRoom/closed）时，同时推送库存(roomstosell)，
                // 以确保 SU 侧“库存/停售”状态一致，且仍严格限定在本次选择的房型/价格计划/日期范围内。
                boolean pushAvailability = request.getAvailableRooms() != null || request.getCloseRoom() != null;
                boolean pushRates = request.getPrice() != null;
                boolean pushRestrictions = request.getMinStay() != null
                        || request.getMaxStay() != null
                        || request.getCloseRoom() != null
                        || request.getCta() != null
                        || request.getCtd() != null;

                if (pushAvailability || pushRates || pushRestrictions) {
                    List<Integer> weekdays = request.getWeekdays();
                    boolean allDays = weekdays == null || weekdays.isEmpty() || weekdays.contains(0);

                    List<SuAriAutoSyncService.DateRange> ranges = new java.util.ArrayList<>();
                    LocalDate rangeStart = null;
                    LocalDate prev = null;
                    LocalDate d = request.getStartDate();
                    while (d != null && request.getEndDate() != null && !d.isAfter(request.getEndDate())) {
                        boolean match = allDays || weekdays.contains(d.getDayOfWeek().getValue());
                        if (match) {
                            if (rangeStart == null) {
                                rangeStart = d;
                                prev = d;
                            } else if (prev != null && d.equals(prev.plusDays(1))) {
                                prev = d;
                            } else {
                                ranges.add(new SuAriAutoSyncService.DateRange(rangeStart, prev));
                                rangeStart = d;
                                prev = d;
                            }
                        }
                        d = d.plusDays(1);
                    }
                    if (rangeStart != null && prev != null) {
                        ranges.add(new SuAriAutoSyncService.DateRange(rangeStart, prev));
                    }

                    suAriAutoSyncService.enqueueForStoreDateRanges(
                            storeId,
                            "room_price_update_by_plan",
                            ranges,
                            Set.of(request.getRoomTypeId()),
                            Set.of(request.getPricePlanId()),
                            pushAvailability,
                            pushRates,
                            pushRestrictions,
                            false
                    );
                }
            }
            return ResponseEntity.ok(ApiResponse.success(
                    ApiMessages.get("api.t.dd06afeba744") + updatedPrices.size() + ApiMessages.get("api.t.bed75b36517a"),
                    updatedPrices
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400)
                    .body(ApiResponse.error(ApiMessages.get("api.t.567d311e6a3e") + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error(ApiMessages.get("api.t.ea70a5e845ad") + e.getMessage()));
        }
    }
}
