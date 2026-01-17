package server.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import server.demo.annotation.StoreScoped;
import server.demo.context.StoreContextHolder;
import server.demo.dto.*;
import server.demo.enums.PriceAdjustmentType;
import server.demo.service.ChannelPriceFallbackService;
import server.demo.service.PriceLabsApiClient;
import server.demo.service.PriceLabsService;
import server.demo.service.PriceLabsReservationSyncService;
import server.demo.service.PriceLabsStatusService;
import server.demo.service.PriceLabsSyncService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * PriceLabs 集成控制器
 * 提供 PriceLabs 集成配置、渠道价格调整、价格查询等 API
 */
@RestController
@RequestMapping("/api/v1/pricelabs")
@CrossOrigin(origins = {"http://localhost:8091", "http://127.0.0.1:8091"}, allowCredentials = "true")
public class PriceLabsController {

    @Autowired
    private PriceLabsService priceLabsService;

    @Autowired
    private PriceLabsSyncService priceLabsSyncService;

    @Autowired
    private PriceLabsReservationSyncService priceLabsReservationSyncService;

    @Autowired
    private PriceLabsStatusService priceLabsStatusService;

    @Autowired
    private ChannelPriceFallbackService channelPriceFallbackService;

    // ==================== 集成配置 API ====================

