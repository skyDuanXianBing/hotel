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
     * 根据用户ID查找易耗品列表
     */
    List<CleaningSupply> findByUserId(Long userId);

    /**
     * 根据用户ID和房型查找易耗品
     */
    Optional<CleaningSupply> findByUserIdAndRoomType(Long userId, String roomType);
}
