package server.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.demo.context.StoreContext;
import server.demo.context.StoreContextHolder;
import server.demo.entity.AutoMessage;
import server.demo.entity.AutoMessageSendLog;
import server.demo.entity.Reservation;
import server.demo.repository.AutoMessageRepository;
import server.demo.repository.AutoMessageSendLogRepository;
import server.demo.repository.ReservationRepository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 自动化消息 Service
 */
@Service
public class AutoMessageService {

    private static final String SENDLOG_ACTION_PREFIX = "AM:";
    private static final String TARGET_TYPE_RESERVATION = "RESERVATION";
    private static final String WAITING_MANUAL_REPLAY = "WAITING_MANUAL_REPLAY";

    @Autowired
    private AutoMessageRepository autoMessageRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private AutoMessageSendLogRepository autoMessageSendLogRepository;

    @Autowired
    private SuBusinessAutoMessageService suBusinessAutoMessageService;

    /**
     * 获取当前门店ID
     */
    private Long getCurrentStoreId() {
        StoreContext context = StoreContextHolder.getContext();
        if (context == null || context.getStoreId() == null) {
            throw new RuntimeException("无法获取当前门店信息");
        }
        return context.getStoreId();
    }

    /**
     * 获取所有自动化消息(门店级)
     */
    public List<AutoMessage> getAllAutoMessages() {
        Long storeId = getCurrentStoreId();
        return autoMessageRepository.findByStoreId(storeId);
    }

    /**
     * 根据用户ID获取自动化消息列表(已废弃,使用getAllAutoMessages)
     */
    @Deprecated
    public List<AutoMessage> getAutoMessagesByUserId(Long userId) {
        return autoMessageRepository.findByUserId(userId);
    }

    /**
     * 获取启用的自动化消息(门店级)
     */
    public List<AutoMessage> getEnabledAutoMessages() {
        Long storeId = getCurrentStoreId();
        return autoMessageRepository.findByStoreIdAndEnabled(storeId, true);
    }

    /**
     * 根据ID获取自动化消息
     */
    public Optional<AutoMessage> getAutoMessageById(Long id) {
        return autoMessageRepository.findById(id);
    }

    /**
     * 创建自动化消息(门店级，storeId由StoreScopedEntityListener自动注入)
     */
    @Transactional
    public AutoMessage createAutoMessage(AutoMessage autoMessage) {
        // storeId由StoreScopedEntityListener自动注入，无需手动设置
        return autoMessageRepository.save(autoMessage);
    }

    /**
     * 更新自动化消息(门店级)
     */
    @Transactional
    public AutoMessage updateAutoMessage(Long id, AutoMessage autoMessage) {
        Long storeId = getCurrentStoreId();

        Optional<AutoMessage> existingMessage = autoMessageRepository.findById(id);
        if (existingMessage.isEmpty()) {
            throw new RuntimeException("自动化消息不存在");
        }

        AutoMessage message = existingMessage.get();

        // 验证消息属于当前门店
        if (!storeId.equals(message.getStoreId())) {
            throw new RuntimeException("无权限修改此自动化消息");
        }

        message.setTitle(autoMessage.getTitle());
        message.setMessage(autoMessage.getMessage());
        message.setAutomationRule(autoMessage.getAutomationRule());
        message.setChannel(autoMessage.getChannel());
        message.setChannels(autoMessage.getChannels());
        message.setResendOnExpire(autoMessage.getResendOnExpire());
        message.setRoom(autoMessage.getRoom());
        message.setRoomSelectionType(autoMessage.getRoomSelectionType());
        message.setRoomSelection(autoMessage.getRoomSelection());
        message.setAction(autoMessage.getAction());
        message.setSendTiming(autoMessage.getSendTiming());
        message.setEnabled(autoMessage.getEnabled());

        return autoMessageRepository.save(message);
    }

    /**
     * 删除自动化消息(门店级)
     */
    @Transactional
    public void deleteAutoMessage(Long id) {
        Long storeId = getCurrentStoreId();

        Optional<AutoMessage> existingMessage = autoMessageRepository.findById(id);
        if (existingMessage.isEmpty()) {
            throw new RuntimeException("自动化消息不存在");
        }

        AutoMessage message = existingMessage.get();

        // 验证消息属于当前门店
        if (!storeId.equals(message.getStoreId())) {
            throw new RuntimeException("无权限删除此自动化消息");
        }

        autoMessageRepository.deleteById(id);
    }

    /**
     * 切换自动化消息启用状态(门店级)
     */
    @Transactional
    public AutoMessage toggleAutoMessage(Long id) {
        Long storeId = getCurrentStoreId();

        Optional<AutoMessage> existingMessage = autoMessageRepository.findById(id);
        if (existingMessage.isEmpty()) {
            throw new RuntimeException("自动化消息不存在");
        }

        AutoMessage message = existingMessage.get();

        // 验证消息属于当前门店
        if (!storeId.equals(message.getStoreId())) {
            throw new RuntimeException("无权限修改此自动化消息");
        }

        message.setEnabled(!message.getEnabled());
        return autoMessageRepository.save(message);
    }

    @Transactional
    public void replayAutoMessage(Long reservationId, Long autoMessageId) {
        if (reservationId == null) {
            throw new RuntimeException("缺少 reservationId");
        }
        if (autoMessageId == null) {
            throw new RuntimeException("缺少 autoMessageId");
        }

        Long storeId = getCurrentStoreId();
        Reservation reservation = reservationRepository.findByStoreIdAndIdWithRoomType(storeId, reservationId)
                .orElseThrow(() -> new RuntimeException("订单不存在或无权限"));
        AutoMessage template = autoMessageRepository.findById(autoMessageId)
                .orElseThrow(() -> new RuntimeException("自动化消息模板不存在"));

        if (!storeId.equals(template.getStoreId())) {
            throw new RuntimeException("无权限重放该自动化消息模板");
        }

        AutoMessageSendLog sendLog = autoMessageSendLogRepository
                .findByStoreIdAndAutoMessageIdAndTargetTypeAndTargetId(storeId, autoMessageId, TARGET_TYPE_RESERVATION, reservationId)
                .orElseGet(AutoMessageSendLog::new);

        sendLog.setStoreId(storeId);
        sendLog.setAction(SENDLOG_ACTION_PREFIX + autoMessageId);
        sendLog.setTargetType(TARGET_TYPE_RESERVATION);
        sendLog.setTargetId(reservationId);
        sendLog.setAutoMessageId(autoMessageId);
        sendLog.setSuccess(false);
        sendLog.setErrorMessage(WAITING_MANUAL_REPLAY + ": manual replay requested");
        autoMessageSendLogRepository.save(sendLog);

        suBusinessAutoMessageService.trySendForReservation(
                storeId,
                reservation,
                template,
                LocalDateTime.now(),
                Duration.ZERO
        );
    }
}