    /**
     * 获取 PriceLabs 集成配置
     */
    @GetMapping("/integration")
    @StoreScoped
    public ResponseEntity<ApiResponse<PriceLabsIntegrationDTO>> getIntegration() {
        try {
            PriceLabsIntegrationDTO integration = priceLabsService.getIntegration();
            return ResponseEntity.ok(ApiResponse.success("获取集成配置成功", integration));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("获取集成配置失败: " + e.getMessage()));
        }
    }

    /**
     * 保存或更新集成配置
     */
    @PostMapping("/integration")
    @StoreScoped
    public ResponseEntity<ApiResponse<PriceLabsIntegrationDTO>> saveIntegration(
            @RequestBody PriceLabsIntegrationDTO request) {
        try {
            PriceLabsIntegrationDTO saved = priceLabsService.saveIntegration(request);
            return ResponseEntity.ok(ApiResponse.success("保存集成配置成功", saved));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("保存集成配置失败: " + e.getMessage()));
        }
    }

    /**
     * 启用/禁用集成
     */
    @PatchMapping("/integration/toggle")
    @StoreScoped
    public ResponseEntity<ApiResponse<PriceLabsIntegrationDTO>> toggleIntegration(
            @RequestBody Map<String, Boolean> request) {
        try {
            Boolean enabled = request.get("enabled");
            PriceLabsIntegrationDTO result = priceLabsService.toggleIntegration(enabled);
            return ResponseEntity.ok(ApiResponse.success(enabled ? "已启用 PriceLabs 集成" : "已禁用 PriceLabs 集成", result));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("操作失败: " + e.getMessage()));
        }
    }

    /**
     * 更新集成配置
     */
    @PatchMapping("/integration/config")
    @StoreScoped
    public ResponseEntity<ApiResponse<PriceLabsIntegrationDTO>> updateIntegrationConfig(
            @RequestBody Map<String, String> request) {
        try {
            String priceLabsEmail = request.get("priceLabsEmail");
            PriceLabsIntegrationDTO result = priceLabsService.updateIntegrationConfig(priceLabsEmail);
            return ResponseEntity.ok(ApiResponse.success("配置更新成功", result));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("更新配置失败: " + e.getMessage()));
        }
    }

    // ==================== 连接配置 API ====================

    /**
     * 获取所有连接配置
     */
    @GetMapping("/connections")
    @StoreScoped
    public ResponseEntity<ApiResponse<List<PriceLabsConnectionDTO>>> getConnections() {
        try {
            List<PriceLabsConnectionDTO> connections = priceLabsService.getConnections();
            return ResponseEntity.ok(ApiResponse.success("获取连接列表成功", connections));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("获取连接列表失败: " + e.getMessage()));
        }
    }

    /**
     * 创建连接配置
     */
    @PostMapping("/connections")
    @StoreScoped
    public ResponseEntity<ApiResponse<PriceLabsConnectionDTO>> createConnection(
            @RequestBody Map<String, Long> request) {
        try {
            Long roomTypeId = request.get("roomTypeId");
            Long pricePlanId = request.get("pricePlanId");
            PriceLabsConnectionDTO connection = priceLabsService.createConnection(roomTypeId, pricePlanId);
            return ResponseEntity.ok(ApiResponse.success("创建连接成功", connection));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("创建连接失败: " + e.getMessage()));
        }
    }

    /**
     * 更新连接状态
     */
    @PatchMapping("/connections/{id}/status")
    @StoreScoped
    public ResponseEntity<ApiResponse<PriceLabsConnectionDTO>> updateConnectionStatus(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> request) {
        try {
            Boolean enabled = request.get("enabled");
            return priceLabsService.updateConnectionStatus(id, enabled)
                    .map(conn -> ResponseEntity.ok(ApiResponse.success("更新连接状态成功", conn)))
                    .orElse(ResponseEntity.status(404)
                            .body(ApiResponse.error("连接不存在")));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("更新连接状态失败: " + e.getMessage()));
        }
    }

    /**
     * 删除连接配置
     */
    @DeleteMapping("/connections/{id}")
    @StoreScoped
    public ResponseEntity<ApiResponse<Void>> deleteConnection(@PathVariable Long id) {
        try {
            if (priceLabsService.deleteConnection(id)) {
                return ResponseEntity.ok(ApiResponse.success("删除连接成功", null));
            } else {
                return ResponseEntity.status(404)
                        .body(ApiResponse.error("连接不存在"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("删除连接失败: " + e.getMessage()));
        }
    }

    // ==================== 渠道价格调整 API ====================

    /**
     * 获取所有渠道的价格调整设置
     */
    @GetMapping("/channel-adjustments")
    @StoreScoped
    public ResponseEntity<ApiResponse<List<ChannelPriceAdjustmentDTO>>> getChannelPriceAdjustments() {
        try {
            List<ChannelPriceAdjustmentDTO> adjustments = priceLabsService.getChannelPriceAdjustments();
            return ResponseEntity.ok(ApiResponse.success("获取渠道价格调整设置成功", adjustments));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("获取渠道价格调整设置失败: " + e.getMessage()));
        }
    }

    /**
     * 更新单个渠道的价格调整设置
     */
    @PutMapping("/channel-adjustments/{channelId}")
    @StoreScoped
    public ResponseEntity<ApiResponse<ChannelPriceAdjustmentDTO>> updateChannelPriceAdjustment(
            @PathVariable Long channelId,
            @RequestBody ChannelPriceAdjustmentRequest request) {
        try {
            ChannelPriceAdjustmentDTO result = priceLabsService.updateChannelPriceAdjustment(
                    channelId,
                    request.getAdjustmentType(),
                    request.getAdjustmentValue(),
                    request.getAutoSyncPrice());
            return ResponseEntity.ok(ApiResponse.success("更新渠道价格调整设置成功", result));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("更新渠道价格调整设置失败: " + e.getMessage()));
        }
    }

    /**
     * 批量更新渠道价格调整设置
     */
    @PutMapping("/channel-adjustments/batch")
    @StoreScoped
    public ResponseEntity<ApiResponse<List<ChannelPriceAdjustmentDTO>>> batchUpdateChannelPriceAdjustments(
            @RequestBody List<ChannelPriceAdjustmentDTO> adjustments) {
        try {
            List<ChannelPriceAdjustmentDTO> results = priceLabsService.batchUpdateChannelPriceAdjustments(adjustments);
            return ResponseEntity.ok(ApiResponse.success("批量更新渠道价格调整设置成功", results));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("批量更新渠道价格调整设置失败: " + e.getMessage()));
        }
    }

    /**
     * 重新计算渠道价格
     */
    @PostMapping("/channel-adjustments/{channelId}/recalculate")
    @StoreScoped
    public ResponseEntity<ApiResponse<Map<String, Object>>> recalculateChannelPrices(
            @PathVariable Long channelId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            int count = priceLabsService.recalculateChannelPrices(channelId, startDate, endDate);
            return ResponseEntity.ok(ApiResponse.success("重新计算渠道价格成功",
                    Map.of("affectedCount", count)));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("重新计算渠道价格失败: " + e.getMessage()));
        }
    }

    // ==================== 渠道价格查询 API ====================

    /**
     * 获取渠道价格列表
     */
    @GetMapping("/prices")
    @StoreScoped
    public ResponseEntity<ApiResponse<List<ChannelPriceDTO>>> getChannelPrices(
            @RequestParam(required = false) Long roomTypeId,
            @RequestParam(required = false) Long channelId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            List<ChannelPriceDTO> prices = priceLabsService.getChannelPrices(roomTypeId, channelId, startDate, endDate);
            return ResponseEntity.ok(ApiResponse.success("获取渠道价格成功", prices));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("获取渠道价格失败: " + e.getMessage()));
        }
    }

    /**
     * 获取未同步到 OTA 的价格
     */
    @GetMapping("/prices/unsynced")
    @StoreScoped
    public ResponseEntity<ApiResponse<List<ChannelPriceDTO>>> getUnsyncedPrices(
            @RequestParam(required = false) Long channelId) {
        try {
            List<ChannelPriceDTO> prices = priceLabsService.getUnsyncedPrices(channelId);
            return ResponseEntity.ok(ApiResponse.success("获取未同步价格成功", prices));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("获取未同步价格失败: " + e.getMessage()));
        }
    }

    // ==================== 手动同步 API ====================

    /**
     * 手动触发同步
     */
    @PostMapping("/sync/manual")
    @StoreScoped
    public ResponseEntity<ApiResponse<Map<String, String>>> manualSync() {
        try {
            priceLabsSyncService.syncAll();
            // 稳定优先：额外拉取 PriceLabs 推荐价（get_prices），用于“立即同步”兜底刷新
            priceLabsSyncService.pullPricesForNextDaysPullSync(365);
            return ResponseEntity.ok(ApiResponse.success("同步任务已启动", Map.of("message", "同步成功")));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("同步失败: " + e.getMessage()));
        }
    }

    /**
     * 单房型同步：按房型拉取 PriceLabs 价格。
     */
    @PostMapping("/sync/room-type/{roomTypeId}")
    @StoreScoped
    public ResponseEntity<ApiResponse<Map<String, String>>> syncRoomType(
            @PathVariable Long roomTypeId,
            @RequestParam(required = false) Integer days
    ) {
        try {
            priceLabsSyncService.pullPricesForRoomType(roomTypeId, days);
            return ResponseEntity.ok(ApiResponse.success("房型价格同步已触发", Map.of("message", "已发起拉取")));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("房型价格同步失败: " + e.getMessage()));
        }
    }

    // ==================== 同步日志 API ====================

    /**
     * 获取同步日志（分页）
     */
    /**
     * 本地兜底生成渠道价格：当 PriceLabs 未回推价格时，按 room_prices > room_type_price_plans > room_type 计算 base_price 并生成 channel_prices。
     * 若某天已有 PriceLabs 数据（pricelabs_updated_at != null 且 base_price != null），则优先使用其 base_price。
     */
    @PostMapping("/channel-prices/fallback-generate")
    @StoreScoped
    public ResponseEntity<ApiResponse<Map<String, Object>>> generateFallbackChannelPrices(
            @RequestParam(required = false) Integer days) {
        try {
            Long storeId = StoreContextHolder.getContext().getStoreId();
            ChannelPriceFallbackService.GenerateResult result = channelPriceFallbackService.generate(storeId, LocalDate.now(), days);
            Map<String, Object> payload = Map.of(
                    "storeId", result.storeId(),
                    "startDate", result.startDate(),
                    "endDate", result.endDate(),
                    "channels", result.channels(),
                    "roomTypePricePlans", result.roomTypePricePlans(),
                    "created", result.created(),
                    "updated", result.updated(),
                    "skippedNoBasePrice", result.skippedNoBasePrice()
            );
            return ResponseEntity.ok(ApiResponse.success("本地兜底生成渠道价格完成", payload));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("本地兜底生成渠道价格失败: " + e.getMessage()));
        }
    }

    @GetMapping("/sync-logs")
    @StoreScoped
    public ResponseEntity<ApiResponse<Page<PriceLabsSyncLogDTO>>> getSyncLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Page<PriceLabsSyncLogDTO> logs = priceLabsService.getSyncLogs(page, size);
            return ResponseEntity.ok(ApiResponse.success("获取同步日志成功", logs));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("获取同步日志失败: " + e.getMessage()));
        }
    }

    /**
     * 获取最近的同步日志
     */
    @GetMapping("/sync-logs/recent")
    @StoreScoped
    public ResponseEntity<ApiResponse<List<PriceLabsSyncLogDTO>>> getRecentSyncLogs(
            @RequestParam(defaultValue = "10") int limit) {
        try {
            List<PriceLabsSyncLogDTO> logs = priceLabsService.getRecentSyncLogs(limit);
            return ResponseEntity.ok(ApiResponse.success("获取最近同步日志成功", logs));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("获取最近同步日志失败: " + e.getMessage()));
        }
    }

    // ==================== 内部请求类 ====================

    /**
     * 渠道价格调整请求
     */
    // ==================== Outbound: reservations/status ====================

    /**
     * PMS -> PriceLabs: push reservations (required for certification).
     * PriceLabs 要求：实现后需要推送 2020-01-01 至今/未来所有 reservations。
     */
    @PostMapping("/reservations/push")
    @StoreScoped
    public ResponseEntity<ApiResponse<Map<String, Object>>> pushReservationsToPriceLabs(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        try {
            Long storeId = StoreContextHolder.getContext().getStoreId();
            PriceLabsReservationSyncService.PushSummary summary =
                    priceLabsReservationSyncService.pushReservationsForDateRange(storeId, startDate, endDate);

            Map<String, Object> payload = Map.of(
                    "listingCount", summary.listingCount(),
                    "reservationCount", summary.reservationCount(),
                    "successCount", summary.successCount(),
                    "failureCount", summary.failureCount(),
                    "failures", summary.failureMessages()
            );
            return ResponseEntity.ok(ApiResponse.success("reservations 推送完成", payload));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("reservations 推送失败: " + e.getMessage()));
        }
    }

    public static class StatusQueryRequest {
        private List<PriceLabsApiClient.StatusReq> statuses;

        public List<PriceLabsApiClient.StatusReq> getStatuses() {
            return statuses;
        }

        public void setStatuses(List<PriceLabsApiClient.StatusReq> statuses) {
            this.statuses = statuses;
        }
    }
    public static class StatusQueryByRoomTypeRequest {
        private Long roomTypeId;
        private LocalDate startDate;
        private LocalDate endDate;

        public Long getRoomTypeId() {
            return roomTypeId;
        }

        public void setRoomTypeId(Long roomTypeId) {
            this.roomTypeId = roomTypeId;
        }

        public LocalDate getStartDate() {
            return startDate;
        }

        public void setStartDate(LocalDate startDate) {
            this.startDate = startDate;
        }

        public LocalDate getEndDate() {
            return endDate;
        }

        public void setEndDate(LocalDate endDate) {
            this.endDate = endDate;
        }
    }

    /**
     * PMS -> PriceLabs: query status for debugging/certification.
     * Body example: { "statuses": [ { "id": "...", "type": "listing" } ] }
     */
    @PostMapping("/status/query")
    @StoreScoped
    public ResponseEntity<ApiResponse<PriceLabsApiClient.PriceLabsResponse>> queryPriceLabsStatus(
            @RequestBody StatusQueryRequest request
    ) {
        try {
            Long storeId = StoreContextHolder.getContext().getStoreId();
            List<PriceLabsApiClient.StatusReq> statuses = request != null ? request.getStatuses() : null;
            PriceLabsApiClient.PriceLabsResponse res = priceLabsStatusService.queryStatus(storeId, statuses);
            return ResponseEntity.ok(ApiResponse.success("status 查询完成", res));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("status 查询失败: " + e.getMessage()));
        }
    }
    /**
     * PMS -> PriceLabs: query reservation status by room type and date range.
     */
    @PostMapping("/status/query-by-room-type")
    @StoreScoped
    public ResponseEntity<ApiResponse<PriceLabsApiClient.PriceLabsResponse>> queryPriceLabsStatusByRoomType(
            @RequestBody StatusQueryByRoomTypeRequest request
    ) {
        try {
            if (request == null) {
                throw new IllegalArgumentException("request 不能为空");
            }
            Long storeId = StoreContextHolder.getContext().getStoreId();
            PriceLabsApiClient.PriceLabsResponse res = priceLabsStatusService.queryReservationStatusByRoomType(
                    storeId,
                    request.getRoomTypeId(),
                    request.getStartDate(),
                    request.getEndDate()
            );
            return ResponseEntity.ok(ApiResponse.success("status 查询完成", res));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("status 查询失败: " + e.getMessage()));
        }
    }

    public static class ChannelPriceAdjustmentRequest {
        private PriceAdjustmentType adjustmentType;
        private BigDecimal adjustmentValue;
        private Boolean autoSyncPrice;

        public PriceAdjustmentType getAdjustmentType() {
            return adjustmentType;
        }

        public void setAdjustmentType(PriceAdjustmentType adjustmentType) {
            this.adjustmentType = adjustmentType;
        }

        public BigDecimal getAdjustmentValue() {
            return adjustmentValue;
        }

        public void setAdjustmentValue(BigDecimal adjustmentValue) {
            this.adjustmentValue = adjustmentValue;
        }

        public Boolean getAutoSyncPrice() {
            return autoSyncPrice;
        }

        public void setAutoSyncPrice(Boolean autoSyncPrice) {
            this.autoSyncPrice = autoSyncPrice;
        }
    }
}
