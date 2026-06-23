package server.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import server.demo.entity.SmartLockConfirmation;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface SmartLockConfirmationRepository extends JpaRepository<SmartLockConfirmation, Long> {
    Optional<SmartLockConfirmation> findByStoreIdAndTokenHash(Long storeId, String tokenHash);

    @Query("""
            select count(confirmation) > 0
            from SmartLockConfirmation confirmation
            where confirmation.storeId = :storeId
              and confirmation.binding.id = :bindingId
              and confirmation.usedAt is null
              and confirmation.expiresAt > :now
            """)
    boolean existsUnfinishedForBinding(
            @Param("storeId") Long storeId,
            @Param("bindingId") Long bindingId,
            @Param("now") LocalDateTime now
    );
}
