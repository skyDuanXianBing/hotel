package server.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import server.demo.entity.AutoMessageSendLog;

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
}

