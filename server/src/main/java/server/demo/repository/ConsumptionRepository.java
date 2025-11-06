package server.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import server.demo.entity.Consumption;

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
}