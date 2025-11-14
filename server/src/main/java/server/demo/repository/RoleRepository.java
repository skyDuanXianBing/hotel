package server.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import server.demo.entity.Role;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(String name);

    boolean existsByName(String name);

    List<Role> findByIsSystem(Boolean isSystem);

    @Query("SELECT r FROM Role r WHERE r.name LIKE %:keyword%")
    List<Role> searchRoles(@Param("keyword") String keyword);

    @Query("SELECT r FROM Role r JOIN r.users u WHERE u.id = :userId")
    List<Role> findRolesByUserId(@Param("userId") Long userId);

    // 门店级查询方法
    List<Role> findByStoreId(Long storeId);

    @Query("SELECT r FROM Role r WHERE r.storeId = :storeId AND r.name LIKE %:keyword%")
    List<Role> searchRolesByStoreId(@Param("storeId") Long storeId, @Param("keyword") String keyword);

    boolean existsByStoreIdAndName(Long storeId, String name);

    Optional<Role> findByStoreIdAndName(Long storeId, String name);
}
