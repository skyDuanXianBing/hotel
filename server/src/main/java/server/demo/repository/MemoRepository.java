package server.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import server.demo.entity.Memo;

import java.util.Optional;

@Repository
public interface MemoRepository extends JpaRepository<Memo, Long> {

    /**
     * 根据门店ID查询备忘录（门店级架构）
     * 每个门店只有一条备忘录记录
     */
    Optional<Memo> findByStoreId(Long storeId);

    /**
     * 检查门店是否已有备忘录
     */
    boolean existsByStoreId(Long storeId);

    /**
     * @deprecated 已废弃，使用findByStoreId替代
     */
    @Deprecated
    Optional<Memo> findByUserId(Long userId);

    /**
     * @deprecated 已废弃，使用existsByStoreId替代
     */
    @Deprecated
    boolean existsByUserId(Long userId);
}
