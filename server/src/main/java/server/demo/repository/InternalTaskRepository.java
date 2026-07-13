package server.demo.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import server.demo.entity.InternalTask;
import server.demo.enums.InternalTaskStatus;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;

public interface InternalTaskRepository extends JpaRepository<InternalTask, Long> {
    Optional<InternalTask> findByIdAndStoreId(Long id, Long storeId);
    Page<InternalTask> findByStoreIdAndArchivedAtIsNull(Long storeId, Pageable pageable);
    Page<InternalTask> findByStoreIdAndStatusAndArchivedAtIsNull(Long storeId, InternalTaskStatus status, Pageable pageable);
    @Query("select t from InternalTask t where t.storeId=:storeId and t.archivedAt is null " +
           "and (t.createdByUserId=:userId or t.assigneeUserId=:userId) and t.status=:status")
    Page<InternalTask> findVisibleToUser(@Param("storeId") Long storeId, @Param("userId") Long userId,
                                         @Param("status") InternalTaskStatus status, Pageable pageable);

    @Query("select count(t) from InternalTask t where t.storeId=:storeId and t.archivedAt is null " +
           "and (t.createdByUserId=:userId or t.assigneeUserId=:userId) and t.status=:status")
    long countVisibleToUser(@Param("storeId") Long storeId, @Param("userId") Long userId,
                            @Param("status") InternalTaskStatus status);
    long countByStoreIdAndStatusAndArchivedAtIsNull(Long storeId, InternalTaskStatus status);

    @Query("""
            select t from InternalTask t
            where t.storeId = :storeId and t.archivedAt is null
              and (:manager = true or t.createdByUserId = :userId or t.assigneeUserId = :userId)
              and (:status is null or t.status = :status)
              and (t.status <> server.demo.enums.InternalTaskStatus.COMPLETED
                   or t.completedAt >= :completedSince)
              and (:hasCursor = false
                   or (case when t.status = server.demo.enums.InternalTaskStatus.UNASSIGNED then 0
                            when t.status = server.demo.enums.InternalTaskStatus.COMPLETED then 2 else 1 end) > :cursorPriority
                   or ((case when t.status = server.demo.enums.InternalTaskStatus.UNASSIGNED then 0
                             when t.status = server.demo.enums.InternalTaskStatus.COMPLETED then 2 else 1 end) = :cursorPriority
                       and coalesce(t.completedAt, t.updatedAt) > :cursorDueAt)
                   or ((case when t.status = server.demo.enums.InternalTaskStatus.UNASSIGNED then 0
                             when t.status = server.demo.enums.InternalTaskStatus.COMPLETED then 2 else 1 end) = :cursorPriority
                       and coalesce(t.completedAt, t.updatedAt) = :cursorDueAt and t.id > :cursorId))
            order by case when t.status = server.demo.enums.InternalTaskStatus.UNASSIGNED then 0
                          when t.status = server.demo.enums.InternalTaskStatus.COMPLETED then 2 else 1 end,
                     coalesce(t.completedAt, t.updatedAt), t.id
            """)
    List<InternalTask> findHomeSlice(@Param("storeId") Long storeId, @Param("userId") Long userId,
                                     @Param("manager") boolean manager, @Param("status") InternalTaskStatus status,
                                     @Param("completedSince") LocalDateTime completedSince,
                                     @Param("hasCursor") boolean hasCursor, @Param("cursorPriority") int cursorPriority,
                                     @Param("cursorDueAt") LocalDateTime cursorDueAt, @Param("cursorId") Long cursorId,
                                     Pageable pageable);

    @Query("select count(t) from InternalTask t where t.storeId=:storeId and t.archivedAt is null " +
           "and (:manager=true or t.createdByUserId=:userId or t.assigneeUserId=:userId) and t.status=:status " +
           "and (:completedSince is null or t.completedAt >= :completedSince)")
    long countHome(@Param("storeId") Long storeId, @Param("userId") Long userId,
                   @Param("manager") boolean manager, @Param("status") InternalTaskStatus status,
                   @Param("completedSince") LocalDateTime completedSince);

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
