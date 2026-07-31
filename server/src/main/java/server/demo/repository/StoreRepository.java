package server.demo.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import server.demo.entity.Store;

import java.util.List;
import java.util.Optional;

@Repository
public interface StoreRepository extends JpaRepository<Store, Long> {
    Optional<Store> findByName(String name);
    Optional<Store> findBySuHotelId(String suHotelId);
    List<Store> findAllBySuHotelIdOrderByIdAsc(String suHotelId);

    /**
     * 悲观写锁读取门店行（SELECT ... FOR UPDATE）。SaaS 购买/人工开通以此串行化同门店的
     * 订阅激活：锁内复查幂等键可把并发冲突转化为幂等重放，并避免并发双 ACTIVE 订阅。
     * 门店不存在时返回空（调用方按无锁继续，与既有行为一致）。
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Store s WHERE s.id = :id")
    Optional<Store> findByIdForUpdate(@Param("id") Long id);

    /**
     * 平台管理端门店选择器搜索：名称模糊匹配 + ID 精确匹配（keyword 为 null 时不过滤），
     * 按 id 升序，返回条数由 pageable 限制（调用方固定 limit 20）。
     */
    @Query("""
            SELECT s FROM Store s
            WHERE :keyword IS NULL
               OR LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR CAST(s.id AS string) = :keyword
            ORDER BY s.id ASC
            """)
    List<Store> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
}
