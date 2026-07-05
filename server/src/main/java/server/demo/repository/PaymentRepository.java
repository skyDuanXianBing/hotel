package server.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import server.demo.entity.Payment;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /**
     * 根据预订ID查询收款记录列表
     */
    List<Payment> findByReservationIdOrderByDateDesc(Long reservationId);

    /**
     * 根据预订ID删除所有收款记录
     */
    void deleteByReservationId(Long reservationId);

    @Query("""
            SELECT p
            FROM Payment p
            WHERE p.date >= :startDate
              AND p.date <= :endDate
              AND p.reservationId IN (
                    SELECT r.id
                    FROM Reservation r
                    WHERE r.storeId = :storeId
                      AND r.status <> server.demo.enums.ReservationStatus.CANCELLED
              )
            ORDER BY p.date ASC, p.id ASC
            """)
    List<Payment> findActiveReservationPaymentsByStoreIdAndDateRange(
            @Param("storeId") Long storeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
