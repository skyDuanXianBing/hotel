package server.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import server.demo.entity.SmartLockRoomBinding;
import server.demo.enums.SmartLockBindingStatus;
import server.demo.enums.SmartLockProvider;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface SmartLockRoomBindingRepository extends JpaRepository<SmartLockRoomBinding, Long> {
    Optional<SmartLockRoomBinding> findByStoreIdAndId(Long storeId, Long id);

    Optional<SmartLockRoomBinding> findByStoreIdAndIdAndStatus(
            Long storeId,
            Long id,
            SmartLockBindingStatus status
    );

    Optional<SmartLockRoomBinding> findByStoreIdAndRoomId(Long storeId, Long roomId);

    Optional<SmartLockRoomBinding> findByStoreIdAndRoomIdAndStatus(
            Long storeId,
            Long roomId,
            SmartLockBindingStatus status
    );

    List<SmartLockRoomBinding> findByStoreIdAndRoomIdIn(Long storeId, Collection<Long> roomIds);

    List<SmartLockRoomBinding> findByStoreIdAndRoomIdInAndStatus(
            Long storeId,
            Collection<Long> roomIds,
            SmartLockBindingStatus status
    );

    Optional<SmartLockRoomBinding> findByStoreIdAndProviderAndProviderLockId(
            Long storeId,
            SmartLockProvider provider,
            String providerLockId
    );

    Optional<SmartLockRoomBinding> findByStoreIdAndProviderAndProviderLockIdAndStatus(
            Long storeId,
            SmartLockProvider provider,
            String providerLockId,
            SmartLockBindingStatus status
    );
}
