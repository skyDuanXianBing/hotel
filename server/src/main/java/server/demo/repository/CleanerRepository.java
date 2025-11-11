package server.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import server.demo.entity.Cleaner;

import java.util.List;

/**
 * 保洁员 Repository
 */
@Repository
public interface CleanerRepository extends JpaRepository<Cleaner, Long> {

    /**
     * 根据用户ID和门店ID查找保洁员列表
     */
    List<Cleaner> findByUserIdAndStoreId(Long userId, Long storeId);

    /**
     * 根据用户ID查找保洁员列表
     */
    List<Cleaner> findByUserId(Long userId);
}
