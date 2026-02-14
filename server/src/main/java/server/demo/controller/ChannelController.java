package server.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import server.demo.annotation.RequirePermission;
import server.demo.annotation.StoreScoped;
import server.demo.dto.ChannelDTO;
import server.demo.dto.CreateChannelRequest;
import server.demo.service.ChannelService;
import server.demo.dto.ApiResponse;
import server.demo.enums.PermissionAction;
import server.demo.enums.PermissionModule;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/channels")
@CrossOrigin(origins = {"http://localhost:8091", "http://127.0.0.1:8091"}, allowCredentials = "true")
@StoreScoped
public class ChannelController {

    @Autowired
    private ChannelService channelService;

    @GetMapping
    @RequirePermission(module = PermissionModule.CHANNEL, action = PermissionAction.VIEW_CHANNELS)
    public ResponseEntity<ApiResponse<List<ChannelDTO>>> getAllChannels() {
        try {
            List<ChannelDTO> channels = channelService.getAllChannels();
            return ResponseEntity.ok(ApiResponse.success("获取渠道列表成功", channels));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("获取渠道列表失败: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    @RequirePermission(module = PermissionModule.CHANNEL, action = PermissionAction.VIEW_CHANNELS)
    public ResponseEntity<ApiResponse<ChannelDTO>> getChannelById(@PathVariable Long id) {
        try {
            return channelService.getChannelById(id)
                    .map(channel -> ResponseEntity.ok(ApiResponse.success("获取渠道成功", channel)))
                    .orElse(ResponseEntity.status(404)
                            .body(ApiResponse.error("渠道不存在")));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error( "获取渠道失败: " + e.getMessage()));
        }
    }

    @PostMapping
    @RequirePermission(module = PermissionModule.CHANNEL, action = PermissionAction.MANAGE_CHANNELS)
    public ResponseEntity<ApiResponse<ChannelDTO>> createChannel(@Valid @RequestBody CreateChannelRequest request) {
        try {
            ChannelDTO createdChannel = channelService.createChannel(request);
            return ResponseEntity.ok(ApiResponse.success("创建渠道成功", createdChannel));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error( "创建渠道失败: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @RequirePermission(module = PermissionModule.CHANNEL, action = PermissionAction.MANAGE_CHANNELS)
    public ResponseEntity<ApiResponse<ChannelDTO>> updateChannel(
            @PathVariable Long id,
            @Valid @RequestBody CreateChannelRequest request) {
        try {
            return channelService.updateChannel(id, request)
                    .map(channel -> ResponseEntity.ok(ApiResponse.success("更新渠道成功", channel)))
                    .orElse(ResponseEntity.status(404)
                            .body(ApiResponse.error("渠道不存在")));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error( "更新渠道失败: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @RequirePermission(module = PermissionModule.CHANNEL, action = PermissionAction.MANAGE_CHANNELS)
    public ResponseEntity<ApiResponse<Void>> deleteChannel(@PathVariable Long id) {
        try {
            if (channelService.deleteChannel(id)) {
                return ResponseEntity.ok(ApiResponse.success("删除渠道成功", null));
            } else {
                return ResponseEntity.status(404)
                        .body(ApiResponse.error("渠道不存在"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error( "删除渠道失败: " + e.getMessage()));
        }
    }

    @PatchMapping("/{id}/status")
    @RequirePermission(module = PermissionModule.CHANNEL, action = PermissionAction.MANAGE_CHANNELS)
    public ResponseEntity<ApiResponse<ChannelDTO>> toggleChannelStatus(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> request) {
        try {
            Boolean enabled = request.get("enabled");
            return channelService.toggleChannelStatus(id, enabled)
                    .map(channel -> ResponseEntity.ok(ApiResponse.success("更新渠道状态成功", channel)))
                    .orElse(ResponseEntity.status(404)
                            .body(ApiResponse.error("渠道不存在")));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error( "更新渠道状态失败: " + e.getMessage()));
        }
    }
}
