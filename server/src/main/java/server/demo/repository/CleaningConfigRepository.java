package server.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import server.demo.entity.CleaningConfig;

import java.util.List;
import java.util.Optional;

/**
 * 保洁配置 Repository
 */
@Repository
public interface CleaningConfigRepository extends JpaRepository<CleaningConfig, Long> {

    /**
     * 根据用户ID查找保洁配置列表
     */
    List<CleaningConfig> findByUserId(Long userId);

    /**
     * 根据用户ID和门店ID查找保洁配置
     */
    Optional<CleaningConfig> findByUserIdAndStoreId(Long userId, Long storeId);
}
