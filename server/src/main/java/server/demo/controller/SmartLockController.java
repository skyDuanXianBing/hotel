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
            return ApiResponse.success("获取门锁集成配置成功", smartLockService.listIntegrations());
        } catch (RuntimeException ex) {
            return errorResponse("获取门锁集成配置失败", ex);
        }
    }

    @PostMapping("/integrations")
    public ApiResponse<SmartLockIntegrationDTO> saveIntegration(
            @RequestBody SmartLockRequests.UpsertIntegrationRequest request
    ) {
        try {
            return ApiResponse.success("保存门锁集成配置成功", smartLockService.saveIntegration(request));
        } catch (RuntimeException ex) {
            return errorResponse("保存门锁集成配置失败", ex);
        }
    }

    @PutMapping("/integrations/{id}")
    public ApiResponse<SmartLockIntegrationDTO> updateIntegration(
            @PathVariable Long id,
            @RequestBody SmartLockRequests.UpsertIntegrationRequest request
    ) {
        try {
            return ApiResponse.success("更新门锁集成配置成功", smartLockService.updateIntegration(id, request));
        } catch (RuntimeException ex) {
            return errorResponse("更新门锁集成配置失败", ex);
        }
    }

    @PostMapping("/integrations/{id}/test")
    public ApiResponse<SmartLockTestResultDTO> testIntegration(@PathVariable Long id) {
        try {
            return ApiResponse.success("门锁连接测试完成", smartLockService.testIntegration(id));
        } catch (RuntimeException ex) {
            return errorResponse("门锁连接测试失败", ex);
        }
    }

    @PostMapping("/integrations/{id}/refresh-token")
    public ApiResponse<SmartLockIntegrationDTO> refreshToken(@PathVariable Long id) {
        try {
            return ApiResponse.success("刷新门锁访问令牌成功", smartLockService.refreshToken(id));
        } catch (RuntimeException ex) {
            return errorResponse("刷新门锁访问令牌失败", ex);
        }
    }

    @PostMapping("/integrations/{id}/devices/sync")
    public ApiResponse<List<SmartLockDeviceDTO>> syncDevices(@PathVariable Long id) {
        try {
            return ApiResponse.success("同步门锁设备成功", smartLockService.syncDevices(id));
        } catch (RuntimeException ex) {
            return errorResponse("同步门锁设备失败", ex);
        }
    }

    @GetMapping("/devices")
    public ApiResponse<List<SmartLockDeviceDTO>> listDevices(
            @RequestParam(required = false) SmartLockProvider provider
    ) {
        try {
            return ApiResponse.success("获取门锁设备成功", smartLockService.listDevices(provider));
        } catch (RuntimeException ex) {
            return errorResponse("获取门锁设备失败", ex);
        }
    }

    @GetMapping("/rooms")
    public ApiResponse<List<SmartLockRoomDTO>> listRooms(
            @RequestParam(required = false) SmartLockProvider provider,
            @RequestParam(required = false) Long roomTypeId
    ) {
        try {
            return ApiResponse.success("获取房间门锁绑定状态成功", smartLockService.listRooms(provider, roomTypeId));
        } catch (RuntimeException ex) {
            return errorResponse("获取房间门锁绑定状态失败", ex);
        }
    }

    @PostMapping("/bindings")
    public ApiResponse<SmartLockBindingDTO> createBinding(
            @RequestBody SmartLockRequests.CreateBindingRequest request
    ) {
        try {
            return ApiResponse.success("绑定房间门锁成功", smartLockService.createBinding(request));
        } catch (RuntimeException ex) {
            return errorResponse("绑定房间门锁失败", ex);
        }
    }

    @DeleteMapping("/bindings/{id}")
    public ApiResponse<Void> deleteBinding(@PathVariable Long id) {
        try {
            smartLockService.deleteBinding(id);
            return ApiResponse.success("解绑房间门锁成功", null);
        } catch (RuntimeException ex) {
            return errorResponse("解绑房间门锁失败", ex);
        }
    }

    @GetMapping("/rooms/{roomId}/status")
    public ApiResponse<SmartLockStatusDTO> getRoomStatus(@PathVariable Long roomId) {
        try {
            return ApiResponse.success("获取门锁状态成功", smartLockService.getRoomStatus(roomId));
        } catch (RuntimeException ex) {
            return errorResponse("获取门锁状态失败", ex);
        }
    }

    @PostMapping("/rooms/{roomId}/status/refresh")
    public ApiResponse<SmartLockStatusDTO> refreshRoomStatus(@PathVariable Long roomId) {
        try {
            return ApiResponse.success("刷新门锁状态成功", smartLockService.refreshRoomStatus(roomId));
        } catch (RuntimeException ex) {
            return errorResponse("刷新门锁状态失败", ex);
        }
    }

    @PostMapping("/rooms/{roomId}/confirmations")
    public ApiResponse<SmartLockConfirmationDTO> createConfirmation(
            @PathVariable Long roomId,
            @RequestBody SmartLockRequests.ConfirmationRequest request
    ) {
        try {
            return ApiResponse.success("门锁操作确认已生成", smartLockService.createConfirmation(roomId, request));
        } catch (RuntimeException ex) {
            return errorResponse("生成门锁操作确认失败", ex);
        }
    }

    @PostMapping("/rooms/{roomId}/unlock")
    public ApiResponse<SmartLockTaskDTO> unlock(
            @PathVariable Long roomId,
            @RequestBody SmartLockRequests.LockOperationRequest request
    ) {
        try {
            return ApiResponse.success("门锁开锁任务已记录", smartLockService.unlock(roomId, request));
        } catch (RuntimeException ex) {
            return errorResponse("门锁开锁失败", ex);
        }
    }

    @PostMapping("/rooms/{roomId}/lock")
    public ApiResponse<SmartLockTaskDTO> lock(
            @PathVariable Long roomId,
            @RequestBody SmartLockRequests.LockOperationRequest request
    ) {
        try {
            return ApiResponse.success("门锁上锁任务已记录", smartLockService.lock(roomId, request));
        } catch (RuntimeException ex) {
            return errorResponse("门锁上锁失败", ex);
        }
    }

    @GetMapping("/rooms/{roomId}/passcodes")
    public ApiResponse<List<SmartLockPasscodeDTO>> listPasscodes(@PathVariable Long roomId) {
        try {
            return ApiResponse.success("获取门锁密码记录成功", smartLockService.listPasscodes(roomId));
        } catch (RuntimeException ex) {
            return errorResponse("获取门锁密码记录失败", ex);
        }
    }

    @PostMapping("/rooms/{roomId}/passcodes")
    public ApiResponse<SmartLockPasscodeDTO> createPasscode(
            @PathVariable Long roomId,
            @RequestBody SmartLockRequests.CreatePasscodeRequest request
    ) {
        try {
            return ApiResponse.success("创建门锁密码任务已记录", smartLockService.createPasscode(roomId, request));
        } catch (RuntimeException ex) {
            return errorResponse("创建门锁密码失败", ex);
        }
    }

    @DeleteMapping("/passcodes/{recordId}")
    public ApiResponse<SmartLockPasscodeDTO> deletePasscode(@PathVariable Long recordId) {
        try {
            return ApiResponse.success("删除门锁密码任务已记录", smartLockService.deletePasscode(recordId));
        } catch (RuntimeException ex) {
            return errorResponse("删除门锁密码失败", ex);
        }
    }

    @GetMapping("/passcode-tasks/{taskId}")
    public ApiResponse<SmartLockTaskDTO> getTask(@PathVariable Long taskId) {
        try {
            return ApiResponse.success("获取门锁任务成功", smartLockService.getTask(taskId));
        } catch (RuntimeException ex) {
            return errorResponse("获取门锁任务失败", ex);
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
                    "SwitchBot webhook 已处理",
                    smartLockService.handleSwitchBotWebhook(token, payload)
            );
        } catch (RuntimeException ex) {
            return ApiResponse.error("SwitchBot webhook 处理失败: " + SmartLockMaskingUtils.safeExceptionMessage(ex));
        }
    }
}
