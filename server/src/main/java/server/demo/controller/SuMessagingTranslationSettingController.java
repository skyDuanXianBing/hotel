package server.demo.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import server.demo.annotation.StoreScoped;
import server.demo.dto.ApiResponse;
import server.demo.dto.SuMessagingTranslationSettingDTO;
import server.demo.service.SuMessagingTranslationSettingService;
import server.demo.util.StoreContextUtils;

@RestController
@RequestMapping("/api/v1/su-messaging/translation-settings")
public class SuMessagingTranslationSettingController {

    private final SuMessagingTranslationSettingService settingService;

    public SuMessagingTranslationSettingController(SuMessagingTranslationSettingService settingService) {
        this.settingService = settingService;
    }

    @GetMapping
    @StoreScoped
    public ResponseEntity<ApiResponse<SuMessagingTranslationSettingDTO>> getTranslationSetting() {
        try {
            Long userId = StoreContextUtils.requireUserId();
            return ResponseEntity.ok(ApiResponse.success(
                    "获取翻译设置成功",
                    settingService.get(userId)
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error("获取翻译设置失败: " + e.getMessage()));
        }
    }

    @PutMapping
    @StoreScoped
    public ResponseEntity<ApiResponse<SuMessagingTranslationSettingDTO>> updateTranslationSetting(
            @Valid @RequestBody SuMessagingTranslationSettingDTO request
    ) {
        try {
            Long userId = StoreContextUtils.requireUserId();
            return ResponseEntity.ok(ApiResponse.success(
                    "更新翻译设置成功",
                    settingService.update(userId, request)
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error("更新翻译设置失败: " + e.getMessage()));
        }
    }
}
