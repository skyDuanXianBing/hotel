package server.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import server.demo.entity.StoreUserPermission;

import java.util.List;

@Repository
public interface StoreUserPermissionRepository extends JpaRepository<StoreUserPermission, Long> {

    List<StoreUserPermission> findByStoreUser_Id(Long storeUserId);

    List<StoreUserPermission> findByStoreUser_IdIn(List<Long> storeUserIds);

    void deleteByStoreUser_Id(Long storeUserId);
}

