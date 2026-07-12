package server.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import server.demo.entity.SuMessageThread;
import server.demo.enums.SuMessagingSenderType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.QueryHint;

public interface SuMessageThreadRepository extends JpaRepository<SuMessageThread, Long> {
    interface KnowledgeDueThreadRow {
        Long getStoreId();

        Long getThreadId();

        Long getDirtyMessageId();
    }

    Optional<SuMessageThread> findByStoreIdAndId(Long storeId, Long id);

    Optional<SuMessageThread> findByStoreIdAndChannelIdAndThreadKey(Long storeId, Integer channelId, String threadKey);

    Optional<SuMessageThread> findFirstByStoreIdAndChannelIdAndBookingIdOrderByLastActivityDesc(Long storeId, Integer channelId, String bookingId);

    List<SuMessageThread> findByStoreIdOrderByLastActivityDesc(Long storeId);

    @Query(
            value = """
                    SELECT /*+ MAX_EXECUTION_TIME(5000) */ t.*
                    FROM su_message_threads t
                    JOIN (
                        SELECT ranked.thread_id, ranked.sent_at, ranked.id
                        FROM (
                            SELECT m.thread_id,
                                   m.sender_type,
                                   m.sent_at,
                                   m.id,
                                   ROW_NUMBER() OVER (
                                       PARTITION BY m.thread_id
                                       ORDER BY m.sent_at DESC, m.id DESC
                                   ) AS row_num
                            FROM su_messages m
                            WHERE m.store_id = :storeId
                              AND (
                                  m.sender_type = 'GUEST'
                                  OR (m.sender_type = 'STAFF' AND m.delivery_status = 'SENT')
                              )
                        ) ranked
                        WHERE ranked.row_num = 1
                          AND ranked.sender_type = 'GUEST'
                    ) latest_message
                      ON latest_message.thread_id = t.id
                    WHERE t.store_id = :storeId
                      AND t.closed = false
                    ORDER BY latest_message.sent_at DESC, latest_message.id DESC
                    """,
            countQuery = """
                    SELECT /*+ MAX_EXECUTION_TIME(5000) */ COUNT(*)
                    FROM su_message_threads t
                    JOIN (
                        SELECT ranked.thread_id
                        FROM (
                            SELECT m.thread_id,
                                   m.sender_type,
                                   ROW_NUMBER() OVER (
                                       PARTITION BY m.thread_id
                                       ORDER BY m.sent_at DESC, m.id DESC
                                   ) AS row_num
                            FROM su_messages m
                            WHERE m.store_id = :storeId
                              AND (
                                  m.sender_type = 'GUEST'
                                  OR (m.sender_type = 'STAFF' AND m.delivery_status = 'SENT')
                              )
                        ) ranked
                        WHERE ranked.row_num = 1
                          AND ranked.sender_type = 'GUEST'
                    ) latest_message
                      ON latest_message.thread_id = t.id
                    WHERE t.store_id = :storeId
                      AND t.closed = false
                    """,
            nativeQuery = true
    )
    @QueryHints(@QueryHint(name = "jakarta.persistence.query.timeout", value = "5000"))
    @org.springframework.transaction.annotation.Transactional(readOnly = true, timeout = 5)
    Page<SuMessageThread> findAwaitingReplyPageByStoreId(
            @Param("storeId") Long storeId,
            Pageable pageable
    );

    @Query(
            value = """
                    SELECT /*+ MAX_EXECUTION_TIME(5000) */ COUNT(*)
                    FROM su_message_threads t
                    JOIN (
                        SELECT ranked.thread_id
                        FROM (
                            SELECT m.thread_id,
                                   m.sender_type,
                                   ROW_NUMBER() OVER (
                                       PARTITION BY m.thread_id
                                       ORDER BY m.sent_at DESC, m.id DESC
                                   ) AS row_num
                            FROM su_messages m
                            WHERE m.store_id = :storeId
                              AND (
                                  m.sender_type = 'GUEST'
                                  OR (m.sender_type = 'STAFF' AND m.delivery_status = 'SENT')
                              )
                        ) ranked
                        WHERE ranked.row_num = 1
                          AND ranked.sender_type = 'GUEST'
                    ) latest_message
                      ON latest_message.thread_id = t.id
                    WHERE t.store_id = :storeId
                      AND t.closed = false
                    """,
            nativeQuery = true
    )
    @QueryHints(@QueryHint(name = "jakarta.persistence.query.timeout", value = "5000"))
    @org.springframework.transaction.annotation.Transactional(readOnly = true, timeout = 5)
    long countAwaitingReplyByStoreId(@Param("storeId") Long storeId);

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

    @Query(
            value = """
                    SELECT store_id AS storeId,
                           id AS threadId,
                           knowledge_dirty_message_id AS dirtyMessageId
                    FROM su_message_threads
                    WHERE knowledge_pending = 1
                      AND (knowledge_extract_after IS NULL OR knowledge_extract_after <= :now)
                      AND (knowledge_extracting_until IS NULL OR knowledge_extracting_until < :now)
                    ORDER BY COALESCE(knowledge_extract_after, '1970-01-01 00:00:00') ASC,
                             store_id ASC,
                             id ASC
                    """,
            nativeQuery = true
    )
    List<KnowledgeDueThreadRow> findDueKnowledgeThreads(
            @Param("now") LocalDateTime now,
            Pageable pageable
    );

