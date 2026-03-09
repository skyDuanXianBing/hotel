package server.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import server.demo.entity.StoreUser;

import java.util.List;
import java.util.Optional;

/**
 * 门店-用户关联Repository
 */
@Repository
public interface StoreUserRepository extends JpaRepository<StoreUser, Long> {

    /**
     * 查询用户所属的所有门店关联
     */
    List<StoreUser> findByUserId(Long userId);

    /**
     * 查询门店的所有成员关联
     */
    List<StoreUser> findByStoreId(Long storeId);

    /**
     * 查询用户在指定门店的关联
     */
    Optional<StoreUser> findByStoreIdAndUserId(Long storeId, Long userId);

    /**
     * 检查用户是否是门店成员
     */
    boolean existsByStoreIdAndUserId(Long storeId, Long userId);

    boolean existsByStoreIdAndUserIdAndIsActiveTrue(Long storeId, Long userId);

    /**
     * 查询用户所属的所有激活状态的门店关联
     */
    @Query("SELECT su FROM StoreUser su JOIN FETCH su.store WHERE su.user.id = :userId AND su.isActive = true")
    List<StoreUser> findActiveStoresByUserId(@Param("userId") Long userId);

    /**
     * 查询门店的所有激活成员
     */
    @Query("SELECT su FROM StoreUser su JOIN FETCH su.user WHERE su.store.id = :storeId AND su.isActive = true")
    List<StoreUser> findActiveUsersByStoreId(@Param("storeId") Long storeId);

    /**
     * 查询用户拥有的门店(作为owner)
     */
    @Query("SELECT su FROM StoreUser su WHERE su.user.id = :userId AND su.role = 'owner'")
    List<StoreUser> findOwnedStores(@Param("userId") Long userId);
}
