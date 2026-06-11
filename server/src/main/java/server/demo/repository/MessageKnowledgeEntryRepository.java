package server.demo.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import server.demo.entity.MessageKnowledgeEntry;

import java.util.List;
import java.util.Optional;

public interface MessageKnowledgeEntryRepository extends JpaRepository<MessageKnowledgeEntry, Long> {

    Optional<MessageKnowledgeEntry> findByStoreIdAndSourceGuestMessageIdAndSourceStaffMessageIdAndNormalizedHash(
            Long storeId,
            Long sourceGuestMessageId,
            Long sourceStaffMessageId,
            String normalizedHash
    );

    @Query("""
            SELECT e
            FROM MessageKnowledgeEntry e
            WHERE e.storeId = :storeId
              AND e.status = :status
              AND e.roomId = :roomId
              AND (:excludeThreadId IS NULL OR e.threadId <> :excludeThreadId)
            ORDER BY e.sourceTimestamp DESC, e.id DESC
            """)
    List<MessageKnowledgeEntry> findActiveByStoreIdAndRoomId(
            @Param("storeId") Long storeId,
            @Param("roomId") Long roomId,
            @Param("status") String status,
            @Param("excludeThreadId") Long excludeThreadId,
            Pageable pageable
    );

    @Query("""
            SELECT e
            FROM MessageKnowledgeEntry e
            WHERE e.storeId = :storeId
              AND e.status = :status
              AND e.roomTypeId = :roomTypeId
              AND (:excludeThreadId IS NULL OR e.threadId <> :excludeThreadId)
            ORDER BY e.sourceTimestamp DESC, e.id DESC
            """)
    List<MessageKnowledgeEntry> findActiveByStoreIdAndRoomTypeId(
            @Param("storeId") Long storeId,
            @Param("roomTypeId") Long roomTypeId,
            @Param("status") String status,
            @Param("excludeThreadId") Long excludeThreadId,
            Pageable pageable
    );

    @Query("""
            SELECT e
            FROM MessageKnowledgeEntry e
            WHERE e.storeId = :storeId
              AND e.status = :status
              AND e.channelId = :channelId
              AND e.bookingKey = :bookingKey
              AND (:excludeThreadId IS NULL OR e.threadId <> :excludeThreadId)
            ORDER BY e.sourceTimestamp DESC, e.id DESC
            """)
    List<MessageKnowledgeEntry> findActiveByStoreIdAndChannelBookingKey(
            @Param("storeId") Long storeId,
            @Param("channelId") Integer channelId,
            @Param("bookingKey") String bookingKey,
            @Param("status") String status,
            @Param("excludeThreadId") Long excludeThreadId,
            Pageable pageable
    );

    @Query("""
            SELECT e
            FROM MessageKnowledgeEntry e
            WHERE e.storeId = :storeId
              AND e.status = :status
              AND (:excludeThreadId IS NULL OR e.threadId <> :excludeThreadId)
            ORDER BY e.sourceTimestamp DESC, e.id DESC
            """)
    List<MessageKnowledgeEntry> findActiveRecentByStoreId(
            @Param("storeId") Long storeId,
            @Param("status") String status,
            @Param("excludeThreadId") Long excludeThreadId,
            Pageable pageable
    );
}
