package server.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import server.demo.entity.Cleaner;

import java.util.List;
import java.util.Optional;

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

    /**
     * 根据门店ID查找保洁员列表
     */
    List<Cleaner> findByStoreId(Long storeId);

    List<Cleaner> findByStoreIdAndIsActiveTrue(Long storeId);

    List<Cleaner> findByStoreIdAndEmailIgnoreCase(Long storeId, String email);

    /**
     * 根据邮箱查找保洁员
     */
    Optional<Cleaner> findByEmail(String email);
}
