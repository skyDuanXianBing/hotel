package server.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import server.demo.entity.SmartLockPasscodeRecord;
import server.demo.enums.SmartLockPasscodeStatus;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface SmartLockPasscodeRecordRepository extends JpaRepository<SmartLockPasscodeRecord, Long> {
    Optional<SmartLockPasscodeRecord> findByStoreIdAndId(Long storeId, Long id);

    List<SmartLockPasscodeRecord> findByStoreIdAndRoomIdOrderByCreatedAtDesc(Long storeId, Long roomId);

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
