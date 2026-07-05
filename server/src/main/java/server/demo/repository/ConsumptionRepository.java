package server.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import server.demo.entity.Consumption;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ConsumptionRepository extends JpaRepository<Consumption, Long> {

    /**
     * 根据预订ID查询消费记录列表
     */
    List<Consumption> findByReservationIdOrderByDateDesc(Long reservationId);

    /**
     * 根据预订ID删除所有消费记录
     */
    void deleteByReservationId(Long reservationId);

    @Query("""
            SELECT c
            FROM Consumption c
            WHERE c.date >= :startDate
              AND c.date <= :endDate
              AND c.reservationId IN (
                    SELECT r.id
                    FROM Reservation r
                    WHERE r.storeId = :storeId
                      AND r.status <> server.demo.enums.ReservationStatus.CANCELLED
              )
            ORDER BY c.date ASC, c.id ASC
            """)
    List<Consumption> findActiveReservationConsumptionsByStoreIdAndDateRange(
            @Param("storeId") Long storeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
