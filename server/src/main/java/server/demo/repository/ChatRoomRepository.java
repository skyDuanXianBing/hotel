package server.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import server.demo.entity.ChatRoom;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    
    /**
     * 根据房间ID查找聊天室
     */
    Optional<ChatRoom> findByRoomId(String roomId);
    
    /**
     * 查找活跃的聊天室
     */
    List<ChatRoom> findByStatusOrderByLastActivityDesc(ChatRoom.ChatStatus status);
    
    /**
     * 查找指定时间后有活动的聊天室
     */
    @Query("SELECT cr FROM ChatRoom cr WHERE cr.lastActivity > :since ORDER BY cr.lastActivity DESC")
    List<ChatRoom> findActiveRoomsSince(LocalDateTime since);
    
    /**
     * 根据客户房间号查找聊天室
     */
    Optional<ChatRoom> findByGuestRoomNumberAndStatus(String guestRoomNumber, ChatRoom.ChatStatus status);
}