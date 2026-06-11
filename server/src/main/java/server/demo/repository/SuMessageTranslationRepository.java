package server.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import server.demo.entity.SuMessageTranslation;

import java.util.Optional;

public interface SuMessageTranslationRepository extends JpaRepository<SuMessageTranslation, Long> {
    Optional<SuMessageTranslation> findFirstByStoreIdAndMessage_IdAndTargetLanguageAndSourceContentHashAndTranslationStatus(
            Long storeId,
            Long messageId,
            String targetLanguage,
            String sourceContentHash,
            String translationStatus
    );
}
