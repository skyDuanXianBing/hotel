package server.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import server.demo.entity.ReservationDailyPrice;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

@Repository
public interface ReservationDailyPriceRepository extends JpaRepository<ReservationDailyPrice, Long> {

    List<ReservationDailyPrice> findByStoreIdAndReservationIdInAndPriceDateBetween(
            Long storeId,
            Collection<Long> reservationIds,
            LocalDate startDate,
            LocalDate endDate
    );

    List<ReservationDailyPrice> findByStoreIdAndReservationIdOrderByPriceDateAsc(Long storeId, Long reservationId);

    long deleteByStoreIdAndReservationId(Long storeId, Long reservationId);
}
