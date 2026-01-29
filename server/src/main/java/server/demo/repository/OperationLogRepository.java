package server.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import server.demo.entity.OperationLog;

import java.util.List;

public interface OperationLogRepository extends JpaRepository<OperationLog, Long> {
    List<OperationLog> findByStoreIdAndReservationIdOrderByTimestampDesc(Long storeId, Long reservationId);
}

