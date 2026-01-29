package server.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import server.demo.entity.RoomTypePricePlan;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomTypePricePlanRepository extends JpaRepository<RoomTypePricePlan, Long> {

    boolean existsByStoreId(Long storeId);

    // 根据房型ID查找所有价格计划关联
    @Query("SELECT rtp FROM RoomTypePricePlan rtp JOIN FETCH rtp.pricePlan WHERE rtp.roomType.id = :roomTypeId")
    List<RoomTypePricePlan> findByRoomTypeId(@Param("roomTypeId") Long roomTypeId);

    // 根据价格计划ID查找所有房型关联
    @Query("SELECT rtp FROM RoomTypePricePlan rtp JOIN FETCH rtp.roomType WHERE rtp.pricePlan.id = :pricePlanId")
    List<RoomTypePricePlan> findByPricePlanId(@Param("pricePlanId") Long pricePlanId);

    // 根据房型ID和价格计划ID查找
    Optional<RoomTypePricePlan> findByRoomTypeIdAndPricePlanId(Long roomTypeId, Long pricePlanId);

    // 检查房型和价格计划是否已关联
    boolean existsByRoomTypeIdAndPricePlanId(Long roomTypeId, Long pricePlanId);

    // 删除房型的所有价格计划关联
    void deleteByRoomTypeId(Long roomTypeId);

    // 删除价格计划的所有房型关联
    void deleteByPricePlanId(Long pricePlanId);

    // 删除门店下价格计划的所有房型关联（门店级隔离）
    void deleteByStoreIdAndPricePlanId(Long storeId, Long pricePlanId);

    // 统计价格计划关联的房型数量
    @Query("SELECT COUNT(rtp) FROM RoomTypePricePlan rtp WHERE rtp.pricePlan.id = :pricePlanId")
    long countByPricePlanId(@Param("pricePlanId") Long pricePlanId);

    @Query("SELECT rtp FROM RoomTypePricePlan rtp " +
            "JOIN FETCH rtp.roomType " +
            "JOIN FETCH rtp.pricePlan " +
            "WHERE rtp.storeId = :storeId")
    List<RoomTypePricePlan> findByStoreIdWithRoomTypeAndPricePlan(@Param("storeId") Long storeId);
}
