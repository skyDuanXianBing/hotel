package server.demo.controller;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import server.demo.annotation.StoreScoped;
import server.demo.dto.ApiResponse;
import server.demo.dto.push.PushDeviceRegisterRequest;
import server.demo.dto.push.PushDeviceUnregisterRequest;
import server.demo.service.PushDeviceTokenService;
import server.demo.util.StoreContextUtils;

import server.demo.i18n.ApiMessages;

@RestController
@RequestMapping("/api/v1/push/devices")
@StoreScoped
public class PushDeviceController {

    private final PushDeviceTokenService pushDeviceTokenService;

    public PushDeviceController(PushDeviceTokenService pushDeviceTokenService) {
        this.pushDeviceTokenService = pushDeviceTokenService;
    }

    @PostMapping
    public ApiResponse<Void> register(@Valid @RequestBody PushDeviceRegisterRequest request) {
        pushDeviceTokenService.register(
                StoreContextUtils.requireUserId(),
                StoreContextUtils.requireStoreId(),
                request.getPlatform(),
                request.getDeviceToken(),
                request.getLocale()
        );
        return ApiResponse.success(ApiMessages.get("api.t.26a4ff97ecfc"), null);
    }

    @DeleteMapping
    public ApiResponse<Void> unregister(@Valid @RequestBody PushDeviceUnregisterRequest request) {
        pushDeviceTokenService.unregister(request.getDeviceToken());
        return ApiResponse.success(ApiMessages.get("api.t.c0e364a14bba"), null);
    }
}
