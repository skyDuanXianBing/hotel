package server.demo.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import server.demo.entity.PriceChangeHistory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PriceChangeHistoryRepository extends JpaRepository<PriceChangeHistory, Long> {

    // 按房型ID查询
    List<PriceChangeHistory> findByRoomTypeId(Long roomTypeId);

    // 按价格计划ID查询
    List<PriceChangeHistory> findByPricePlanId(Long pricePlanId);

    // 按操作人查询
    List<PriceChangeHistory> findByOperator(String operator);

    // 按操作时间范围查询
    @Query("SELECT pch FROM PriceChangeHistory pch WHERE pch.operateTime >= :startTime AND pch.operateTime <= :endTime " +
           "ORDER BY pch.operateTime DESC")
    List<PriceChangeHistory> findByOperateTimeRange(@Param("startTime") LocalDateTime startTime,
                                                     @Param("endTime") LocalDateTime endTime);

    // 复杂条件分页查询
    @Query("SELECT pch FROM PriceChangeHistory pch " +
           "WHERE (:roomTypeId IS NULL OR pch.roomType.id = :roomTypeId) " +
           "AND (:pricePlanId IS NULL OR pch.pricePlan.id = :pricePlanId) " +
           "AND (:operator IS NULL OR pch.operator = :operator) " +
           "AND (:operateStartTime IS NULL OR pch.operateTime >= :operateStartTime) " +
           "AND (:operateEndTime IS NULL OR pch.operateTime <= :operateEndTime) " +
           "AND (:priceDateStart IS NULL OR pch.priceDateEnd >= :priceDateStart) " +
           "AND (:priceDateEnd IS NULL OR pch.priceDateStart <= :priceDateEnd) " +
           "ORDER BY pch.operateTime DESC")
    Page<PriceChangeHistory> findByConditions(
            @Param("roomTypeId") Long roomTypeId,
            @Param("pricePlanId") Long pricePlanId,
            @Param("operator") String operator,
            @Param("operateStartTime") LocalDateTime operateStartTime,
            @Param("operateEndTime") LocalDateTime operateEndTime,
            @Param("priceDateStart") LocalDate priceDateStart,
            @Param("priceDateEnd") LocalDate priceDateEnd,
            Pageable pageable);

    // 复杂条件分页查询(带userId过滤)
    @Query("SELECT pch FROM PriceChangeHistory pch " +
           "WHERE pch.user.id = :userId " +
           "AND (:roomTypeId IS NULL OR pch.roomType.id = :roomTypeId) " +
           "AND (:pricePlanId IS NULL OR pch.pricePlan.id = :pricePlanId) " +
           "AND (:operator IS NULL OR pch.operator = :operator) " +
           "AND (:operateStartTime IS NULL OR pch.operateTime >= :operateStartTime) " +
           "AND (:operateEndTime IS NULL OR pch.operateTime <= :operateEndTime) " +
           "AND (:priceDateStart IS NULL OR pch.priceDateEnd >= :priceDateStart) " +
           "AND (:priceDateEnd IS NULL OR pch.priceDateStart <= :priceDateEnd) " +
           "ORDER BY pch.operateTime DESC")
    Page<PriceChangeHistory> findByConditionsAndUserId(
            @Param("userId") Long userId,
            @Param("roomTypeId") Long roomTypeId,
            @Param("pricePlanId") Long pricePlanId,
            @Param("operator") String operator,
            @Param("operateStartTime") LocalDateTime operateStartTime,
            @Param("operateEndTime") LocalDateTime operateEndTime,
            @Param("priceDateStart") LocalDate priceDateStart,
            @Param("priceDateEnd") LocalDate priceDateEnd,
            Pageable pageable);

    // 按房型ID和日期范围查询
    @Query("SELECT pch FROM PriceChangeHistory pch " +
           "WHERE pch.roomType.id = :roomTypeId " +
           "AND ((pch.priceDateStart <= :endDate AND pch.priceDateEnd >= :startDate)) " +
           "ORDER BY pch.operateTime DESC")
    List<PriceChangeHistory> findByRoomTypeIdAndPriceDateRange(
            @Param("roomTypeId") Long roomTypeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    // 删除指定房型的所有价格变更历史记录
    void deleteByRoomTypeId(Long roomTypeId);
}
