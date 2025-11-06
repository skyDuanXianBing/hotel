package server.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import server.demo.dto.ChannelDTO;
import server.demo.dto.CreateChannelRequest;
import server.demo.service.ChannelService;
import server.demo.dto.ApiResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/channels")
@CrossOrigin(origins = {"http://localhost:8091", "http://127.0.0.1:8091"}, allowCredentials = "true")
public class ChannelController {

    @Autowired
    private ChannelService channelService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ChannelDTO>>> getAllChannels(HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            List<ChannelDTO> channels = channelService.getAllChannels(userId);
            return ResponseEntity.ok(ApiResponse.success("获取渠道列表成功", channels));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("获取渠道列表失败: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ChannelDTO>> getChannelById(@PathVariable Long id, HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            return channelService.getChannelById(userId, id)
                    .map(channel -> ResponseEntity.ok(ApiResponse.success("获取渠道成功", channel)))
                    .orElse(ResponseEntity.status(404)
                            .body(ApiResponse.error("渠道不存在")));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error( "获取渠道失败: " + e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ChannelDTO>> createChannel(@Valid @RequestBody CreateChannelRequest request, HttpServletRequest httpRequest) {
        try {
            Long userId = (Long) httpRequest.getAttribute("userId");
            ChannelDTO createdChannel = channelService.createChannel(userId, request);
            return ResponseEntity.ok(ApiResponse.success("创建渠道成功", createdChannel));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error( "创建渠道失败: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ChannelDTO>> updateChannel(
            @PathVariable Long id,
            @Valid @RequestBody CreateChannelRequest request,
            HttpServletRequest httpRequest) {
        try {
            Long userId = (Long) httpRequest.getAttribute("userId");
            return channelService.updateChannel(userId, id, request)
                    .map(channel -> ResponseEntity.ok(ApiResponse.success("更新渠道成功", channel)))
                    .orElse(ResponseEntity.status(404)
                            .body(ApiResponse.error("渠道不存在")));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error( "更新渠道失败: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteChannel(@PathVariable Long id, HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            if (channelService.deleteChannel(userId, id)) {
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
    public ResponseEntity<ApiResponse<ChannelDTO>> toggleChannelStatus(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> request,
            HttpServletRequest httpRequest) {
        try {
            Long userId = (Long) httpRequest.getAttribute("userId");
            Boolean enabled = request.get("enabled");
            return channelService.toggleChannelStatus(userId, id, enabled)
                    .map(channel -> ResponseEntity.ok(ApiResponse.success("更新渠道状态成功", channel)))
                    .orElse(ResponseEntity.status(404)
                            .body(ApiResponse.error("渠道不存在")));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error( "更新渠道状态失败: " + e.getMessage()));
        }
    }
}