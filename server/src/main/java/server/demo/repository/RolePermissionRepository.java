package server.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import server.demo.entity.RolePermission;
import server.demo.enums.PermissionModule;
import server.demo.enums.PermissionAction;

import java.util.List;

@Repository
public interface RolePermissionRepository extends JpaRepository<RolePermission, Long> {

    List<RolePermission> findByRoleId(Long roleId);

    List<RolePermission> findByRoleIdAndModule(Long roleId, PermissionModule module);

    @Query("SELECT rp FROM RolePermission rp WHERE rp.role.id IN :roleIds")
    List<RolePermission> findByRoleIdIn(@Param("roleIds") List<Long> roleIds);

    @Modifying
    @Query("DELETE FROM RolePermission rp WHERE rp.role.id = :roleId")
    void deleteByRoleId(@Param("roleId") Long roleId);

    @Modifying
    @Query("DELETE FROM RolePermission rp WHERE rp.role.id = :roleId AND rp.module = :module")
    void deleteByRoleIdAndModule(@Param("roleId") Long roleId, @Param("module") PermissionModule module);

    boolean existsByRoleIdAndModuleAndAction(Long roleId, PermissionModule module, PermissionAction action);

    boolean existsByRoleIdInAndModuleAndAction(
            List<Long> roleIds,
            PermissionModule module,
            PermissionAction action
    );
}
