package server.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import server.demo.entity.RealTimeChatMessage;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RealTimeChatMessageRepository extends JpaRepository<RealTimeChatMessage, Long> {
    
    /**
     * 根据聊天室ID查找消息，按时间排序
     */
    List<RealTimeChatMessage> findByChatRoomIdOrderBySentAtAsc(Long chatRoomId);
    
    /**
     * 查找聊天室中指定时间后的消息
     */
    @Query("SELECT m FROM RealTimeChatMessage m WHERE m.chatRoom.id = :chatRoomId AND m.sentAt > :since ORDER BY m.sentAt ASC")
    List<RealTimeChatMessage> findMessagesSince(Long chatRoomId, LocalDateTime since);
    
    /**
     * 查找未读消息数量
     */
    @Query("SELECT COUNT(m) FROM RealTimeChatMessage m WHERE m.chatRoom.id = :chatRoomId AND m.isRead = false AND m.senderType = :senderType")
    Long countUnreadMessages(Long chatRoomId, RealTimeChatMessage.SenderType senderType);
    
    /**
     * 标记聊天室中指定发送者类型的消息为已读
     */
    @Query("UPDATE RealTimeChatMessage m SET m.isRead = true WHERE m.chatRoom.id = :chatRoomId AND m.senderType = :senderType AND m.isRead = false")
    void markMessagesAsRead(Long chatRoomId, RealTimeChatMessage.SenderType senderType);
}