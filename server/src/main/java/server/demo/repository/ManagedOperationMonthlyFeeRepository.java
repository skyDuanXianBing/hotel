package server.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import server.demo.entity.ManagedOperationMonthlyFee;

import java.util.List;

public interface ManagedOperationMonthlyFeeRepository extends JpaRepository<ManagedOperationMonthlyFee, Long> {
    List<ManagedOperationMonthlyFee> findByStoreIdAndMonthlyDataIdOrderBySortOrderAscIdAsc(
            Long storeId, Long monthlyDataId);

    @Modifying
    @Query("delete from ManagedOperationMonthlyFee f where f.storeId=:storeId and f.monthlyData.id=:monthlyDataId")
    int deleteByStoreIdAndMonthlyDataId(@Param("storeId") Long storeId, @Param("monthlyDataId") Long monthlyDataId);
}
