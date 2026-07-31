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

import server.demo.i18n.ApiMessages;
@RestController
@RequestMapping("/api/v1/channels")
@StoreScoped
public class ChannelController {

    @Autowired
    private ChannelService channelService;

    @GetMapping
    @RequirePermission(module = PermissionModule.CHANNEL, action = PermissionAction.VIEW_CHANNELS)
    public ResponseEntity<ApiResponse<List<ChannelDTO>>> getAllChannels() {
        try {
            List<ChannelDTO> channels = channelService.getAllChannels();
            return ResponseEntity.ok(ApiResponse.success(ApiMessages.get("api.t.0cba72c3ff41"), channels));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error(ApiMessages.get("api.t.f42c8d874dbe") + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    @RequirePermission(module = PermissionModule.CHANNEL, action = PermissionAction.VIEW_CHANNELS)
    public ResponseEntity<ApiResponse<ChannelDTO>> getChannelById(@PathVariable Long id) {
        try {
            return channelService.getChannelById(id)
                    .map(channel -> ResponseEntity.ok(ApiResponse.success(ApiMessages.get("api.t.32abe68fd281"), channel)))
                    .orElse(ResponseEntity.status(404)
                            .body(ApiResponse.error(ApiMessages.get("api.t.11e0759cc797"))));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error( ApiMessages.get("api.t.d9aca65f6dea") + e.getMessage()));
        }
    }

    @PostMapping
    @RequirePermission(module = PermissionModule.CHANNEL, action = PermissionAction.MANAGE_CHANNELS)
    public ResponseEntity<ApiResponse<ChannelDTO>> createChannel(@Valid @RequestBody CreateChannelRequest request) {
        try {
            ChannelDTO createdChannel = channelService.createChannel(request);
            return ResponseEntity.ok(ApiResponse.success(ApiMessages.get("api.t.7f9bf6dd396e"), createdChannel));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error( ApiMessages.get("api.t.f98123cd4382") + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @RequirePermission(module = PermissionModule.CHANNEL, action = PermissionAction.MANAGE_CHANNELS)
    public ResponseEntity<ApiResponse<ChannelDTO>> updateChannel(
            @PathVariable Long id,
            @Valid @RequestBody CreateChannelRequest request) {
        try {
            return channelService.updateChannel(id, request)
                    .map(channel -> ResponseEntity.ok(ApiResponse.success(ApiMessages.get("api.t.4da2ad99162e"), channel)))
                    .orElse(ResponseEntity.status(404)
                            .body(ApiResponse.error(ApiMessages.get("api.t.11e0759cc797"))));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error( ApiMessages.get("api.t.309f3a3d747c") + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @RequirePermission(module = PermissionModule.CHANNEL, action = PermissionAction.MANAGE_CHANNELS)
    public ResponseEntity<ApiResponse<Void>> deleteChannel(@PathVariable Long id) {
        try {
            if (channelService.deleteChannel(id)) {
                return ResponseEntity.ok(ApiResponse.success(ApiMessages.get("api.t.d6fbc763eef4"), null));
            } else {
                return ResponseEntity.status(404)
                        .body(ApiResponse.error(ApiMessages.get("api.t.11e0759cc797")));
            }
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error( ApiMessages.get("api.t.bb47d1c106f6") + e.getMessage()));
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
                    .map(channel -> ResponseEntity.ok(ApiResponse.success(ApiMessages.get("api.t.7ad981e41d56"), channel)))
                    .orElse(ResponseEntity.status(404)
                            .body(ApiResponse.error(ApiMessages.get("api.t.11e0759cc797"))));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error( ApiMessages.get("api.t.c856c586387b") + e.getMessage()));
        }
    }
}
