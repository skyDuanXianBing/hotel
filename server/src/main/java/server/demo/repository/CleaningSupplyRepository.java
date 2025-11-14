package server.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import server.demo.entity.CleaningSupply;

import java.util.List;
import java.util.Optional;

/**
 * 保洁易耗品 Repository
 */
@Repository
public interface CleaningSupplyRepository extends JpaRepository<CleaningSupply, Long> {

    /**
     * 根据用户ID查找易耗品列表(已废弃,使用门店级查询)
     */
    @Deprecated
    List<CleaningSupply> findByUserId(Long userId);

    /**
     * 根据用户ID和房型查找易耗品(已废弃,使用门店级查询)
     */
    @Deprecated
    Optional<CleaningSupply> findByUserIdAndRoomType(Long userId, String roomType);

    // 门店级查询方法
    List<CleaningSupply> findByStoreId(Long storeId);

    Optional<CleaningSupply> findByStoreIdAndRoomType(Long storeId, String roomType);
}
