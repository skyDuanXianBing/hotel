package server.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import server.demo.entity.RoomPrice;
import server.demo.entity.RoomType;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface RoomPriceRepository extends JpaRepository<RoomPrice, Long> {
    
    // 根据房型ID和日期查找价格
    Optional<RoomPrice> findByRoomTypeIdAndPriceDate(Long roomTypeId, LocalDate priceDate);
    
    // 根据房型和日期查找价格
    Optional<RoomPrice> findByRoomTypeAndPriceDate(RoomType roomType, LocalDate priceDate);
    
    // 根据房型ID查找指定日期范围内的价格
    @Query("SELECT rp FROM RoomPrice rp WHERE rp.roomType.id = :roomTypeId " +
           "AND rp.priceDate >= :startDate AND rp.priceDate <= :endDate " +
           "ORDER BY rp.priceDate")
    List<RoomPrice> findByRoomTypeIdAndDateRange(@Param("roomTypeId") Long roomTypeId, 
                                                 @Param("startDate") LocalDate startDate, 
                                                 @Param("endDate") LocalDate endDate);
    
    // 根据日期范围查找所有房型的价格
    @Query("SELECT rp FROM RoomPrice rp JOIN FETCH rp.roomType " +
           "WHERE rp.priceDate >= :startDate AND rp.priceDate <= :endDate " +
           "ORDER BY rp.roomType.name, rp.priceDate")
    List<RoomPrice> findByDateRangeWithRoomType(@Param("startDate") LocalDate startDate, 
                                                @Param("endDate") LocalDate endDate);
    
    // 根据房型ID列表和日期范围查找价格
    @Query("SELECT rp FROM RoomPrice rp JOIN FETCH rp.roomType " +
           "WHERE rp.roomType.id IN :roomTypeIds " +
           "AND rp.priceDate >= :startDate AND rp.priceDate <= :endDate " +
           "ORDER BY rp.roomType.name, rp.priceDate")
    List<RoomPrice> findByRoomTypeIdsAndDateRange(@Param("roomTypeIds") List<Long> roomTypeIds,
                                                  @Param("startDate") LocalDate startDate, 
                                                  @Param("endDate") LocalDate endDate);
    
    // 删除指定房型和日期的价格
    void deleteByRoomTypeAndPriceDate(RoomType roomType, LocalDate priceDate);
    
    // 删除指定房型ID和日期范围的价格
    @Query("DELETE FROM RoomPrice rp WHERE rp.roomType.id = :roomTypeId " +
           "AND rp.priceDate >= :startDate AND rp.priceDate <= :endDate")
    void deleteByRoomTypeIdAndDateRange(@Param("roomTypeId") Long roomTypeId,
                                       @Param("startDate") LocalDate startDate, 
                                       @Param("endDate") LocalDate endDate);
    
    // 检查是否存在指定房型和日期的价格
    boolean existsByRoomTypeIdAndPriceDate(Long roomTypeId, LocalDate priceDate);

    // 门店级：检查价格计划是否已产生覆盖价记录
    boolean existsByStoreIdAndPricePlanId(Long storeId, Long pricePlanId);

    // 根据房型ID、价格计划ID和日期查找价格
    Optional<RoomPrice> findByRoomTypeIdAndPricePlanIdAndPriceDate(Long roomTypeId, Long pricePlanId, LocalDate priceDate);

    // 根据房型ID、价格计划ID和日期范围查找价格
    @Query("SELECT rp FROM RoomPrice rp WHERE rp.roomType.id = :roomTypeId " +
           "AND rp.pricePlan.id = :pricePlanId " +
           "AND rp.priceDate >= :startDate AND rp.priceDate <= :endDate " +
           "ORDER BY rp.priceDate")
    List<RoomPrice> findByRoomTypeIdAndPricePlanIdAndDateRange(@Param("roomTypeId") Long roomTypeId,
                                                                @Param("pricePlanId") Long pricePlanId,
                                                                @Param("startDate") LocalDate startDate,
                                                                @Param("endDate") LocalDate endDate);

    // 根据价格计划ID和日期范围查找所有价格
    @Query("SELECT rp FROM RoomPrice rp JOIN FETCH rp.roomType JOIN FETCH rp.pricePlan " +
           "WHERE rp.pricePlan.id = :pricePlanId " +
           "AND rp.priceDate >= :startDate AND rp.priceDate <= :endDate " +
           "ORDER BY rp.roomType.name, rp.priceDate")
    List<RoomPrice> findByPricePlanIdAndDateRange(@Param("pricePlanId") Long pricePlanId,
                                                   @Param("startDate") LocalDate startDate,
                                                   @Param("endDate") LocalDate endDate);

    // 根据日期范围查找所有价格(包含价格计划)
    @Query("SELECT rp FROM RoomPrice rp JOIN FETCH rp.roomType " +
           "LEFT JOIN FETCH rp.pricePlan " +
           "WHERE rp.priceDate >= :startDate AND rp.priceDate <= :endDate " +
           "ORDER BY rp.roomType.name, rp.pricePlan.id, rp.priceDate")
    List<RoomPrice> findByDateRangeWithRoomTypeAndPricePlan(@Param("startDate") LocalDate startDate,
                                                             @Param("endDate") LocalDate endDate);

    // 删除指定房型的所有价格记录
    void deleteByRoomTypeId(Long roomTypeId);

    // 根据门店ID、房型ID和日期范围查找价格（门店级查询）
    @Query("SELECT rp FROM RoomPrice rp " +
           "WHERE rp.storeId = :storeId " +
           "AND rp.roomType.id = :roomTypeId " +
           "AND rp.priceDate >= :startDate AND rp.priceDate <= :endDate " +
           "ORDER BY rp.priceDate")
    List<RoomPrice> findByStoreIdAndRoomTypeIdAndPriceDateBetween(
            @Param("storeId") Long storeId,
            @Param("roomTypeId") Long roomTypeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT rp FROM RoomPrice rp " +
            "JOIN FETCH rp.roomType " +
            "LEFT JOIN FETCH rp.pricePlan " +
            "WHERE rp.storeId = :storeId " +
            "AND rp.priceDate >= :startDate AND rp.priceDate <= :endDate " +
            "ORDER BY rp.roomType.id, rp.pricePlan.id, rp.priceDate")
    List<RoomPrice> findByStoreIdAndPriceDateBetweenWithRoomTypeAndPricePlan(
            @Param("storeId") Long storeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT rp FROM RoomPrice rp " +
            "JOIN FETCH rp.roomType " +
            "LEFT JOIN FETCH rp.pricePlan " +
            "WHERE rp.storeId = :storeId " +
            "AND rp.roomType.id IN :roomTypeIds " +
            "AND rp.priceDate >= :startDate AND rp.priceDate <= :endDate " +
            "ORDER BY rp.roomType.id, rp.pricePlan.id, rp.priceDate")
    List<RoomPrice> findByStoreIdAndRoomTypeIdsAndPriceDateBetweenWithRoomTypeAndPricePlan(
            @Param("storeId") Long storeId,
            @Param("roomTypeIds") List<Long> roomTypeIds,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Modifying
    @Query("DELETE FROM RoomPrice rp WHERE rp.storeId = :storeId " +
           "AND rp.roomType.id = :roomTypeId " +
           "AND rp.pricePlan.id = :pricePlanId")
       int deleteByStoreIdAndRoomTypeIdAndPricePlanId(
           @Param("storeId") Long storeId,
           @Param("roomTypeId") Long roomTypeId,
           @Param("pricePlanId") Long pricePlanId);

    @Modifying
    @Query("DELETE FROM RoomPrice rp WHERE rp.storeId = :storeId " +
           "AND rp.roomType.id = :roomTypeId " +
           "AND rp.pricePlan.id = :pricePlanId " +
           "AND rp.priceDate >= :fromDate")
       int deleteByStoreIdAndRoomTypeIdAndPricePlanIdAndPriceDateGreaterThanEqual(
           @Param("storeId") Long storeId,
           @Param("roomTypeId") Long roomTypeId,
           @Param("pricePlanId") Long pricePlanId,
           @Param("fromDate") LocalDate fromDate);
}
