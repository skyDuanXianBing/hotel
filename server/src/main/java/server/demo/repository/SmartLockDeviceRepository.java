package server.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import server.demo.entity.SmartLockDevice;
import server.demo.enums.SmartLockProvider;

import java.util.List;
import java.util.Optional;

@Repository
public interface SmartLockDeviceRepository extends JpaRepository<SmartLockDevice, Long> {
    List<SmartLockDevice> findByStoreIdOrderByLockNameAsc(Long storeId);

    List<SmartLockDevice> findByStoreIdAndProviderOrderByLockNameAsc(
            Long storeId,
            SmartLockProvider provider
    );

    List<SmartLockDevice> findByStoreIdAndIntegrationIdOrderByLockNameAsc(Long storeId, Long integrationId);

    Optional<SmartLockDevice> findByStoreIdAndId(Long storeId, Long id);

    Optional<SmartLockDevice> findByStoreIdAndProviderAndProviderLockId(
            Long storeId,
            SmartLockProvider provider,
            String providerLockId
    );
}
