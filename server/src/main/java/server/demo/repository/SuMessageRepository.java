package server.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import server.demo.entity.SuMessage;
import server.demo.enums.SuMessagingSenderType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SuMessageRepository extends JpaRepository<SuMessage, Long> {
    interface ChannelUnreadSummaryRow {
        Integer getChannelId();

        Long getUnreadMessageCount();

        Long getUnreadThreadCount();
    }

    interface ThreadUnreadCountRow {
        Long getThreadId();

        Long getUnreadCount();
    }

    Optional<SuMessage> findByStoreIdAndExternalMessageId(Long storeId, String externalMessageId);

    Optional<SuMessage> findByStoreIdAndId(Long storeId, Long id);

    @Query("""
            SELECT m
            FROM SuMessage m
            JOIN FETCH m.thread
            WHERE m.storeId = :storeId
              AND m.thread.id = :threadId
              AND m.id = :messageId
            """)
    Optional<SuMessage> findByStoreIdAndThreadIdAndId(
            @Param("storeId") Long storeId,
            @Param("threadId") Long threadId,
            @Param("messageId") Long messageId
    );

    List<SuMessage> findByThread_IdOrderBySentAtAsc(Long threadId);

    List<SuMessage> findByThread_IdAndSentAtAfterOrderBySentAtAsc(Long threadId, LocalDateTime since);

    @Query("""
            SELECT m
            FROM SuMessage m
            JOIN FETCH m.thread
            WHERE m.storeId = :storeId
              AND m.sentAt >= :since
            ORDER BY m.sentAt DESC, m.id DESC
            """)
    List<SuMessage> findRecentMessagesForKnowledgeIndex(
            @Param("storeId") Long storeId,
            @Param("since") LocalDateTime since,
            Pageable pageable
    );

    @Query("""
            SELECT DISTINCT m.storeId
            FROM SuMessage m
            WHERE m.storeId IS NOT NULL
              AND m.sentAt >= :since
            ORDER BY m.storeId ASC
            """)
    List<Long> findDistinctStoreIdsWithMessagesSince(
            @Param("since") LocalDateTime since,
            Pageable pageable
    );

    @Query("""
            SELECT m
            FROM SuMessage m
            WHERE m.storeId = :storeId
              AND m.thread.id = :threadId
            ORDER BY m.sentAt DESC, m.id DESC
            """)
    List<SuMessage> findRecentByStoreIdAndThreadIdDesc(
            @Param("storeId") Long storeId,
            @Param("threadId") Long threadId,
            Pageable pageable
    );

    @Query("""
            SELECT m
            FROM SuMessage m
            WHERE m.storeId = :storeId
              AND m.thread.id = :threadId
              AND m.senderType = :senderType
            ORDER BY m.sentAt DESC, m.id DESC
            """)
    List<SuMessage> findRecentByStoreIdAndThreadIdAndSenderTypeDesc(
            @Param("storeId") Long storeId,
            @Param("threadId") Long threadId,
            @Param("senderType") SuMessagingSenderType senderType,
            Pageable pageable
    );

    @Query("""
            SELECT m
            FROM SuMessage m
            WHERE m.storeId = :storeId
              AND m.thread.id = :threadId
            ORDER BY m.sentAt ASC, m.id ASC
            """)
    List<SuMessage> findByStoreIdAndThreadIdOrderBySentAtAsc(
            @Param("storeId") Long storeId,
            @Param("threadId") Long threadId,
            Pageable pageable
    );

    @Query("""
            SELECT m
            FROM SuMessage m
            JOIN FETCH m.thread
            WHERE m.storeId = :storeId
              AND m.thread.id = :threadId
              AND m.id <= :coveredMessageId
            ORDER BY m.sentAt ASC, m.id ASC
            """)
    List<SuMessage> findByStoreIdAndThreadIdUpToMessageIdOrderBySentAtAsc(
            @Param("storeId") Long storeId,
            @Param("threadId") Long threadId,
            @Param("coveredMessageId") Long coveredMessageId,
            Pageable pageable
    );

    @Query("""
            SELECT m
            FROM SuMessage m
            WHERE m.storeId = :storeId
              AND m.thread.id = :threadId
              AND (:beforeMessageId IS NULL OR m.id < :beforeMessageId)
              AND (:afterMessageId IS NULL OR m.id > :afterMessageId)
            ORDER BY m.id DESC
            """)
    List<SuMessage> findMessagePageByCursorDesc(
            @Param("storeId") Long storeId,
            @Param("threadId") Long threadId,
            @Param("beforeMessageId") Long beforeMessageId,
            @Param("afterMessageId") Long afterMessageId,
            Pageable pageable
    );

    boolean existsByStoreIdAndThread_IdAndIdLessThan(Long storeId, Long threadId, Long id);

    boolean existsByStoreIdAndThread_IdAndIdGreaterThan(Long storeId, Long threadId, Long id);

    long countByThread_IdAndSenderTypeAndIsReadFalse(Long threadId, SuMessagingSenderType senderType);

    @Query("""
            SELECT m.thread.id AS threadId, COUNT(m) AS unreadCount
            FROM SuMessage m
            WHERE m.storeId = :storeId
              AND m.thread.id IN :threadIds
              AND m.senderType = :senderType
              AND m.isRead = false
            GROUP BY m.thread.id
            """)
    List<ThreadUnreadCountRow> countUnreadByStoreIdAndThreadIds(
            @Param("storeId") Long storeId,
            @Param("threadIds") List<Long> threadIds,
            @Param("senderType") SuMessagingSenderType senderType
    );

    boolean existsByThread_IdAndSenderType(Long threadId, SuMessagingSenderType senderType);

    @Query("""
            SELECT COUNT(m)
            FROM SuMessage m
            WHERE m.storeId = :storeId
              AND m.senderType = :senderType
              AND m.isRead = false
            """)
    long countUnreadMessagesByStoreId(
            @Param("storeId") Long storeId,
            @Param("senderType") SuMessagingSenderType senderType
    );

    @Query("""
            SELECT COUNT(DISTINCT m.thread.id)
            FROM SuMessage m
            WHERE m.storeId = :storeId
              AND m.senderType = :senderType
              AND m.isRead = false
            """)
    long countUnreadThreadsByStoreId(
            @Param("storeId") Long storeId,
            @Param("senderType") SuMessagingSenderType senderType
    );

    @Query("""
            SELECT m.thread.channelId AS channelId,
                   COUNT(m) AS unreadMessageCount,
                   COUNT(DISTINCT m.thread.id) AS unreadThreadCount
            FROM SuMessage m
            WHERE m.storeId = :storeId
              AND m.senderType = :senderType
              AND m.isRead = false
            GROUP BY m.thread.channelId
            """)
    List<ChannelUnreadSummaryRow> summarizeUnreadByChannel(
            @Param("storeId") Long storeId,
            @Param("senderType") SuMessagingSenderType senderType
    );

    @Modifying
    @Query("UPDATE SuMessage m SET m.isRead = true WHERE m.thread.id = :threadId AND m.senderType = :senderType AND m.isRead = false")
    int markThreadMessagesAsRead(@Param("threadId") Long threadId, @Param("senderType") SuMessagingSenderType senderType);
}
