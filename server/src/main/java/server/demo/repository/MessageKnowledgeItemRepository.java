package server.demo.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import server.demo.entity.MessageKnowledgeItem;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MessageKnowledgeItemRepository extends JpaRepository<MessageKnowledgeItem, Long> {

    Optional<MessageKnowledgeItem> findByStoreIdAndScopeKeyAndTopicHash(
            Long storeId,
            String scopeKey,
            String topicHash
    );

    Optional<MessageKnowledgeItem> findByStoreIdAndScopeKeyAndTopicHashAndFactHash(
            Long storeId,
            String scopeKey,
            String topicHash,
            String factHash
    );

    Optional<MessageKnowledgeItem> findByStoreIdAndId(Long storeId, Long id);

    @Query("""
            SELECT item
            FROM MessageKnowledgeItem item
            WHERE item.storeId = :storeId
              AND item.status = :status
              AND item.roomId = :roomId
            ORDER BY item.lastSeenAt DESC, item.id DESC
            """)
    List<MessageKnowledgeItem> findActiveByStoreIdAndRoomId(
            @Param("storeId") Long storeId,
            @Param("roomId") Long roomId,
            @Param("status") String status,
            Pageable pageable
    );

    @Query("""
            SELECT item
            FROM MessageKnowledgeItem item
            WHERE item.storeId = :storeId
              AND item.status = :status
              AND item.roomTypeId = :roomTypeId
            ORDER BY item.lastSeenAt DESC, item.id DESC
            """)
    List<MessageKnowledgeItem> findActiveByStoreIdAndRoomTypeId(
            @Param("storeId") Long storeId,
            @Param("roomTypeId") Long roomTypeId,
            @Param("status") String status,
            Pageable pageable
    );

    @Query("""
            SELECT item
            FROM MessageKnowledgeItem item
            WHERE item.storeId = :storeId
              AND item.status = :status
              AND item.scopeType = 'STORE'
            ORDER BY item.lastSeenAt DESC, item.id DESC
            """)
    List<MessageKnowledgeItem> findActiveStoreScopedByStoreId(
            @Param("storeId") Long storeId,
            @Param("status") String status,
            Pageable pageable
    );

    @Query("""
            SELECT item
            FROM MessageKnowledgeItem item
            WHERE item.storeId = :storeId
              AND (:status IS NULL OR item.status = :status)
              AND (:scopeType IS NULL OR item.scopeType = :scopeType)
              AND (:topic IS NULL OR LOWER(item.topic) = :topic)
              AND (
                    :keyword IS NULL
                    OR LOWER(item.question) LIKE :keyword
                    OR LOWER(item.answer) LIKE :keyword
                    OR LOWER(COALESCE(item.semanticText, '')) LIKE :keyword
                    OR LOWER(COALESCE(item.searchIntentsJson, '')) LIKE :keyword
                    OR LOWER(item.topic) LIKE :keyword
                    OR LOWER(COALESCE(item.roomNumber, '')) LIKE :keyword
                    OR LOWER(COALESCE(item.roomTypeName, '')) LIKE :keyword
                  )
            ORDER BY item.updatedAt DESC, item.id DESC
            """)
    Page<MessageKnowledgeItem> searchManagementItems(
            @Param("storeId") Long storeId,
            @Param("status") String status,
            @Param("keyword") String keyword,
            @Param("scopeType") String scopeType,
            @Param("topic") String topic,
            Pageable pageable
    );

    @Query("""
            SELECT DISTINCT item.storeId
            FROM MessageKnowledgeItem item
            WHERE item.status = :itemStatus
              AND (item.embeddingStatus IS NULL OR item.embeddingStatus IN :embeddingStatuses)
              AND (item.embeddingNextAttemptAt IS NULL OR item.embeddingNextAttemptAt <= :now)
              AND (item.embeddingLeaseUntil IS NULL OR item.embeddingLeaseUntil <= :now)
            ORDER BY item.storeId ASC
            """)
    List<Long> findDueEmbeddingStoreIds(
            @Param("itemStatus") String itemStatus,
            @Param("embeddingStatuses") List<String> embeddingStatuses,
            @Param("now") LocalDateTime now,
            Pageable pageable
    );

    @Query("""
            SELECT item
            FROM MessageKnowledgeItem item
            WHERE item.storeId = :storeId
              AND item.status = :itemStatus
              AND (item.embeddingStatus IS NULL OR item.embeddingStatus IN :embeddingStatuses)
              AND (item.embeddingNextAttemptAt IS NULL OR item.embeddingNextAttemptAt <= :now)
              AND (item.embeddingLeaseUntil IS NULL OR item.embeddingLeaseUntil <= :now)
            ORDER BY item.embeddingUpdatedAt ASC, item.updatedAt ASC, item.id ASC
            """)
    List<MessageKnowledgeItem> findEmbeddingBackfillCandidates(
            @Param("storeId") Long storeId,
            @Param("itemStatus") String itemStatus,
            @Param("embeddingStatuses") List<String> embeddingStatuses,
            @Param("now") LocalDateTime now,
            Pageable pageable
    );

    @Query("""
            SELECT item
            FROM MessageKnowledgeItem item
            WHERE item.storeId = :storeId
              AND item.status = :itemStatus
              AND item.embeddingStatus = :embeddingStatus
              AND item.embeddingVector IS NOT NULL
              AND item.semanticText IS NOT NULL
            ORDER BY item.lastSeenAt DESC, item.id DESC
            """)
    List<MessageKnowledgeItem> findReadySemanticCandidatesByStoreId(
            @Param("storeId") Long storeId,
            @Param("itemStatus") String itemStatus,
            @Param("embeddingStatus") String embeddingStatus,
            Pageable pageable
    );

    @Query("""
            SELECT item
            FROM MessageKnowledgeItem item
            WHERE item.storeId = :storeId
              AND item.status = :itemStatus
              AND item.embeddingStatus = :embeddingStatus
              AND item.embeddingVector IS NOT NULL
              AND item.semanticText IS NOT NULL
              AND item.roomId = :roomId
            ORDER BY item.lastSeenAt DESC, item.id DESC
            """)
    List<MessageKnowledgeItem> findReadySemanticCandidatesByStoreIdAndRoomId(
            @Param("storeId") Long storeId,
            @Param("roomId") Long roomId,
            @Param("itemStatus") String itemStatus,
            @Param("embeddingStatus") String embeddingStatus,
            Pageable pageable
    );

    @Query("""
            SELECT item
            FROM MessageKnowledgeItem item
            WHERE item.storeId = :storeId
              AND item.status = :itemStatus
              AND item.embeddingStatus = :embeddingStatus
              AND item.embeddingVector IS NOT NULL
              AND item.semanticText IS NOT NULL
              AND item.roomTypeId = :roomTypeId
            ORDER BY item.lastSeenAt DESC, item.id DESC
            """)
    List<MessageKnowledgeItem> findReadySemanticCandidatesByStoreIdAndRoomTypeId(
            @Param("storeId") Long storeId,
            @Param("roomTypeId") Long roomTypeId,
            @Param("itemStatus") String itemStatus,
            @Param("embeddingStatus") String embeddingStatus,
            Pageable pageable
    );

    @Query("""
            SELECT item
            FROM MessageKnowledgeItem item
            WHERE item.storeId = :storeId
              AND item.status = :itemStatus
              AND item.embeddingStatus = :embeddingStatus
              AND item.embeddingVector IS NOT NULL
              AND item.semanticText IS NOT NULL
              AND item.scopeType = :scopeType
              AND item.scopeId = :scopeId
            ORDER BY item.lastSeenAt DESC, item.id DESC
            """)
    List<MessageKnowledgeItem> findReadySemanticCandidatesByStoreIdAndScope(
            @Param("storeId") Long storeId,
            @Param("scopeType") String scopeType,
            @Param("scopeId") Long scopeId,
            @Param("itemStatus") String itemStatus,
            @Param("embeddingStatus") String embeddingStatus,
            Pageable pageable
    );

    @Query("""
            SELECT item
            FROM MessageKnowledgeItem item
            WHERE item.storeId = :storeId
              AND item.status = :itemStatus
              AND item.embeddingStatus = :embeddingStatus
              AND item.embeddingVector IS NOT NULL
              AND item.semanticText IS NOT NULL
              AND item.scopeType = 'STORE'
            ORDER BY item.lastSeenAt DESC, item.id DESC
            """)
    List<MessageKnowledgeItem> findReadySemanticStoreScopedByStoreId(
            @Param("storeId") Long storeId,
            @Param("itemStatus") String itemStatus,
            @Param("embeddingStatus") String embeddingStatus,
            Pageable pageable
    );
}
