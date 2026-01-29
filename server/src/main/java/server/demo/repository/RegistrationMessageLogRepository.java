package server.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import server.demo.entity.RegistrationMessageLog;

import java.util.List;

@Repository
public interface RegistrationMessageLogRepository extends JpaRepository<RegistrationMessageLog, Long> {
    List<RegistrationMessageLog> findByFormIdOrderByCreatedAtDesc(Long formId);
}
