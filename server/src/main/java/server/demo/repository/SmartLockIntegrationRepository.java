package server.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import server.demo.entity.SmartLockIntegration;
import server.demo.enums.SmartLockProvider;

import java.util.List;
import java.util.Optional;

@Repository
public interface SmartLockIntegrationRepository extends JpaRepository<SmartLockIntegration, Long> {
    List<SmartLockIntegration> findByStoreIdOrderByCreatedAtDesc(Long storeId);

    Optional<SmartLockIntegration> findByStoreIdAndId(Long storeId, Long id);

    Optional<SmartLockIntegration> findByStoreIdAndProvider(Long storeId, SmartLockProvider provider);
}
