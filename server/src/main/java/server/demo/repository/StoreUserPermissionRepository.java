package server.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import server.demo.entity.StoreUserPermission;
import server.demo.enums.PermissionAction;
import server.demo.enums.PermissionModule;

import java.util.List;

@Repository
public interface StoreUserPermissionRepository extends JpaRepository<StoreUserPermission, Long> {

    List<StoreUserPermission> findByStoreUser_Id(Long storeUserId);

    List<StoreUserPermission> findByStoreUser_IdIn(List<Long> storeUserIds);

    boolean existsByStoreUser_IdAndModuleAndAction(
            Long storeUserId,
            PermissionModule module,
            PermissionAction action
    );

    void deleteByStoreUser_Id(Long storeUserId);
}
