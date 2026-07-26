package server.demo.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import server.demo.entity.PaymentAttempt;
import server.demo.enums.PaymentAttemptStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentAttemptRepository extends JpaRepository<PaymentAttempt, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT attempt
            FROM PaymentAttempt attempt
            JOIN FETCH attempt.site
            WHERE attempt.storeId = :storeId
              AND attempt.idempotencyKey = :idempotencyKey
            """)
    Optional<PaymentAttempt> findByStoreIdAndIdempotencyKeyWithSite(
            @Param("storeId") Long storeId,
            @Param("idempotencyKey") String idempotencyKey
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT attempt
            FROM PaymentAttempt attempt
            JOIN FETCH attempt.site
            WHERE attempt.storeId = :storeId
              AND attempt.publicReference = :publicReference
            """)
    Optional<PaymentAttempt> findByStoreIdAndPublicReferenceForUpdate(
            @Param("storeId") Long storeId,
            @Param("publicReference") String publicReference
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT attempt
            FROM PaymentAttempt attempt
            JOIN FETCH attempt.site
            WHERE attempt.publicReference = :publicReference
            """)
    Optional<PaymentAttempt> findByPublicReferenceForUpdate(@Param("publicReference") String publicReference);

    /**
     * webhook 路由专用只读查找：验签前仅凭 metadata 引用定位门店，不加锁、不触发任何状态变更。
     */
    Optional<PaymentAttempt> findByPublicReference(String publicReference);

    @Query("""
            SELECT attempt.publicReference
            FROM PaymentAttempt attempt
            WHERE attempt.status = :status
              AND attempt.expiresAt <= :now
            ORDER BY attempt.id
            """)
    List<String> findExpiredPublicReferences(
            @Param("status") PaymentAttemptStatus status,
            @Param("now") LocalDateTime now,
            Pageable pageable
    );

    boolean existsBySite_Id(Long siteId);
}
