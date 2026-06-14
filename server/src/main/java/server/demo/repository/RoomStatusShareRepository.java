package server.demo.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import server.demo.entity.RoomStatusShare;

import java.util.Optional;

@Repository
public interface RoomStatusShareRepository extends JpaRepository<RoomStatusShare, Long> {
    
    /**
     * 根据分享token查找分享记录
     */
    Optional<RoomStatusShare> findByShareToken(String shareToken);
    
    /**
     * 检查分享token是否存在
     */
    boolean existsByShareToken(String shareToken);
    
    /**
     * 分页查询激活的分享记录
     */
    @Query("SELECT r FROM RoomStatusShare r WHERE r.storeId = :storeId AND r.isActive = true ORDER BY r.createdAt DESC")
    Page<RoomStatusShare> findActiveSharesByStoreId(@Param("storeId") Long storeId, Pageable pageable);

    Optional<RoomStatusShare> findByStoreIdAndId(Long storeId, Long id);
    
    /**
     * 根据分享标题查找
     */
    Optional<RoomStatusShare> findByShareTitle(String shareTitle);
    
    /**
     * 检查分享标题是否存在（排除指定ID）
     */
    @Query("SELECT COUNT(r) > 0 FROM RoomStatusShare r WHERE r.storeId = :storeId AND r.shareTitle = :title AND r.id != :excludeId")
    boolean existsByStoreIdAndShareTitleAndIdNot(
            @Param("storeId") Long storeId,
            @Param("title") String shareTitle,
            @Param("excludeId") Long excludeId
    );
    
    /**
     * 检查分享标题是否存在
     */
    boolean existsByStoreIdAndShareTitle(Long storeId, String shareTitle);
}
