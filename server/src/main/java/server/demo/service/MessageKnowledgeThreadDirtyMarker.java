package server.demo.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.demo.entity.SuMessage;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
public class MessageKnowledgeThreadDirtyMarker {
    private static final long DEFAULT_DELAY_MINUTES = 30L;

    @PersistenceContext
    private EntityManager entityManager;

    private final Clock clock;

    @Value("${messaging.knowledge.thread-dirty-marker.delay-minutes:30}")
    private long delayMinutes = DEFAULT_DELAY_MINUTES;

    public MessageKnowledgeThreadDirtyMarker(Clock clock) {
        this.clock = clock;
    }

    @Transactional
    public boolean markDirty(SuMessage message) {
        if (message == null || message.getThread() == null) {
            return false;
        }
        return markDirty(
                message.getStoreId(),
                message.getThread().getId(),
                message.getId()
        );
    }

    @Transactional
    public boolean markDirty(Long storeId, Long threadId, Long messageId) {
        if (!hasPositiveId(storeId) || !hasPositiveId(threadId) || !hasPositiveId(messageId)) {
            return false;
        }

        LocalDateTime now = nowUtc();
        LocalDateTime extractAfter = now.plusMinutes(resolveDelayMinutes());
        int updated = entityManager
                .createNativeQuery("""
                        UPDATE su_message_threads
                        SET knowledge_pending = 1,
                            knowledge_extract_after = :extractAfter,
                            knowledge_dirty_message_id = :messageId,
                            knowledge_error = NULL,
                            updated_at = :now
                        WHERE store_id = :storeId
                          AND id = :threadId
                          AND (
                                knowledge_dirty_message_id IS NULL
                                OR knowledge_dirty_message_id <= :messageId
                              )
                        """)
                .setParameter("extractAfter", extractAfter)
                .setParameter("messageId", messageId)
                .setParameter("now", now)
                .setParameter("storeId", storeId)
                .setParameter("threadId", threadId)
                .executeUpdate();
        return updated == 1;
    }

    private LocalDateTime nowUtc() {
        return LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
    }

    private long resolveDelayMinutes() {
        if (delayMinutes < 0L) {
            return DEFAULT_DELAY_MINUTES;
        }
        return delayMinutes;
    }

    private static boolean hasPositiveId(Long value) {
        return value != null && value > 0L;
    }
}
