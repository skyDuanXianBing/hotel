package server.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import server.demo.entity.Payment;

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
}