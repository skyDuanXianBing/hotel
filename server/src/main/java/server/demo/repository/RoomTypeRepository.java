package server.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import server.demo.entity.RoomType;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomTypeRepository extends JpaRepository<RoomType, Long> {

    // 按用户ID查询所有房型
    List<RoomType> findByUserId(Long userId);

    // 按用户ID和代码查询
    Optional<RoomType> findByUserIdAndCode(Long userId, String code);

    // 按用户ID和名称模糊查询
    List<RoomType> findByUserIdAndNameContainingIgnoreCase(Long userId, String name);

    // 按用户ID查询所有房型并排序
    @Query("SELECT rt FROM RoomType rt WHERE rt.user.id = :userId ORDER BY rt.name")
    List<RoomType> findByUserIdOrderByName(@Param("userId") Long userId);

    // 检查用户的房型代码是否存在
    boolean existsByUserIdAndCode(Long userId, String code);

    // 以下为旧方法，保留用于数据迁移兼容
    Optional<RoomType> findByCode(String code);
    List<RoomType> findByNameContainingIgnoreCase(String name);
    @Query("SELECT rt FROM RoomType rt ORDER BY rt.name")
    List<RoomType> findAllOrderByName();
    boolean existsByCode(String code);
}