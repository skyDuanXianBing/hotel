package server.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import server.demo.entity.ManagedOperationMonthlyData;

import java.util.List;
import java.util.Optional;

public interface ManagedOperationMonthlyDataRepository extends JpaRepository<ManagedOperationMonthlyData, Long> {
    Optional<ManagedOperationMonthlyData> findByStoreIdAndSettingsIdAndSettlementMonth(
            Long storeId, Long settingsId, String settlementMonth);

    List<ManagedOperationMonthlyData> findByStoreIdAndSettingsId(Long storeId, Long settingsId);

    @Modifying
    @Query("delete from ManagedOperationMonthlyData d where d.storeId=:storeId and d.settings.id=:settingsId")
    int deleteByStoreIdAndSettingsId(@Param("storeId") Long storeId, @Param("settingsId") Long settingsId);
}