    @Query(
            value = """
                    SELECT id
                    FROM su_message_threads
                    WHERE store_id = :storeId
                      AND knowledge_pending = 1
                      AND (knowledge_extract_after IS NULL OR knowledge_extract_after <= :now)
                      AND (knowledge_extracting_until IS NULL OR knowledge_extracting_until < :now)
                    ORDER BY COALESCE(knowledge_extract_after, '1970-01-01 00:00:00') ASC,
                             id ASC
                    """,
            nativeQuery = true
    )
    List<Long> findDueKnowledgeThreadIds(
            @Param("storeId") Long storeId,
            @Param("now") LocalDateTime now,
            Pageable pageable
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            value = """
                    UPDATE su_message_threads
                    SET knowledge_extracting_owner = :leaseOwner,
                        knowledge_extracting_until = :leaseUntil,
                        knowledge_error = NULL,
                        updated_at = :now
                    WHERE store_id = :storeId
                      AND id = :threadId
                      AND knowledge_pending = 1
                      AND (knowledge_extract_after IS NULL OR knowledge_extract_after <= :now)
                      AND (knowledge_extracting_until IS NULL OR knowledge_extracting_until < :now)
                    """,
            nativeQuery = true
    )
    int claimDueKnowledgeThread(
            @Param("storeId") Long storeId,
            @Param("threadId") Long threadId,
            @Param("now") LocalDateTime now,
            @Param("leaseOwner") String leaseOwner,
            @Param("leaseUntil") LocalDateTime leaseUntil
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            value = """
                    UPDATE su_message_threads
                    SET knowledge_pending = 0,
                        knowledge_extract_after = NULL,
                        knowledge_extracting_owner = NULL,
                        knowledge_extracting_until = NULL,
                        knowledge_extracted_at = :completedAt,
                        knowledge_extracted_message_id = :processedMessageId,
                        knowledge_error = NULL,
                        knowledge_attempt_count = 0,
                        knowledge_extractor_version = :extractorVersion,
                        updated_at = :completedAt
                    WHERE store_id = :storeId
                      AND id = :threadId
                      AND knowledge_extracting_owner = :leaseOwner
                      AND (
                            knowledge_dirty_message_id IS NULL
                            OR knowledge_dirty_message_id <= :processedMessageId
                          )
                    """,
            nativeQuery = true
    )
    int completeKnowledgeExtractionIfCurrent(
            @Param("storeId") Long storeId,
            @Param("threadId") Long threadId,
            @Param("leaseOwner") String leaseOwner,
            @Param("processedMessageId") Long processedMessageId,
            @Param("completedAt") LocalDateTime completedAt,
            @Param("extractorVersion") String extractorVersion
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            value = """
                    UPDATE su_message_threads
                    SET knowledge_extracting_owner = NULL,
                        knowledge_extracting_until = NULL,
                        knowledge_extracted_at = :completedAt,
                        knowledge_extracted_message_id = CASE
                            WHEN knowledge_extracted_message_id IS NULL
                                 OR knowledge_extracted_message_id < :processedMessageId
                            THEN :processedMessageId
                            ELSE knowledge_extracted_message_id
                        END,
                        knowledge_error = NULL,
                        knowledge_extractor_version = :extractorVersion,
                        updated_at = :completedAt
                    WHERE store_id = :storeId
                      AND id = :threadId
                      AND knowledge_extracting_owner = :leaseOwner
                      AND knowledge_dirty_message_id > :processedMessageId
                    """,
            nativeQuery = true
    )
    int releaseKnowledgeExtractionForStaleDirty(
            @Param("storeId") Long storeId,
            @Param("threadId") Long threadId,
            @Param("leaseOwner") String leaseOwner,
            @Param("processedMessageId") Long processedMessageId,
            @Param("completedAt") LocalDateTime completedAt,
            @Param("extractorVersion") String extractorVersion
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            value = """
                    UPDATE su_message_threads
                    SET knowledge_pending = 1,
                        knowledge_extract_after = :retryAfter,
                        knowledge_extracting_owner = NULL,
                        knowledge_extracting_until = NULL,
                        knowledge_error = :error,
                        knowledge_attempt_count = COALESCE(knowledge_attempt_count, 0) + 1,
                        updated_at = :now
                    WHERE store_id = :storeId
                      AND id = :threadId
                      AND knowledge_extracting_owner = :leaseOwner
                    """,
            nativeQuery = true
    )
    int failKnowledgeExtractionForRetry(
            @Param("storeId") Long storeId,
            @Param("threadId") Long threadId,
            @Param("leaseOwner") String leaseOwner,
            @Param("now") LocalDateTime now,
            @Param("retryAfter") LocalDateTime retryAfter,
            @Param("error") String error
    );
}
