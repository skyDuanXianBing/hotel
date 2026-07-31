package server.demo.controller;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import server.demo.annotation.StoreScoped;
import server.demo.dto.ApiResponse;
import server.demo.dto.SmartLockBindingDTO;
import server.demo.dto.SmartLockConfirmationDTO;
import server.demo.dto.SmartLockDeviceDTO;
import server.demo.dto.SmartLockIntegrationDTO;
import server.demo.dto.SmartLockPasscodeDTO;
import server.demo.dto.SmartLockRequests;
import server.demo.dto.SmartLockRoomDTO;
import server.demo.dto.SmartLockStatusDTO;
import server.demo.dto.SmartLockTaskDTO;
import server.demo.dto.SmartLockTestResultDTO;
import server.demo.enums.SmartLockProvider;
import server.demo.service.SmartLockService;
import server.demo.util.SmartLockMaskingUtils;

import java.util.List;
import java.util.Map;

import server.demo.i18n.ApiMessages;
@RestController
@RequestMapping("/api/v1/smart-locks")
@StoreScoped
public class SmartLockController {
    private final SmartLockService smartLockService;

    public SmartLockController(SmartLockService smartLockService) {
        this.smartLockService = smartLockService;
    }

    @GetMapping("/integrations")
    public ApiResponse<List<SmartLockIntegrationDTO>> listIntegrations() {
        try {
            return ApiResponse.success(ApiMessages.get("api.t.9f641d4f49ba"), smartLockService.listIntegrations());
        } catch (RuntimeException ex) {
            return errorResponse(ApiMessages.get("api.t.5d6d32237900"), ex);
        }
    }

    @PostMapping("/integrations")
    public ApiResponse<SmartLockIntegrationDTO> saveIntegration(
            @RequestBody SmartLockRequests.UpsertIntegrationRequest request
    ) {
        try {
            return ApiResponse.success(ApiMessages.get("api.t.b0c98b2459df"), smartLockService.saveIntegration(request));
        } catch (RuntimeException ex) {
            return errorResponse(ApiMessages.get("api.t.78d7e7a45aa8"), ex);
        }
    }

    @PutMapping("/integrations/{id}")
    public ApiResponse<SmartLockIntegrationDTO> updateIntegration(
            @PathVariable Long id,
            @RequestBody SmartLockRequests.UpsertIntegrationRequest request
    ) {
        try {
            return ApiResponse.success(ApiMessages.get("api.t.475242bee3d4"), smartLockService.updateIntegration(id, request));
        } catch (RuntimeException ex) {
            return errorResponse(ApiMessages.get("api.t.eff9e7869ab0"), ex);
        }
    }

    @PostMapping("/integrations/{id}/test")
    public ApiResponse<SmartLockTestResultDTO> testIntegration(@PathVariable Long id) {
        try {
            return ApiResponse.success(ApiMessages.get("api.t.6b519e2a4d15"), smartLockService.testIntegration(id));
        } catch (RuntimeException ex) {
            return errorResponse(ApiMessages.get("api.t.02d915e87301"), ex);
        }
    }

    @PostMapping("/integrations/{id}/refresh-token")
    public ApiResponse<SmartLockIntegrationDTO> refreshToken(@PathVariable Long id) {
        try {
            return ApiResponse.success(ApiMessages.get("api.t.443158ea90e0"), smartLockService.refreshToken(id));
        } catch (RuntimeException ex) {
            return errorResponse(ApiMessages.get("api.t.8a0ab8680f9b"), ex);
        }
    }

    @PostMapping("/integrations/{id}/devices/sync")
    public ApiResponse<List<SmartLockDeviceDTO>> syncDevices(@PathVariable Long id) {
        try {
            return ApiResponse.success(ApiMessages.get("api.t.816edcc7423c"), smartLockService.syncDevices(id));
        } catch (RuntimeException ex) {
            return errorResponse(ApiMessages.get("api.t.b5ebc2b8adda"), ex);
        }
    }

    @GetMapping("/devices")
    public ApiResponse<List<SmartLockDeviceDTO>> listDevices(
            @RequestParam(required = false) SmartLockProvider provider
    ) {
        try {
            return ApiResponse.success(ApiMessages.get("api.t.736210f82335"), smartLockService.listDevices(provider));
        } catch (RuntimeException ex) {
            return errorResponse(ApiMessages.get("api.t.4b54438b8eee"), ex);
        }
    }

    @GetMapping("/rooms")
    public ApiResponse<List<SmartLockRoomDTO>> listRooms(
            @RequestParam(required = false) SmartLockProvider provider,
            @RequestParam(required = false) Long roomTypeId
    ) {
        try {
            return ApiResponse.success(ApiMessages.get("api.t.94906216bcbd"), smartLockService.listRooms(provider, roomTypeId));
        } catch (RuntimeException ex) {
            return errorResponse(ApiMessages.get("api.t.1b908da0612c"), ex);
        }
    }

    @PostMapping("/bindings")
    public ApiResponse<SmartLockBindingDTO> createBinding(
            @RequestBody SmartLockRequests.CreateBindingRequest request
    ) {
        try {
            return ApiResponse.success(ApiMessages.get("api.t.aa12a9ce4700"), smartLockService.createBinding(request));
        } catch (RuntimeException ex) {
            return errorResponse(ApiMessages.get("api.t.8b56fc209f77"), ex);
        }
    }

    @DeleteMapping("/bindings/{id}")
    public ApiResponse<Void> deleteBinding(@PathVariable Long id) {
        try {
            smartLockService.deleteBinding(id);
            return ApiResponse.success(ApiMessages.get("api.t.0ae5fdb8ea4b"), null);
        } catch (RuntimeException ex) {
            return errorResponse(ApiMessages.get("api.t.012ad78eac7d"), ex);
        }
    }

