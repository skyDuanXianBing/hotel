package server.demo.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import server.demo.entity.SmartLockPasscodeRecord;
import server.demo.enums.SmartLockPasscodeStatus;
import server.demo.enums.SmartLockProvider;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface SmartLockPasscodeRecordRepository extends JpaRepository<SmartLockPasscodeRecord, Long> {
    Optional<SmartLockPasscodeRecord> findByStoreIdAndId(Long storeId, Long id);

    List<SmartLockPasscodeRecord> findByStoreIdAndRoomIdOrderByCreatedAtDesc(Long storeId, Long roomId);

    @Query("""
            select distinct record.storeId
            from SmartLockPasscodeRecord record
            where record.provider = :provider
              and (
                (
                  record.status = :pendingStatus
                  and record.providerPasscodeId is null
                  and record.passcodeName is not null
                  and record.passcodeName <> ''
                )
                or (
                  record.status = :deletePendingStatus
                  and record.providerPasscodeId is not null
                )
              )
            order by record.storeId asc
            """)
    List<Long> findDistinctStoreIdsForSwitchBotReconciliation(
            @Param("provider") SmartLockProvider provider,
            @Param("pendingStatus") SmartLockPasscodeStatus pendingStatus,
            @Param("deletePendingStatus") SmartLockPasscodeStatus deletePendingStatus
    );

    @Query("""
            select record
            from SmartLockPasscodeRecord record
            where record.storeId = :storeId
              and record.provider = :provider
              and (
                (
                  record.status = :pendingStatus
                  and record.providerPasscodeId is null
                  and record.passcodeName is not null
                  and record.passcodeName <> ''
                )
                or (
                  record.status = :deletePendingStatus
                  and record.providerPasscodeId is not null
                )
              )
            order by record.createdAt asc, record.id asc
            """)
    List<SmartLockPasscodeRecord> findSwitchBotReconciliationCandidates(
            @Param("storeId") Long storeId,
            @Param("provider") SmartLockProvider provider,
            @Param("pendingStatus") SmartLockPasscodeStatus pendingStatus,
            @Param("deletePendingStatus") SmartLockPasscodeStatus deletePendingStatus,
            Pageable pageable
    );

    @Query("""
            select count(record) > 0
            from SmartLockPasscodeRecord record
            where record.storeId = :storeId
              and record.binding.id = :bindingId
              and record.status in :statuses
            """)
    boolean existsRiskyStatusForBinding(
            @Param("storeId") Long storeId,
            @Param("bindingId") Long bindingId,
            @Param("statuses") Collection<SmartLockPasscodeStatus> statuses
    );
}
