package server.demo.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import server.demo.entity.InternalTask;
import server.demo.enums.InternalTaskStatus;

import java.time.LocalDateTime;
import java.util.Optional;

public interface InternalTaskRepository extends JpaRepository<InternalTask, Long> {
    Optional<InternalTask> findByIdAndStoreId(Long id, Long storeId);
    Page<InternalTask> findByStoreIdAndArchivedAtIsNull(Long storeId, Pageable pageable);
    Page<InternalTask> findByStoreIdAndStatusAndArchivedAtIsNull(Long storeId, InternalTaskStatus status, Pageable pageable);
    Page<InternalTask> findByStoreIdAndAssigneeUserIdAndStatusAndArchivedAtIsNull(Long storeId, Long userId, InternalTaskStatus status, Pageable pageable);
    long countByStoreIdAndAssigneeUserIdAndStatusAndArchivedAtIsNull(Long storeId, Long userId, InternalTaskStatus status);
    long countByStoreIdAndStatusAndArchivedAtIsNull(Long storeId, InternalTaskStatus status);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update InternalTask t set t.status = :completed, t.completedByUserId = :userId, " +
            "t.completedByNameSnapshot = :name, t.completedAt = :now, t.updatedAt = :now, t.version = t.version + 1 " +
            "where t.id = :taskId and t.storeId = :storeId and t.assigneeUserId = :userId " +
            "and t.status = :assigned and t.archivedAt is null")
    int completeAssigned(@Param("taskId") Long taskId, @Param("storeId") Long storeId,
                         @Param("userId") Long userId, @Param("name") String name,
                         @Param("now") LocalDateTime now, @Param("assigned") InternalTaskStatus assigned,
                         @Param("completed") InternalTaskStatus completed);
}
