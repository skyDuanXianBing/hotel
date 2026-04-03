package server.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import server.demo.entity.OrderBox;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderBoxRepository extends JpaRepository<OrderBox, Long> {

    @Query("SELECT ob FROM OrderBox ob JOIN FETCH ob.reservation r " +
           "LEFT JOIN FETCH r.room rm LEFT JOIN FETCH rm.roomType " +
           "JOIN FETCH r.channel ORDER BY ob.movedInAt DESC")
    List<OrderBox> findAllWithDetails();

    Optional<OrderBox> findByReservationId(Long reservationId);

    boolean existsByReservationId(Long reservationId);

    @Query("SELECT COUNT(ob) FROM OrderBox ob")
    long countAll();

    void deleteByReservationId(Long reservationId);
}
