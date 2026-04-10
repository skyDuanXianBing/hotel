package server.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import server.demo.entity.RoomBlockout;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

@Repository
public interface RoomBlockoutRepository extends JpaRepository<RoomBlockout, Long> {

    List<RoomBlockout> findByStoreIdAndRoom_IdInAndBlockDateBetween(
            Long storeId,
            Collection<Long> roomIds,
            LocalDate startDate,
            LocalDate endDate
    );

    List<RoomBlockout> findByStoreIdAndBlockDateBetween(
            Long storeId,
            LocalDate startDate,
            LocalDate endDate
    );

    long deleteByStoreIdAndRoom_IdInAndBlockDateBetween(
            Long storeId,
            Collection<Long> roomIds,
            LocalDate startDate,
            LocalDate endDate
    );

    long deleteByStoreIdAndRoom_IdIn(
            Long storeId,
            Collection<Long> roomIds
    );
}
