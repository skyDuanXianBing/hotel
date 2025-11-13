package server.demo.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import server.demo.entity.CleaningTask;

import java.time.LocalDate;
import java.util.List;

/**
 * 保洁任务Repository接口
 */
@Repository
public interface CleaningTaskRepository extends JpaRepository<CleaningTask, Long> {

    /**
     * 根据日期范围查询任务
     */
    List<CleaningTask> findByTaskDateBetween(LocalDate startDate, LocalDate endDate);

    /**
     * 根据日期范围和userId查询任务
     */
    @Query("SELECT ct FROM CleaningTask ct " +
           "WHERE ct.room.userId = :userId " +
           "AND ct.taskDate BETWEEN :startDate AND :endDate")
    List<CleaningTask> findByTaskDateBetweenAndUserId(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * 根据任务状态查询
     */
    List<CleaningTask> findByStatus(String status);

    /**
     * 根据房间ID查询任务
     */
    List<CleaningTask> findByRoomId(Long roomId);

    /**
     * 根据保洁员ID查询任务
     */
    List<CleaningTask> findByCleanerId(Long cleanerId);

    /**
     * 根据任务类型查询
     */
    List<CleaningTask> findByTaskType(String taskType);

    /**
     * 综合查询(支持多条件筛选和分页)
     */
    @Query("SELECT ct FROM CleaningTask ct " +
           "WHERE ct.room.userId = :userId " +
           "AND (:startDate IS NULL OR ct.taskDate >= :startDate) " +
           "AND (:endDate IS NULL OR ct.taskDate <= :endDate) " +
           "AND (:status IS NULL OR ct.status = :status) " +
           "AND (:taskType IS NULL OR ct.taskType = :taskType) " +
           "AND (:roomId IS NULL OR ct.room.id = :roomId) " +
           "AND (:cleanerId IS NULL OR ct.cleaner.id = :cleanerId) " +
           "AND (:roomTypeId IS NULL OR ct.room.roomType.id = :roomTypeId)")
    Page<CleaningTask> findWithFilters(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("status") String status,
            @Param("taskType") String taskType,
            @Param("roomId") Long roomId,
            @Param("cleanerId") Long cleanerId,
            @Param("roomTypeId") Long roomTypeId,
            Pageable pageable
    );

    /**
     * 统计指定日期范围内各状态的任务数量
     */
    @Query("SELECT ct.status, COUNT(ct) FROM CleaningTask ct " +
           "WHERE ct.room.userId = :userId " +
           "AND ct.taskDate BETWEEN :startDate AND :endDate " +
           "GROUP BY ct.status")
    List<Object[]> countByStatusInDateRange(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * 查询指定日期和房间的任务
     */
    List<CleaningTask> findByTaskDateAndRoomId(LocalDate taskDate, Long roomId);

    /**
     * 删除指定日期之前的任务
     */
    void deleteByTaskDateBefore(LocalDate date);
}
