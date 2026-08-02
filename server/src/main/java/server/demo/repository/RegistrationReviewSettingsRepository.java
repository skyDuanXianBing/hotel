package server.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import server.demo.entity.RegistrationReviewSettings;

import java.util.Optional;

@Repository
public interface RegistrationReviewSettingsRepository extends JpaRepository<RegistrationReviewSettings, Long> {

    Optional<RegistrationReviewSettings> findByStoreId(Long storeId);
}
