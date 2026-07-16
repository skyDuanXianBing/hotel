package server.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import server.demo.entity.ManagedOperationRoom;

import java.util.List;

public interface ManagedOperationRoomRepository extends JpaRepository<ManagedOperationRoom, Long> {
    @Query("select mor from ManagedOperationRoom mor join fetch mor.room r join fetch r.roomType " +
            "where mor.storeId=:storeId and mor.settings.id=:settingsId order by r.roomNumber")
    List<ManagedOperationRoom> findByStoreIdAndSettingsIdWithRoom(
            @Param("storeId") Long storeId, @Param("settingsId") Long settingsId);

    @Modifying
    @Query("delete from ManagedOperationRoom mor where mor.storeId=:storeId and mor.settings.id=:settingsId")
    int deleteByStoreIdAndSettingsId(@Param("storeId") Long storeId, @Param("settingsId") Long settingsId);
}
