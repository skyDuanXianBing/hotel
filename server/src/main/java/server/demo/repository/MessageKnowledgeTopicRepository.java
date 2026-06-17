package server.demo.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import server.demo.entity.MessageKnowledgeTopic;

import java.util.List;
import java.util.Optional;

public interface MessageKnowledgeTopicRepository extends JpaRepository<MessageKnowledgeTopic, Long> {

    Optional<MessageKnowledgeTopic> findByStoreIdAndTopicCode(Long storeId, String topicCode);

    @Query("""
            SELECT topic
            FROM MessageKnowledgeTopic topic
            WHERE topic.storeId = :storeId
              AND topic.status = :status
            ORDER BY topic.topicCode ASC
            """)
    List<MessageKnowledgeTopic> findByStoreIdAndStatus(
            @Param("storeId") Long storeId,
            @Param("status") String status,
            Pageable pageable
    );

}
