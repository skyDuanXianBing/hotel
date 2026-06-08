package server.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import server.demo.entity.SuMessageThread;
import server.demo.enums.SuMessagingSenderType;

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
              AND (:closed IS NULL OR t.closed = :closed)
              AND (:unread IS NULL
                   OR (:unread = true AND EXISTS (
                        SELECT m.id
                        FROM SuMessage m
                        WHERE m.storeId = :storeId
                          AND m.thread = t
                          AND m.senderType = :guestSenderType
                          AND m.isRead = false
                   ))
                   OR (:unread = false AND NOT EXISTS (
                        SELECT m.id
                        FROM SuMessage m
                        WHERE m.storeId = :storeId
                          AND m.thread = t
                          AND m.senderType = :guestSenderType
                          AND m.isRead = false
                   )))
              AND (:search IS NULL
                   OR LOWER(t.guestName) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(t.bookingId) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(t.threadId) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(t.listingName) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(t.lastMessage) LIKE LOWER(CONCAT('%', :search, '%')))
            ORDER BY t.lastActivity DESC, t.id DESC
            """)
    Page<SuMessageThread> findPageByStoreIdAndFilters(
            @Param("storeId") Long storeId,
            @Param("channelId") Integer channelId,
            @Param("closed") Boolean closed,
            @Param("unread") Boolean unread,
            @Param("search") String search,
            @Param("guestSenderType") SuMessagingSenderType guestSenderType,
            Pageable pageable
    );

    @Query("""
            SELECT t
            FROM SuMessageThread t
            WHERE t.storeId = :storeId
              AND (:channelId IS NULL OR t.channelId = :channelId)
              AND (:closed IS NULL OR t.closed = :closed)
              AND (:unread IS NULL
                   OR (:unread = true AND EXISTS (
                        SELECT m.id
                        FROM SuMessage m
                        WHERE m.storeId = :storeId
                          AND m.thread = t
                          AND m.senderType = :guestSenderType
                          AND m.isRead = false
                   ))
                   OR (:unread = false AND NOT EXISTS (
                        SELECT m.id
                        FROM SuMessage m
                        WHERE m.storeId = :storeId
                          AND m.thread = t
                          AND m.senderType = :guestSenderType
                          AND m.isRead = false
                   )))
              AND (:search IS NULL
                   OR LOWER(t.guestName) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(t.bookingId) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(t.threadId) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(t.listingName) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(t.lastMessage) LIKE LOWER(CONCAT('%', :search, '%')))
            ORDER BY t.lastActivity DESC, t.id DESC
            """)
    List<SuMessageThread> findByStoreIdAndFilters(
            @Param("storeId") Long storeId,
            @Param("channelId") Integer channelId,
            @Param("closed") Boolean closed,
            @Param("unread") Boolean unread,
            @Param("search") String search,
            @Param("guestSenderType") SuMessagingSenderType guestSenderType
    );

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
