package server.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import server.demo.entity.RoomType;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomTypeRepository extends JpaRepository<RoomType, Long> {

    List<RoomType> findByStoreId(Long storeId);

    List<RoomType> findByStoreIdOrderByName(Long storeId);

    Optional<RoomType> findByStoreIdAndCode(Long storeId, String code);

    List<RoomType> findAllByStoreIdAndCodeOrderByIdAsc(Long storeId, String code);

    List<RoomType> findAllByStoreIdAndNameOrderByIdAsc(Long storeId, String name);

    List<RoomType> findByStoreIdAndNameContainingIgnoreCase(Long storeId, String name);

    boolean existsByStoreIdAndCode(Long storeId, String code);

    boolean existsByStoreIdAndName(Long storeId, String name);

    boolean existsByStoreIdAndCodeAndIdNot(Long storeId, String code, Long id);

    boolean existsByStoreIdAndNameAndIdNot(Long storeId, String name, Long id);

    Optional<RoomType> findByStoreIdAndId(Long storeId, Long id);

    List<RoomType> findByStoreIdAndIdIn(Long storeId, List<Long> ids);

    // ===== 兼容旧逻辑的方法，后续将被移除 =====
    @Deprecated
    List<RoomType> findByUserId(Long userId);

    @Deprecated
    Optional<RoomType> findByUserIdAndCode(Long userId, String code);

    @Deprecated
    List<RoomType> findByUserIdAndNameContainingIgnoreCase(Long userId, String name);

    @Deprecated
    List<RoomType> findByUserIdOrderByName(Long userId);

    @Deprecated
    boolean existsByUserIdAndCode(Long userId, String code);

    @Deprecated
    Optional<RoomType> findByCode(String code);

    @Deprecated
    List<RoomType> findByNameContainingIgnoreCase(String name);

    @Deprecated
    @org.springframework.data.jpa.repository.Query("SELECT rt FROM RoomType rt ORDER BY rt.name")
    List<RoomType> findAllOrderByName();
}
