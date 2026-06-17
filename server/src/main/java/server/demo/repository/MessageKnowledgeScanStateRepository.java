package server.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import server.demo.entity.MessageKnowledgeScanState;

import java.time.LocalDateTime;
import java.util.Optional;

public interface MessageKnowledgeScanStateRepository extends JpaRepository<MessageKnowledgeScanState, Long> {

    Optional<MessageKnowledgeScanState> findByStoreId(Long storeId);

    @Query(
            value = """
                    SELECT *
                    FROM message_knowledge_scan_states
                    WHERE (next_scan_after IS NULL OR next_scan_after <= :now)
                      AND (backoff_until IS NULL OR backoff_until <= :now)
                      AND (lease_until IS NULL OR lease_until < :now)
                    ORDER BY COALESCE(next_scan_after, '1970-01-01 00:00:00') ASC,
                             store_id ASC
                    LIMIT 1
                    FOR UPDATE SKIP LOCKED
                    """,
            nativeQuery = true
    )
    Optional<MessageKnowledgeScanState> findNextDueStateForUpdate(@Param("now") LocalDateTime now);

    @Modifying
    @Query(
            value = """
                    INSERT IGNORE INTO message_knowledge_scan_states (
                        store_id,
                        status,
                        last_scanned_message_id,
                        next_scan_after,
                        failure_count,
                        processed_message_count,
                        extracted_pair_count,
                        created_at,
                        updated_at
                    )
                    VALUES (
                        :storeId,
                        'IDLE',
                        0,
                        :now,
                        0,
                        0,
                        0,
                        :now,
                        :now
                    )
                    """,
            nativeQuery = true
    )
    int insertStateIfAbsent(
            @Param("storeId") Long storeId,
            @Param("now") LocalDateTime now
    );
}
