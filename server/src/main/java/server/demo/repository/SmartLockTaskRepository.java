package server.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import server.demo.entity.SmartLockTask;
import server.demo.enums.SmartLockProvider;
import server.demo.enums.SmartLockTaskStatus;
import server.demo.enums.SmartLockTaskType;

import java.util.List;
import java.util.Optional;

@Repository
public interface SmartLockTaskRepository extends JpaRepository<SmartLockTask, Long> {
    Optional<SmartLockTask> findByStoreIdAndId(Long storeId, Long id);

    Optional<SmartLockTask> findByStoreIdAndIdempotencyKey(Long storeId, String idempotencyKey);

    List<SmartLockTask> findByProviderAndProviderTaskIdOrderByCreatedAtDesc(
            SmartLockProvider provider,
            String providerTaskId
    );

    @Query("""
            select task
            from SmartLockTask task
            where task.storeId = :storeId
              and task.passcodeRecord.id = :passcodeRecordId
              and task.taskType = :taskType
              and task.status = :status
              and task.providerTaskId is null
            order by task.createdAt desc, task.id desc
            """)
    List<SmartLockTask> findPasscodeTasksWithoutProviderTaskId(
            @Param("storeId") Long storeId,
            @Param("passcodeRecordId") Long passcodeRecordId,
            @Param("taskType") SmartLockTaskType taskType,
            @Param("status") SmartLockTaskStatus status
    );

    @Query("""
            select count(task) > 0
            from SmartLockTask task
            where task.storeId = :storeId
              and task.binding.id = :bindingId
              and task.status = :status
            """)
    boolean existsByBindingAndStatus(
            @Param("storeId") Long storeId,
            @Param("bindingId") Long bindingId,
            @Param("status") SmartLockTaskStatus status
    );
}
