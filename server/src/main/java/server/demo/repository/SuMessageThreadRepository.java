package server.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import server.demo.entity.SuMessageThread;

import java.util.List;
import java.util.Optional;

public interface SuMessageThreadRepository extends JpaRepository<SuMessageThread, Long> {
    Optional<SuMessageThread> findByStoreIdAndId(Long storeId, Long id);

    Optional<SuMessageThread> findByStoreIdAndChannelIdAndThreadKey(Long storeId, Integer channelId, String threadKey);

    Optional<SuMessageThread> findFirstByStoreIdAndChannelIdAndBookingIdOrderByLastActivityDesc(Long storeId, Integer channelId, String bookingId);

    List<SuMessageThread> findByStoreIdOrderByLastActivityDesc(Long storeId);

    @Query("""
            SELECT t
            FROM SuMessageThread t
            WHERE t.storeId = :storeId
              AND (:channelId IS NULL OR t.channelId = :channelId)
              AND (:threadId IS NULL OR t.threadId = :threadId OR t.threadKey = :threadId)
              AND (:bookingId IS NULL OR t.bookingId = :bookingId OR t.threadKey = :bookingId)
              AND (:externalMessageId IS NULL OR EXISTS (
                  SELECT m.id
                  FROM SuMessage m
                  WHERE m.storeId = :storeId
                    AND m.thread = t
                    AND m.externalMessageId = :externalMessageId
              ))
              AND (:messageId IS NULL OR EXISTS (
                  SELECT m.id
                  FROM SuMessage m
                  WHERE m.storeId = :storeId
                    AND m.thread = t
                    AND m.id = :messageId
              ))
            ORDER BY t.lastActivity DESC, t.id DESC
            """)
    List<SuMessageThread> findRecentByStoreIdAndMessagingFilters(
            @Param("storeId") Long storeId,
            @Param("channelId") Integer channelId,
            @Param("threadId") String threadId,
            @Param("bookingId") String bookingId,
            @Param("externalMessageId") String externalMessageId,
            @Param("messageId") Long messageId,
            Pageable pageable
    );
}
