package server.demo.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import server.demo.entity.PriceLabsSyncLog;
import server.demo.enums.SyncStatus;
import server.demo.enums.SyncType;

import java.time.LocalDateTime;
import java.util.List;

/**
 * PriceLabs 同步日志 Repository
 */
@Repository
public interface PriceLabsSyncLogRepository extends JpaRepository<PriceLabsSyncLog, Long> {

    /**
     * 根据门店ID分页查询同步日志
     */
    Page<PriceLabsSyncLog> findByStoreIdOrderByCreatedAtDesc(Long storeId, Pageable pageable);

    /**
     * 根据门店ID和同步类型查询日志
     */
    List<PriceLabsSyncLog> findByStoreIdAndSyncTypeOrderByCreatedAtDesc(Long storeId, SyncType syncType);

    /**
     * 根据门店ID和状态查询日志
     */
    List<PriceLabsSyncLog> findByStoreIdAndStatusOrderByCreatedAtDesc(Long storeId, SyncStatus status);

    /**
     * 查询门店最近的同步日志
     */
    @Query("SELECT psl FROM PriceLabsSyncLog psl WHERE psl.storeId = :storeId ORDER BY psl.createdAt DESC")
    List<PriceLabsSyncLog> findRecentLogs(@Param("storeId") Long storeId, Pageable pageable);

    /**
     * 查询门店特定类型的最后一条日志
     */
    @Query("SELECT psl FROM PriceLabsSyncLog psl WHERE psl.storeId = :storeId AND psl.syncType = :syncType " +
           "ORDER BY psl.createdAt DESC LIMIT 1")
    PriceLabsSyncLog findLastLogByType(@Param("storeId") Long storeId, @Param("syncType") SyncType syncType);

    /**
     * 统计门店同步成功次数
     */
    long countByStoreIdAndStatus(Long storeId, SyncStatus status);

    /**
     * 统计门店特定时间段内的同步次数
     */
    @Query("SELECT COUNT(psl) FROM PriceLabsSyncLog psl WHERE psl.storeId = :storeId " +
           "AND psl.createdAt BETWEEN :startTime AND :endTime")
    long countByStoreIdAndTimePeriod(
            @Param("storeId") Long storeId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * 删除旧的同步日志
     */
    @Modifying
    @Query("DELETE FROM PriceLabsSyncLog psl WHERE psl.createdAt < :beforeTime")
    int deleteOldLogs(@Param("beforeTime") LocalDateTime beforeTime);

    /**
     * 删除门店的所有同步日志
     */
    void deleteByStoreId(Long storeId);
}
