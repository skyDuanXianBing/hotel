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

import server.demo.i18n.ApiMessages;
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
                    ApiMessages.get("api.t.d607b7e2141e"),
                    settingService.get(userId)
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error(ApiMessages.get("api.t.665c082409ab") + e.getMessage()));
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
                    ApiMessages.get("api.t.5e1ebdbe559e"),
                    settingService.update(userId, request)
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error(ApiMessages.get("api.t.853481c7105e") + e.getMessage()));
        }
    }
}
