package server.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import server.demo.entity.AutoMessageSendLog;

import java.util.List;
import java.util.Optional;

public interface AutoMessageSendLogRepository extends JpaRepository<AutoMessageSendLog, Long> {
    boolean existsByStoreIdAndActionAndTargetTypeAndTargetId(Long storeId, String action, String targetType, Long targetId);

    Optional<AutoMessageSendLog> findByStoreIdAndActionAndTargetTypeAndTargetId(Long storeId, String action, String targetType, Long targetId);

    Optional<AutoMessageSendLog> findByStoreIdAndAutoMessageIdAndTargetTypeAndTargetId(
            Long storeId,
            Long autoMessageId,
            String targetType,
            Long targetId
    );

    @Query("""
            SELECT log
            FROM AutoMessageSendLog log
            WHERE log.storeId = :storeId
              AND log.targetType = :targetType
              AND (:targetId IS NULL OR log.targetId = :targetId)
              AND (:autoMessageId IS NULL OR log.autoMessageId = :autoMessageId)
              AND (:success IS NULL OR log.success = :success)
            ORDER BY log.createdAt DESC, log.id DESC
            """)
    List<AutoMessageSendLog> findRecentByStoreIdAndFilters(
            @Param("storeId") Long storeId,
            @Param("targetType") String targetType,
            @Param("targetId") Long targetId,
            @Param("autoMessageId") Long autoMessageId,
            @Param("success") Boolean success,
            Pageable pageable
    );
}
