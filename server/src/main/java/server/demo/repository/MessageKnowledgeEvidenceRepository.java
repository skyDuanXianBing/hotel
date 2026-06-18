package server.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import server.demo.entity.MessageKnowledgeEvidence;

import java.util.List;
import java.util.Optional;

public interface MessageKnowledgeEvidenceRepository extends JpaRepository<MessageKnowledgeEvidence, Long> {

    Optional<MessageKnowledgeEvidence> findByStoreIdAndSourceFingerprint(
            Long storeId,
            String sourceFingerprint
    );

    boolean existsByStoreIdAndSourceFingerprint(
            Long storeId,
            String sourceFingerprint
    );

    @Query("""
            SELECT evidence
            FROM MessageKnowledgeEvidence evidence
            WHERE evidence.storeId = :storeId
              AND evidence.item.id = :itemId
            ORDER BY evidence.sourceTimestamp DESC, evidence.id DESC
            """)
    List<MessageKnowledgeEvidence> findByStoreIdAndItemIdOrderBySourceTimestampDesc(
            @Param("storeId") Long storeId,
            @Param("itemId") Long itemId
    );

    @Query("""
            SELECT DISTINCT evidence.language
            FROM MessageKnowledgeEvidence evidence
            WHERE evidence.storeId = :storeId
              AND evidence.item.id = :itemId
              AND evidence.language IS NOT NULL
              AND evidence.language <> ''
            ORDER BY evidence.language
            """)
    List<String> findDistinctLanguagesByStoreIdAndItemId(
            @Param("storeId") Long storeId,
            @Param("itemId") Long itemId
    );
}
