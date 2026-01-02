package server.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import server.demo.entity.SuMessage;
import server.demo.enums.SuMessagingSenderType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SuMessageRepository extends JpaRepository<SuMessage, Long> {
    Optional<SuMessage> findByStoreIdAndExternalMessageId(Long storeId, String externalMessageId);

    List<SuMessage> findByThread_IdOrderBySentAtAsc(Long threadId);

    List<SuMessage> findByThread_IdAndSentAtAfterOrderBySentAtAsc(Long threadId, LocalDateTime since);

    long countByThread_IdAndSenderTypeAndIsReadFalse(Long threadId, SuMessagingSenderType senderType);

    boolean existsByThread_IdAndSenderType(Long threadId, SuMessagingSenderType senderType);

    @Modifying
    @Query("UPDATE SuMessage m SET m.isRead = true WHERE m.thread.id = :threadId AND m.senderType = :senderType AND m.isRead = false")
    int markThreadMessagesAsRead(@Param("threadId") Long threadId, @Param("senderType") SuMessagingSenderType senderType);
}
