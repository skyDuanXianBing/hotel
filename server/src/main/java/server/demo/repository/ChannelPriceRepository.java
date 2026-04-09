package server.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import server.demo.entity.ChannelPrice;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 渠道价格 Repository
 */
@Repository
public interface ChannelPriceRepository extends JpaRepository<ChannelPrice, Long> {

    boolean existsByStoreIdAndPriceDateBetween(Long storeId, LocalDate startDate, LocalDate endDate);

    boolean existsByStoreIdAndPricePlanId(Long storeId, Long pricePlanId);

    /**
     * 根据门店ID查找所有渠道价格
     */
    List<ChannelPrice> findByStoreId(Long storeId);

    /**
     * 根据门店、房型和日期范围查找渠道价格（门店级隔离）
     */
    @Query("SELECT cp FROM ChannelPrice cp WHERE cp.storeId = :storeId AND cp.roomType.id = :roomTypeId " +
           "AND cp.priceDate BETWEEN :startDate AND :endDate ORDER BY cp.priceDate")
    List<ChannelPrice> findByStoreIdAndRoomTypeIdAndPriceDateBetween(
            @Param("storeId") Long storeId,
            @Param("roomTypeId") Long roomTypeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * 根据房型ID和日期范围查找渠道价格
     * @deprecated 请使用 findByStoreIdAndRoomTypeIdAndPriceDateBetween 以确保门店级隔离
     */
    @Deprecated
    List<ChannelPrice> findByRoomTypeIdAndPriceDateBetween(Long roomTypeId, LocalDate startDate, LocalDate endDate);

    /**
     * 根据门店、渠道和日期范围查找价格（门店级隔离）
     */
    @Query("SELECT cp FROM ChannelPrice cp WHERE cp.storeId = :storeId AND cp.channel.id = :channelId " +
           "AND cp.priceDate BETWEEN :startDate AND :endDate ORDER BY cp.priceDate")
    List<ChannelPrice> findByStoreIdAndChannelIdAndPriceDateBetween(
            @Param("storeId") Long storeId,
            @Param("channelId") Long channelId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * 根据渠道ID和日期范围查找价格
     * @deprecated 请使用 findByStoreIdAndChannelIdAndPriceDateBetween 以确保门店级隔离
     */
    @Deprecated
    List<ChannelPrice> findByChannelIdAndPriceDateBetween(Long channelId, LocalDate startDate, LocalDate endDate);

    /**
     * 根据门店、房型、价格计划、渠道和日期查找价格（门店级隔离）
     */
    Optional<ChannelPrice> findByStoreIdAndRoomTypeIdAndPricePlanIdAndChannelIdAndPriceDate(
            Long storeId, Long roomTypeId, Long pricePlanId, Long channelId, LocalDate priceDate);

    /**
     * 根据房型、价格计划、渠道和日期查找价格
     */
    Optional<ChannelPrice> findByRoomTypeIdAndPricePlanIdAndChannelIdAndPriceDate(
            Long roomTypeId, Long pricePlanId, Long channelId, LocalDate priceDate);

    /**
     * 根据门店、房型、渠道和日期范围查找价格
     */
    @Query("SELECT cp FROM ChannelPrice cp WHERE cp.storeId = :storeId " +
           "AND cp.roomType.id = :roomTypeId AND cp.channel.id = :channelId " +
           "AND cp.priceDate BETWEEN :startDate AND :endDate ORDER BY cp.priceDate")
    List<ChannelPrice> findByStoreAndRoomTypeAndChannelAndDateRange(
            @Param("storeId") Long storeId,
            @Param("roomTypeId") Long roomTypeId,
            @Param("channelId") Long channelId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT cp FROM ChannelPrice cp WHERE cp.storeId = :storeId " +
           "AND cp.roomType.id = :roomTypeId AND cp.pricePlan.id = :pricePlanId AND cp.priceDate = :priceDate " +
           "AND cp.priceLabsUpdatedAt IS NOT NULL ORDER BY cp.updatedAt DESC")
    List<ChannelPrice> findPriceLabsCandidatesByStoreAndRoomTypeAndPricePlanAndDate(
            @Param("storeId") Long storeId,
            @Param("roomTypeId") Long roomTypeId,
            @Param("pricePlanId") Long pricePlanId,
            @Param("priceDate") LocalDate priceDate);

    /**
     * 查找未同步到OTA的价格
     */
    @Query("SELECT cp FROM ChannelPrice cp WHERE cp.storeId = :storeId AND cp.isSyncedToOta = false " +
            "AND (cp.otaSyncState IS NULL OR cp.otaSyncState <> 'NOT_REQUIRED') ORDER BY cp.priceDate")
    List<ChannelPrice> findByStoreIdAndIsSyncedToOtaFalse(@Param("storeId") Long storeId);

    /**
     * 查找特定渠道未同步的价格（门店级隔离）
     */
    @Query("SELECT cp FROM ChannelPrice cp WHERE cp.storeId = :storeId AND cp.channel.id = :channelId " +
            "AND cp.isSyncedToOta = false AND (cp.otaSyncState IS NULL OR cp.otaSyncState <> 'NOT_REQUIRED') " +
            "ORDER BY cp.priceDate")
    List<ChannelPrice> findByStoreIdAndChannelIdAndIsSyncedToOtaFalse(
            @Param("storeId") Long storeId,
            @Param("channelId") Long channelId);

    @Query("SELECT cp FROM ChannelPrice cp WHERE cp.storeId = :storeId AND cp.channel.id = :channelId " +
            "AND cp.isSyncedToOta = false AND (cp.otaSyncState IS NULL OR cp.otaSyncState <> 'NOT_REQUIRED') " +
            "AND cp.priceDate BETWEEN :startDate AND :endDate " +
           "ORDER BY cp.roomType.id, cp.pricePlan.id, cp.priceDate")
    List<ChannelPrice> findUnsyncedByStoreIdAndChannelIdAndDateRange(
            @Param("storeId") Long storeId,
            @Param("channelId") Long channelId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT DISTINCT cp.storeId FROM ChannelPrice cp WHERE cp.isSyncedToOta = false " +
            "AND (cp.otaSyncState IS NULL OR cp.otaSyncState <> 'NOT_REQUIRED') " +
           "AND cp.priceDate BETWEEN :startDate AND :endDate AND cp.channel.code IN :channelCodes")
    List<Long> findDistinctStoreIdsWithUnsyncedPricesInRange(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("channelCodes") List<String> channelCodes);

    /**
     * 查找特定渠道未同步的价格
     * @deprecated 请使用 findByStoreIdAndChannelIdAndIsSyncedToOtaFalse 以确保门店级隔离
     */
    @Deprecated
    List<ChannelPrice> findByChannelIdAndIsSyncedToOtaFalse(Long channelId);

    /**
     * 批量更新OTA同步状态
     */
    @Modifying
    @Query("UPDATE ChannelPrice cp SET cp.isSyncedToOta = true, cp.otaSyncState = 'SUCCESS', cp.otaSyncAt = CURRENT_TIMESTAMP " +
           "WHERE cp.id IN :ids")
    int markAsSyncedToOta(@Param("ids") List<Long> ids);

    @Modifying
    @Query("UPDATE ChannelPrice cp SET cp.isSyncedToOta = false, cp.otaSyncState = 'FAILED' WHERE cp.id IN :ids")
    int markAsFailedToOta(@Param("ids") List<Long> ids);

    /**
     * 删除门店下指定日期之前的价格记录
     */
    @Modifying
    @Query("DELETE FROM ChannelPrice cp WHERE cp.storeId = :storeId AND cp.priceDate < :beforeDate")
    int deleteOldPrices(@Param("storeId") Long storeId, @Param("beforeDate") LocalDate beforeDate);

    /**
     * 根据门店和日期范围查找所有渠道价格
     */
    @Query("SELECT cp FROM ChannelPrice cp WHERE cp.storeId = :storeId " +
           "AND cp.priceDate BETWEEN :startDate AND :endDate ORDER BY cp.priceDate, cp.roomType.id, cp.channel.id")
    List<ChannelPrice> findByStoreIdAndDateRange(
            @Param("storeId") Long storeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT cp FROM ChannelPrice cp " +
            "JOIN FETCH cp.roomType " +
            "JOIN FETCH cp.pricePlan " +
            "JOIN FETCH cp.channel " +
            "WHERE cp.storeId = :storeId " +
            "AND cp.priceDate BETWEEN :startDate AND :endDate")
    List<ChannelPrice> findByStoreIdAndDateRangeWithRelations(
            @Param("storeId") Long storeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Modifying
    @Query("DELETE FROM ChannelPrice cp WHERE cp.storeId = :storeId AND cp.pricePlan.id = :pricePlanId")
    int deleteByStoreIdAndPricePlanId(@Param("storeId") Long storeId, @Param("pricePlanId") Long pricePlanId);

    @Modifying
    @Query("DELETE FROM ChannelPrice cp WHERE cp.storeId = :storeId AND cp.roomType.id = :roomTypeId")
    int deleteByStoreIdAndRoomTypeId(@Param("storeId") Long storeId, @Param("roomTypeId") Long roomTypeId);

    @Modifying
    @Query("DELETE FROM ChannelPrice cp WHERE cp.storeId = :storeId AND cp.channel.id = :channelId")
    int deleteByStoreIdAndChannelId(@Param("storeId") Long storeId, @Param("channelId") Long channelId);
}
