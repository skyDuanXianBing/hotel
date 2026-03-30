package server.demo.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import server.demo.annotation.StoreScoped;
import server.demo.context.StoreContextHolder;
import server.demo.dto.ApiResponse;
import server.demo.service.OtaReservationSyncService;
import server.demo.service.OtaSyncService;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/ota-sync")
public class OtaSyncController {

    private final OtaSyncService otaSyncService;
    private final OtaReservationSyncService otaReservationSyncService;

    public OtaSyncController(OtaSyncService otaSyncService, OtaReservationSyncService otaReservationSyncService) {
        this.otaSyncService = otaSyncService;
        this.otaReservationSyncService = otaReservationSyncService;
    }

    /**
     * 手动触发：将 PriceLabs 计算后的渠道价（channel_prices.channel_price）发布到 Su Channel Manager
     * 默认：未来 365 天（最大 500 天），仅 Airbnb(244) + Booking(19)
     */
    @PostMapping("/manual")
    @StoreScoped
    public ResponseEntity<ApiResponse<OtaSyncService.OtaSyncResult>> manualSync(
            @RequestParam(required = false) Integer days
    ) {
        try {
            Long storeId = StoreContextHolder.getContext().getStoreId();
            OtaSyncService.OtaSyncResult result = otaSyncService.syncStorePricesToSu(storeId, LocalDate.now(), days);
            return ResponseEntity.ok(ApiResponse.success("同步到 OTA 已触发", result));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error("同步到 OTA 失败: " + e.getMessage()));
        }
    }

    /**
     * 手动拉取 OTA 预订（Su Reservation API）并写入 PMS 预订表；成功后会 Ack Reservation_notif
     * 当前仅支持 Airbnb(244) + Booking(19)
     */
    @PostMapping("/reservations/manual")
    @StoreScoped
    public ResponseEntity<ApiResponse<OtaReservationSyncService.ReservationSyncResult>> manualPullReservations() {
        try {
            Long storeId = StoreContextHolder.getContext().getStoreId();
            OtaReservationSyncService.ReservationSyncResult result = otaReservationSyncService.syncStoreReservations(storeId);
            return ResponseEntity.ok(ApiResponse.success("拉取OTA预订成功", result));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error("拉取OTA预订失败: " + e.getMessage()));
        }
    }
}
