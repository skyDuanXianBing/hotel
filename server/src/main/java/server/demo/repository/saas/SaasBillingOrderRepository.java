package server.demo.repository.saas;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import server.demo.entity.saas.SaasBillingOrder;
import server.demo.enums.SaasBillingOrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

public interface SaasBillingOrderRepository extends JpaRepository<SaasBillingOrder, Long> {

    /**
     * 幂等重放查找：按 (store_id, idempotency_key) 命中已有订单即视为同一笔购买（V064）。
     */
    Optional<SaasBillingOrder> findByStoreIdAndIdempotencyKey(Long storeId, String idempotencyKey);

    /**
     * 管理端概览：指定时间之后的已支付订单金额合计。
     */
    @Query("SELECT COALESCE(SUM(o.amount), 0) FROM SaasBillingOrder o "
            + "WHERE o.status = :status AND o.createdAt >= :since")
    BigDecimal sumAmountByStatusSince(@Param("status") SaasBillingOrderStatus status,
                                      @Param("since") LocalDateTime since);
}
