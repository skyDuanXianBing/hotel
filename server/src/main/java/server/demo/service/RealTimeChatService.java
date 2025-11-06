package server.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.demo.dto.RealTimeChatRequest;
import server.demo.dto.RealTimeChatResponse;
import server.demo.entity.ChatRoom;
import server.demo.entity.RealTimeChatMessage;
import server.demo.repository.ChatRoomRepository;
import server.demo.repository.RealTimeChatMessageRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 实时聊天服务
 */
@Service
@Transactional
public class RealTimeChatService {
    
    private static final Logger logger = LoggerFactory.getLogger(RealTimeChatService.class);
    
    @Autowired
    private ChatRoomRepository chatRoomRepository;
    
    @Autowired
    private RealTimeChatMessageRepository messageRepository;
    
    // 内存中存储最新消息，用于实时推送
    private final ConcurrentHashMap<String, List<RealTimeChatResponse>> recentMessages = new ConcurrentHashMap<>();
    
    /**
     * 发送消息
     */
    public RealTimeChatResponse sendMessage(RealTimeChatRequest request) {
        logger.info("【实时聊天】收到消息发送请求 - 房间ID: {}, 发送者类型: {}, 发送者姓名: {}, 消息内容: '{}'", 
                   request.getRoomId(), request.getSenderType(), request.getSenderName(), request.getMessage());
        
        // 获取或创建聊天室
        ChatRoom chatRoom = getOrCreateChatRoom(request);
        
        // 创建消息
        RealTimeChatMessage message = new RealTimeChatMessage(
            chatRoom, 
            request.getSenderType(), 
            request.getSenderName(), 
            request.getMessage()
        );
        message.setMessageType(request.getMessageType());
        
        // 保存消息
        message = messageRepository.save(message);
        logger.info("【实时聊天】消息已保存到数据库 - 消息ID: {}, 房间ID: {}", message.getId(), request.getRoomId());
        
        // 更新聊天室最后活动时间
        chatRoom.updateLastActivity();
        chatRoomRepository.save(chatRoom);
        
        // 转换为响应DTO
        RealTimeChatResponse response = RealTimeChatResponse.fromEntity(message);
        
        // 添加到内存缓存
        addToRecentMessages(request.getRoomId(), response);
        
        logger.info("【实时聊天】消息处理完成 - 消息ID: {}, 房间ID: {}, 发送者: {}", 
                   response.getMessageId(), response.getRoomId(), response.getSenderName());
        
        // 特别说明：此服务仅处理消息存储，不会自动触发AI回复
        logger.debug("【实时聊天】注意：RealTimeChatService不包含AI自动回复逻辑");
        
        return response;
    }
    
    /**
     * 获取聊天室消息
     */
    public List<RealTimeChatResponse> getRoomMessages(String roomId) {
        ChatRoom chatRoom = chatRoomRepository.findByRoomId(roomId).orElse(null);
        if (chatRoom == null) {
            return List.of();
        }
        
        List<RealTimeChatMessage> messages = messageRepository.findByChatRoomIdOrderBySentAtAsc(chatRoom.getId());
        return messages.stream()
                .map(RealTimeChatResponse::fromEntity)
                .collect(Collectors.toList());
    }
    
    /**
     * 获取指定时间后的新消息
     */
    public List<RealTimeChatResponse> getNewMessages(String roomId, LocalDateTime since) {
        ChatRoom chatRoom = chatRoomRepository.findByRoomId(roomId).orElse(null);
        if (chatRoom == null) {
            return List.of();
        }
        
        List<RealTimeChatMessage> messages = messageRepository.findMessagesSince(chatRoom.getId(), since);
        return messages.stream()
                .map(RealTimeChatResponse::fromEntity)
                .collect(Collectors.toList());
    }
    
    /**
     * 获取所有活跃的聊天室
     */
    public List<ChatRoom> getActiveChatRooms() {
        return chatRoomRepository.findByStatusOrderByLastActivityDesc(ChatRoom.ChatStatus.ACTIVE);
    }
    
    /**
     * 创建新的聊天室
     */
    public ChatRoom createChatRoom(String guestName, String guestRoomNumber) {
        String roomId = generateRoomId();
        ChatRoom chatRoom = new ChatRoom(roomId, guestName, guestRoomNumber);
        return chatRoomRepository.save(chatRoom);
    }
    
    /**
     * 关闭聊天室
     */
    public void closeChatRoom(String roomId) {
        ChatRoom chatRoom = chatRoomRepository.findByRoomId(roomId).orElse(null);
        if (chatRoom != null) {
            chatRoom.setStatus(ChatRoom.ChatStatus.CLOSED);
            chatRoomRepository.save(chatRoom);
            
            // 清理内存缓存
            recentMessages.remove(roomId);
        }
    }
    
    /**
     * 获取或创建聊天室
     */
    private ChatRoom getOrCreateChatRoom(RealTimeChatRequest request) {
        ChatRoom chatRoom = chatRoomRepository.findByRoomId(request.getRoomId()).orElse(null);
        
        if (chatRoom == null) {
            // 创建新聊天室
            chatRoom = new ChatRoom(
                request.getRoomId(), 
                request.getSenderName(), 
                request.getGuestRoomNumber()
            );
            chatRoom = chatRoomRepository.save(chatRoom);
        }
        
        return chatRoom;
    }
    
    /**
     * 生成唯一的房间ID
     */
    private String generateRoomId() {
        return "room_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
    
    /**
     * 添加消息到内存缓存
     */
    private void addToRecentMessages(String roomId, RealTimeChatResponse message) {
        recentMessages.computeIfAbsent(roomId, k -> new CopyOnWriteArrayList<>()).add(message);
        
        // 只保留最近50条消息
        List<RealTimeChatResponse> messages = recentMessages.get(roomId);
        if (messages.size() > 50) {
            messages.remove(0);
        }
    }
    
    /**
     * 获取内存中的最新消息
     */
    public List<RealTimeChatResponse> getRecentMessages(String roomId) {
        return recentMessages.getOrDefault(roomId, List.of());
    }
}