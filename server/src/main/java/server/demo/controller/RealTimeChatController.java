package server.demo.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import server.demo.dto.ApiResponse;
import server.demo.dto.RealTimeChatRequest;
import server.demo.dto.RealTimeChatResponse;
import server.demo.entity.ChatRoom;
import server.demo.service.RealTimeChatService;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 实时聊天控制器
 */
@RestController
@RequestMapping("/api/v1/realtime-chat")
@CrossOrigin(origins = {"http://localhost:8091", "http://127.0.0.1:8091"}, 
            allowedHeaders = "*", 
            methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS},
            allowCredentials = "true")
public class RealTimeChatController {
    
    private static final Logger logger = LoggerFactory.getLogger(RealTimeChatController.class);
    
    @Autowired
    private RealTimeChatService chatService;
    
    /**
     * 发送消息
     */
    @PostMapping("/send")
    public ResponseEntity<ApiResponse<RealTimeChatResponse>> sendMessage(
            @Valid @RequestBody RealTimeChatRequest request) {
        
        logger.info("收到聊天消息: {}", request);
        
        try {
            RealTimeChatResponse response = chatService.sendMessage(request);
            logger.info("消息发送成功: messageId={}, roomId={}", response.getMessageId(), response.getRoomId());
            
            return ResponseEntity.ok(ApiResponse.success("消息发送成功", response));
            
        } catch (Exception e) {
            logger.error("发送消息失败", e);
            return ResponseEntity.ok(ApiResponse.error("发送消息失败: " + e.getMessage(), null));
        }
    }
    
    /**
     * 获取聊天室消息
     */
    @GetMapping("/messages/{roomId}")
    public ResponseEntity<ApiResponse<List<RealTimeChatResponse>>> getRoomMessages(
            @PathVariable String roomId) {
        
        logger.info("获取聊天室消息: roomId={}", roomId);
        
        try {
            List<RealTimeChatResponse> messages = chatService.getRoomMessages(roomId);
            return ResponseEntity.ok(ApiResponse.success("获取消息成功", messages));
            
        } catch (Exception e) {
            logger.error("获取消息失败", e);
            return ResponseEntity.ok(ApiResponse.error("获取消息失败: " + e.getMessage(), null));
        }
    }
    
    /**
     * 轮询新消息
     */
    @GetMapping("/poll/{roomId}")
    public ResponseEntity<ApiResponse<List<RealTimeChatResponse>>> pollNewMessages(
            @PathVariable String roomId,
            @RequestParam(required = false) String since) {
        
        try {
            List<RealTimeChatResponse> messages;
            
            if (since != null && !since.isEmpty()) {
                // 解析ISO格式的时间字符串（支持带时区的格式如 2025-09-27T03:44:26.850Z）
                LocalDateTime sinceTime;
                try {
                    // 尝试解析带时区的ISO格式
                    ZonedDateTime zonedDateTime = ZonedDateTime.parse(since, DateTimeFormatter.ISO_DATE_TIME);
                    sinceTime = zonedDateTime.toLocalDateTime();
                } catch (Exception e) {
                    // 如果失败，尝试解析不带时区的格式
                    sinceTime = LocalDateTime.parse(since, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                }
                messages = chatService.getNewMessages(roomId, sinceTime);
            } else {
                // 获取最近的消息
                messages = chatService.getRecentMessages(roomId);
            }
            
            return ResponseEntity.ok(ApiResponse.success("轮询成功", messages));
            
        } catch (Exception e) {
            logger.error("轮询消息失败", e);
            return ResponseEntity.ok(ApiResponse.error("轮询失败: " + e.getMessage(), null));
        }
    }
    
    /**
     * 获取活跃聊天室列表
     */
    @GetMapping("/rooms")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getActiveChatRooms() {
        
        logger.info("获取活跃聊天室列表");
        
        try {
            List<ChatRoom> chatRooms = chatService.getActiveChatRooms();
            
            List<Map<String, Object>> roomList = chatRooms.stream()
                    .map(room -> {
                        Map<String, Object> roomInfo = new HashMap<>();
                        roomInfo.put("roomId", room.getRoomId());
                        roomInfo.put("guestName", room.getGuestName());
                        roomInfo.put("guestRoomNumber", room.getGuestRoomNumber());
                        roomInfo.put("status", room.getStatus());
                        roomInfo.put("lastActivity", room.getLastActivity());
                        roomInfo.put("createdAt", room.getCreatedAt());
                        return roomInfo;
                    })
                    .toList();
            
            return ResponseEntity.ok(ApiResponse.success("获取聊天室列表成功", roomList));
            
        } catch (Exception e) {
            logger.error("获取聊天室列表失败", e);
            return ResponseEntity.ok(ApiResponse.error("获取列表失败: " + e.getMessage(), null));
        }
    }
    
    /**
     * 创建新聊天室
     */
    @PostMapping("/room/create")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createChatRoom(
            @RequestParam String guestName,
            @RequestParam(required = false) String guestRoomNumber) {
        
        logger.info("创建聊天室: guestName={}, guestRoomNumber={}", guestName, guestRoomNumber);
        
        try {
            ChatRoom chatRoom = chatService.createChatRoom(guestName, guestRoomNumber);
            
            Map<String, Object> roomInfo = new HashMap<>();
            roomInfo.put("roomId", chatRoom.getRoomId());
            roomInfo.put("guestName", chatRoom.getGuestName());
            roomInfo.put("guestRoomNumber", chatRoom.getGuestRoomNumber());
            roomInfo.put("status", chatRoom.getStatus());
            roomInfo.put("createdAt", chatRoom.getCreatedAt());
            
            return ResponseEntity.ok(ApiResponse.success("聊天室创建成功", roomInfo));
            
        } catch (Exception e) {
            logger.error("创建聊天室失败", e);
            return ResponseEntity.ok(ApiResponse.error("创建聊天室失败: " + e.getMessage(), null));
        }
    }
    
    /**
     * 关闭聊天室
     */
    @PostMapping("/room/{roomId}/close")
    public ResponseEntity<ApiResponse<String>> closeChatRoom(@PathVariable String roomId) {
        
        logger.info("关闭聊天室: roomId={}", roomId);
        
        try {
            chatService.closeChatRoom(roomId);
            return ResponseEntity.ok(ApiResponse.success("聊天室已关闭", "success"));
            
        } catch (Exception e) {
            logger.error("关闭聊天室失败", e);
            return ResponseEntity.ok(ApiResponse.error("关闭聊天室失败: " + e.getMessage(), null));
        }
    }
}