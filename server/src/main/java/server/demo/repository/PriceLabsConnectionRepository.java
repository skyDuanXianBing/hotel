package server.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import server.demo.entity.PriceLabsConnection;

import java.util.List;
import java.util.Optional;

/**
 * PriceLabs 连接配置 Repository
 */
@Repository
public interface PriceLabsConnectionRepository extends JpaRepository<PriceLabsConnection, Long> {

    /**
     * 根据门店ID查找所有连接
     */
    List<PriceLabsConnection> findByStoreId(Long storeId);

    /**
     * 根据门店ID查找启用的连接
     */
    List<PriceLabsConnection> findByStoreIdAndIsEnabledTrue(Long storeId);

    /**
     * 根据房型ID查找连接
     */
    List<PriceLabsConnection> findByRoomTypeId(Long roomTypeId);

    /**
     * 根据价格计划ID查找连接
     */
    List<PriceLabsConnection> findByPricePlanId(Long pricePlanId);

    /**
     * 根据房型ID和价格计划ID查找连接
     */
    Optional<PriceLabsConnection> findByRoomTypeIdAndPricePlanId(Long roomTypeId, Long pricePlanId);

    /**
     * 根据 PriceLabs listing_id 查找连接
     */
    Optional<PriceLabsConnection> findByPriceLabsListingId(String priceLabsListingId);

    /**
     * 根据门店ID和同步状态查找连接
     */
    List<PriceLabsConnection> findByStoreIdAndSyncStatus(Long storeId, String syncStatus);

    /**
     * 查找门店下已连接的房型数量
     */
    @Query("SELECT COUNT(DISTINCT pc.roomType.id) FROM PriceLabsConnection pc WHERE pc.storeId = :storeId AND pc.isEnabled = true")
    long countConnectedRoomTypes(@Param("storeId") Long storeId);

    /**
     * 删除门店下的所有连接
     */
    void deleteByStoreId(Long storeId);
}
