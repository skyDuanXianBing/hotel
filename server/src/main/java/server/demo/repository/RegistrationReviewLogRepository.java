package server.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import server.demo.entity.RegistrationReviewLog;

import java.util.List;

@Repository
public interface RegistrationReviewLogRepository extends JpaRepository<RegistrationReviewLog, Long> {
    List<RegistrationReviewLog> findByFormIdOrderByCreatedAtDesc(Long formId);
}
