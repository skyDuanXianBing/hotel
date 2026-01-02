package server.demo.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import server.demo.annotation.StoreScoped;
import server.demo.config.SuWebhookConfig;
import server.demo.context.StoreContextHolder;
import server.demo.dto.ApiResponse;
import server.demo.entity.Store;
import server.demo.repository.StoreRepository;
import server.demo.util.SuHotelIdUtil;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/su/webhook-config")
@StoreScoped
public class SuWebhookConfigController {

    private final SuWebhookConfig suWebhookConfig;
    private final StoreRepository storeRepository;

    public SuWebhookConfigController(SuWebhookConfig suWebhookConfig, StoreRepository storeRepository) {
        this.suWebhookConfig = suWebhookConfig;
        this.storeRepository = storeRepository;
    }

    @GetMapping("/reservation-notif-url")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getReservationNotifUrl() {
        Long storeId = StoreContextHolder.getContext().getStoreId();
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("门店不存在: " + storeId));

        String hotelId = SuHotelIdUtil.normalize(store.getSuHotelId());
        if (hotelId == null) {
            hotelId = SuHotelIdUtil.buildDefault(storeId);
        }

        Map<String, Object> data = Map.of(
                "storeId", storeId,
                "hotelId", hotelId,
                "serverBaseUrl", suWebhookConfig.getServerBaseUrl(),
                "webhookUrl", suWebhookConfig.getReservationNotifWebhookUrl(hotelId)
        );

        return ResponseEntity.ok(ApiResponse.success("获取 Su Webhook 回调地址成功", data));
    }
}

