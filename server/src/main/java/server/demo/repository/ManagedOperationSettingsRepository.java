package server.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import server.demo.entity.ManagedOperationSettings;

import java.util.Optional;

public interface ManagedOperationSettingsRepository extends JpaRepository<ManagedOperationSettings, Long> {
    Optional<ManagedOperationSettings> findByStoreId(Long storeId);
}
