package server.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import server.demo.entity.SortConfig;

import java.util.List;
import java.util.Optional;

@Repository
public interface SortConfigRepository extends JpaRepository<SortConfig, Long> {

    /**
     * 根据用户ID和排序类型查找所有排序配置
     */
    List<SortConfig> findByUserIdAndSortTypeOrderBySortOrderAsc(Long userId, String sortType);

    /**
     * 根据用户ID、排序类型和实体ID查找排序配置
     */
    Optional<SortConfig> findByUserIdAndSortTypeAndEntityId(Long userId, String sortType, Long entityId);

    /**
     * 删除指定用户和排序类型的所有配置
     */
    @Modifying
    @Query("DELETE FROM SortConfig sc WHERE sc.userId = :userId AND sc.sortType = :sortType")
    void deleteByUserIdAndSortType(@Param("userId") Long userId, @Param("sortType") String sortType);

    /**
     * 检查是否存在指定的排序配置
     */
    boolean existsByUserIdAndSortTypeAndEntityId(Long userId, String sortType, Long entityId);
}
