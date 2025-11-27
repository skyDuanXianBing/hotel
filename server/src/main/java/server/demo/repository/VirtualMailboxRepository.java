package server.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import server.demo.entity.VirtualMailbox;
import server.demo.enums.MailboxStatus;

import java.util.List;
import java.util.Optional;

/**
 * 虚拟邮箱Repository
 */
@Repository
public interface VirtualMailboxRepository extends JpaRepository<VirtualMailbox, Long> {

    /**
     * 根据订单ID查找虚拟邮箱
     */
    Optional<VirtualMailbox> findByReservationId(Long reservationId);

    /**
     * 根据邮箱地址查找虚拟邮箱
     */
    Optional<VirtualMailbox> findByEmailAddress(String emailAddress);

    /**
     * 根据门店ID和状态查找虚拟邮箱列表
     */
    List<VirtualMailbox> findByStoreIdAndStatus(Long storeId, MailboxStatus status);

    /**
     * 根据门店ID查找所有虚拟邮箱
     */
    List<VirtualMailbox> findByStoreId(Long storeId);

    /**
     * 检查邮箱地址是否已存在
     */
    boolean existsByEmailAddress(String emailAddress);
}