    @GetMapping("/rooms/{roomId}/status")
    public ApiResponse<SmartLockStatusDTO> getRoomStatus(@PathVariable Long roomId) {
        try {
            return ApiResponse.success(ApiMessages.get("api.t.bc9e7c5bb633"), smartLockService.getRoomStatus(roomId));
        } catch (RuntimeException ex) {
            return errorResponse(ApiMessages.get("api.t.204157e63e63"), ex);
        }
    }

    @PostMapping("/rooms/{roomId}/status/refresh")
    public ApiResponse<SmartLockStatusDTO> refreshRoomStatus(@PathVariable Long roomId) {
        try {
            return ApiResponse.success(ApiMessages.get("api.t.813821e50cf6"), smartLockService.refreshRoomStatus(roomId));
        } catch (RuntimeException ex) {
            return errorResponse(ApiMessages.get("api.t.b492d296f349"), ex);
        }
    }

    @PostMapping("/rooms/{roomId}/confirmations")
    public ApiResponse<SmartLockConfirmationDTO> createConfirmation(
            @PathVariable Long roomId,
            @RequestBody SmartLockRequests.ConfirmationRequest request
    ) {
        try {
            return ApiResponse.success(ApiMessages.get("api.t.2a507b5c2b4c"), smartLockService.createConfirmation(roomId, request));
        } catch (RuntimeException ex) {
            return errorResponse(ApiMessages.get("api.t.b52ffdfd50d2"), ex);
        }
    }

    @PostMapping("/rooms/{roomId}/unlock")
    public ApiResponse<SmartLockTaskDTO> unlock(
            @PathVariable Long roomId,
            @RequestBody SmartLockRequests.LockOperationRequest request
    ) {
        try {
            return ApiResponse.success(ApiMessages.get("api.t.58548bc61880"), smartLockService.unlock(roomId, request));
        } catch (RuntimeException ex) {
            return errorResponse(ApiMessages.get("api.t.a7a83eb843d4"), ex);
        }
    }

    @PostMapping("/rooms/{roomId}/lock")
    public ApiResponse<SmartLockTaskDTO> lock(
            @PathVariable Long roomId,
            @RequestBody SmartLockRequests.LockOperationRequest request
    ) {
        try {
            return ApiResponse.success(ApiMessages.get("api.t.768fffe29123"), smartLockService.lock(roomId, request));
        } catch (RuntimeException ex) {
            return errorResponse(ApiMessages.get("api.t.649a9b208f46"), ex);
        }
    }

    @GetMapping("/rooms/{roomId}/passcodes")
    public ApiResponse<List<SmartLockPasscodeDTO>> listPasscodes(@PathVariable Long roomId) {
        try {
            return ApiResponse.success(ApiMessages.get("api.t.814946c40d9a"), smartLockService.listPasscodes(roomId));
        } catch (RuntimeException ex) {
            return errorResponse(ApiMessages.get("api.t.5eb64b888a7a"), ex);
        }
    }

    @PostMapping("/rooms/{roomId}/passcodes")
    public ApiResponse<SmartLockPasscodeDTO> createPasscode(
            @PathVariable Long roomId,
            @RequestBody SmartLockRequests.CreatePasscodeRequest request
    ) {
        try {
            return ApiResponse.success(ApiMessages.get("api.t.de4b99781498"), smartLockService.createPasscode(roomId, request));
        } catch (RuntimeException ex) {
            return errorResponse(ApiMessages.get("api.t.3f6d9a83db57"), ex);
        }
    }

    @DeleteMapping("/passcodes/{recordId}")
    public ApiResponse<SmartLockPasscodeDTO> deletePasscode(@PathVariable Long recordId) {
        try {
            return ApiResponse.success(ApiMessages.get("api.t.1d3e90954742"), smartLockService.deletePasscode(recordId));
        } catch (RuntimeException ex) {
            return errorResponse(ApiMessages.get("api.t.37bcc1e62755"), ex);
        }
    }

    @GetMapping("/passcode-tasks/{taskId}")
    public ApiResponse<SmartLockTaskDTO> getTask(@PathVariable Long taskId) {
        try {
            return ApiResponse.success(ApiMessages.get("api.t.f87d46d31348"), smartLockService.getTask(taskId));
        } catch (RuntimeException ex) {
            return errorResponse(ApiMessages.get("api.t.b7bf075865c7"), ex);
        }
    }

    private <T> ApiResponse<T> errorResponse(String prefix, RuntimeException ex) {
        return ApiResponse.error(prefix + ": " + SmartLockMaskingUtils.safeExceptionMessage(ex));
    }
}

@RestController
@RequestMapping("/api/public/smart-locks")
class SmartLockPublicWebhookController {
    private final SmartLockService smartLockService;

    SmartLockPublicWebhookController(SmartLockService smartLockService) {
        this.smartLockService = smartLockService;
    }

    @PostMapping("/switchbot/webhooks/{token}")
    public ApiResponse<Map<String, Object>> handleSwitchBotWebhook(
            @PathVariable String token,
            @RequestBody(required = false) Map<String, Object> payload
    ) {
        try {
            return ApiResponse.success(
                    ApiMessages.get("api.t.c2dd8c254aaf"),
                    smartLockService.handleSwitchBotWebhook(token, payload)
            );
        } catch (RuntimeException ex) {
            return ApiResponse.error(ApiMessages.get("api.t.c800720831d6") + SmartLockMaskingUtils.safeExceptionMessage(ex));
        }
    }
}
