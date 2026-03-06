package server.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.demo.context.StoreContextHolder;
import server.demo.entity.EnterpriseEmailConfig;
import server.demo.entity.Reservation;
import server.demo.entity.VirtualMailbox;
import server.demo.enums.MailboxStatus;
import server.demo.repository.EnterpriseEmailConfigRepository;
import server.demo.repository.ReservationRepository;
import server.demo.repository.VirtualMailboxRepository;

import java.util.List;
import java.util.Optional;

/**
 * 虚拟邮箱服务
 * 负责创建和管理虚拟邮箱
 */
@Service
public class VirtualMailboxService {

    @Autowired
    private VirtualMailboxRepository virtualMailboxRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private EnterpriseEmailConfigRepository emailConfigRepository;

    /**
     * 为订单创建虚拟邮箱
     * 如果已存在则返回现有邮箱
     *
     * @param reservationId 订单ID
     * @return 虚拟邮箱
     */
    @Transactional
    public VirtualMailbox createMailboxForReservation(Long reservationId) {
        // 检查是否已存在
        Optional<VirtualMailbox> existing = virtualMailboxRepository.findByReservationId(reservationId);
        if (existing.isPresent()) {
            return existing.get();
        }

        // 获取订单信息
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("订单不存在: " + reservationId));

        // 生成虚拟邮箱地址
        String emailAddress = generateVirtualEmail(reservation);
        String displayName = generateDisplayName(reservation);

        // 创建虚拟邮箱
        VirtualMailbox mailbox = new VirtualMailbox(reservationId, emailAddress, displayName);
        mailbox.setStoreId(reservation.getStoreId());

        return virtualMailboxRepository.save(mailbox);
    }

    /**
     * 生成虚拟邮箱地址
     * 格式: order-{订单号}@{门店域名}
     *
     * @param reservation 订单
     * @return 虚拟邮箱地址
     */
    private String generateVirtualEmail(Reservation reservation) {
        // 获取门店的邮箱配置
        Optional<EnterpriseEmailConfig> config = emailConfigRepository
                .findByStoreIdAndEnabledTrue(reservation.getStoreId());

        String domain;
        String prefix;

        if (config.isPresent()) {
            domain = config.get().getEmailDomain();
            prefix = config.get().getEmailPrefix()
                    .replace("{orderNumber}", reservation.getOrderNumber());
        } else {
            // 默认配置
            domain = "the-host.jp";
            prefix = "order-" + reservation.getOrderNumber();
        }

        return prefix + "@" + domain;
    }

    /**
     * 生成显示名称
     *
     * @param reservation 订单
     * @return 显示名称
     */
    private String generateDisplayName(Reservation reservation) {
        return "订单 " + reservation.getOrderNumber();
    }

    /**
     * 根据订单ID获取虚拟邮箱
     *
     * @param reservationId 订单ID
     * @return 虚拟邮箱
     */
    public Optional<VirtualMailbox> getMailboxByReservation(Long reservationId) {
        return virtualMailboxRepository.findByReservationId(reservationId);
    }

    /**
     * 根据邮箱地址获取虚拟邮箱
     *
     * @param emailAddress 邮箱地址
     * @return 虚拟邮箱
     */
    public Optional<VirtualMailbox> getMailboxByEmailAddress(String emailAddress) {
        return virtualMailboxRepository.findByEmailAddress(emailAddress);
    }

    /**
     * 获取当前门店的所有活跃邮箱
     *
     * @return 虚拟邮箱列表
     */
    public List<VirtualMailbox> getActiveMailboxes() {
        Long storeId = StoreContextHolder.getContext().getStoreId();
        return virtualMailboxRepository.findByStoreIdAndStatus(storeId, MailboxStatus.ACTIVE);
    }

    /**
     * 关闭虚拟邮箱
     *
     * @param mailboxId 邮箱ID
     */
    @Transactional
    public void closeMailbox(Long mailboxId) {
        VirtualMailbox mailbox = virtualMailboxRepository.findById(mailboxId)
                .orElseThrow(() -> new RuntimeException("虚拟邮箱不存在: " + mailboxId));

        mailbox.setStatus(MailboxStatus.CLOSED);
        virtualMailboxRepository.save(mailbox);
    }

    /**
     * 重新激活虚拟邮箱
     *
     * @param mailboxId 邮箱ID
     */
    @Transactional
    public void reopenMailbox(Long mailboxId) {
        VirtualMailbox mailbox = virtualMailboxRepository.findById(mailboxId)
                .orElseThrow(() -> new RuntimeException("虚拟邮箱不存在: " + mailboxId));

        mailbox.setStatus(MailboxStatus.ACTIVE);
        virtualMailboxRepository.save(mailbox);
    }

    /**
     * 根据ID获取虚拟邮箱
     *
     * @param mailboxId 邮箱ID
     * @return 虚拟邮箱
     */
    public Optional<VirtualMailbox> getMailboxById(Long mailboxId) {
        return virtualMailboxRepository.findById(mailboxId);
    }
}
