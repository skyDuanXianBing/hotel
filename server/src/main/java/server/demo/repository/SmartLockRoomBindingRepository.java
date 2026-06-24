package server.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    @Query("""
            select binding
            from SmartLockRoomBinding binding
            where binding.storeId = :storeId
              and binding.provider = :provider
              and binding.status = :status
              and (:excludedBindingId is null or binding.id <> :excludedBindingId)
              and (
                    binding.controlProviderLockId = :providerLockId
                 or binding.passcodeProviderLockId = :providerLockId
                 or binding.providerLockId = :providerLockId
              )
            """)
    List<SmartLockRoomBinding> findActiveByAnyRoleProviderLockId(
            @Param("storeId") Long storeId,
            @Param("provider") SmartLockProvider provider,
            @Param("providerLockId") String providerLockId,
            @Param("status") SmartLockBindingStatus status,
            @Param("excludedBindingId") Long excludedBindingId
    );
}
