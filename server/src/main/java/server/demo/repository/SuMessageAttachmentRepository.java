package server.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import server.demo.entity.SuMessageAttachment;

import java.util.Optional;

public interface SuMessageAttachmentRepository extends JpaRepository<SuMessageAttachment, Long> {
    Optional<SuMessageAttachment> findByStoreIdAndThread_IdAndMessage_IdAndId(
            Long storeId,
            Long threadId,
            Long messageId,
            Long id
    );
}
