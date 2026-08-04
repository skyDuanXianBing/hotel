package server.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import server.demo.entity.ManagedOperationSettings;

import java.util.List;
import java.util.Optional;

public interface ManagedOperationSettingsRepository extends JpaRepository<ManagedOperationSettings, Long> {
    Optional<ManagedOperationSettings> findByStoreIdAndId(Long storeId, Long id);

    List<ManagedOperationSettings> findByStoreIdOrderByIdAsc(Long storeId);

    boolean existsByStoreIdAndPropertyName(Long storeId, String propertyName);
}
