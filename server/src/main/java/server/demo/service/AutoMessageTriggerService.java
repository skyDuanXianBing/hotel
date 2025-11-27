package server.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import server.demo.entity.AutoMessage;
import server.demo.entity.Reservation;
import server.demo.entity.VirtualMailbox;
import server.demo.enums.EmailSenderType;
import server.demo.enums.ReservationStatus;
import server.demo.repository.AutoMessageRepository;
import server.demo.repository.ReservationRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 自动化消息触发服务
 * 负责在订单事件发生时自动发送邮件消息
 */
@Service
public class AutoMessageTriggerService {

    private static final Logger logger = LoggerFactory.getLogger(AutoMessageTriggerService.class);

    @Autowired
    private AutoMessageRepository autoMessageRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private VirtualMailboxService virtualMailboxService;

    @Autowired
    private EmailMessageService emailMessageService;

    /**
     * 订单创建时触发
     * 异步执行,不阻塞订单创建流程
     *
     * @param reservation 订单对象
     */
    @Async
    public void onReservationCreated(Reservation reservation) {
        try {
            logger.info("触发订单创建自动消息: 订单ID={}", reservation.getId());

            // 查找匹配的自动消息模板
            List<AutoMessage> messages = findMatchingMessages(
                    "订单确认时",
                    reservation.getChannel().getName(),
                    reservation.getRoom().getRoomNumber(),
                    reservation.getStoreId()
            );

            // 发送自动消息
            for (AutoMessage message : messages) {
                sendAutoMessage(reservation, message);
            }
        } catch (Exception e) {
            logger.error("发送订单创建自动消息失败: 订单ID={}, 错误={}",
                    reservation.getId(), e.getMessage(), e);
        }
    }

    /**
     * 入住前24小时检查 (定时任务)
     * 每小时执行一次
     */
    @Scheduled(cron = "0 0 * * * *")
    public void check24HoursBeforeCheckIn() {
        try {
            LocalDate tomorrow = LocalDate.now().plusDays(1);
            logger.debug("检查入住前24小时的订单: 日期={}", tomorrow);

            List<Reservation> reservations = reservationRepository
                    .findByCheckInDateAndStatus(tomorrow, ReservationStatus.CONFIRMED);

            for (Reservation reservation : reservations) {
                triggerCheckInReminderMessages(reservation);
            }

            logger.info("入住前24小时检查完成,处理了{}个订单", reservations.size());
        } catch (Exception e) {
            logger.error("入住前24小时检查失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 入住当天检查 (定时任务)
     * 每天早上8点执行
     */
    @Scheduled(cron = "0 0 8 * * *")
    public void checkCheckInDay() {
        try {
            LocalDate today = LocalDate.now();
            logger.debug("检查入住当天的订单: 日期={}", today);

            List<Reservation> reservations = reservationRepository
                    .findByCheckInDateAndStatus(today, ReservationStatus.CONFIRMED);

            for (Reservation reservation : reservations) {
                triggerCheckInDayMessages(reservation);
            }

            logger.info("入住当天检查完成,处理了{}个订单", reservations.size());
        } catch (Exception e) {
            logger.error("入住当天检查失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 退房当天检查 (定时任务)
     * 每天早上10点执行
     */
    @Scheduled(cron = "0 0 10 * * *")
    public void checkCheckOutDay() {
        try {
            LocalDate today = LocalDate.now();
            logger.debug("检查退房当天的订单: 日期={}", today);

            List<Reservation> reservations = reservationRepository
                    .findByCheckOutDateAndStatus(today, ReservationStatus.CONFIRMED);

            for (Reservation reservation : reservations) {
                triggerCheckOutDayMessages(reservation);
            }

            logger.info("退房当天检查完成,处理了{}个订单", reservations.size());
        } catch (Exception e) {
            logger.error("退房当天检查失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 退房后检查 (定时任务)
     * 每天中午12点执行
     */
    @Scheduled(cron = "0 0 12 * * *")
    public void checkAfterCheckOut() {
        try {
            LocalDate yesterday = LocalDate.now().minusDays(1);
            logger.debug("检查退房后的订单: 日期={}", yesterday);

            List<Reservation> reservations = reservationRepository
                    .findByCheckOutDateAndStatus(yesterday, ReservationStatus.CONFIRMED);

            for (Reservation reservation : reservations) {
                triggerAfterCheckOutMessages(reservation);
            }

            logger.info("退房后检查完成,处理了{}个订单", reservations.size());
        } catch (Exception e) {
            logger.error("退房后检查失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 触发入住前提醒消息
     */
    private void triggerCheckInReminderMessages(Reservation reservation) {
        List<AutoMessage> messages = findMatchingMessages(
                "入住前24小时",
                reservation.getChannel().getName(),
                reservation.getRoom().getRoomNumber(),
                reservation.getStoreId()
        );

        for (AutoMessage message : messages) {
            sendAutoMessage(reservation, message);
        }
    }

    /**
     * 触发入住当天消息
     */
    private void triggerCheckInDayMessages(Reservation reservation) {
        List<AutoMessage> messages = findMatchingMessages(
                "入住当天",
                reservation.getChannel().getName(),
                reservation.getRoom().getRoomNumber(),
                reservation.getStoreId()
        );

        for (AutoMessage message : messages) {
            sendAutoMessage(reservation, message);
        }
    }

    /**
     * 触发退房当天消息
     */
    private void triggerCheckOutDayMessages(Reservation reservation) {
        List<AutoMessage> messages = findMatchingMessages(
                "退房当天",
                reservation.getChannel().getName(),
                reservation.getRoom().getRoomNumber(),
                reservation.getStoreId()
        );

        for (AutoMessage message : messages) {
            sendAutoMessage(reservation, message);
        }
    }

    /**
     * 触发退房后消息
     */
    private void triggerAfterCheckOutMessages(Reservation reservation) {
        List<AutoMessage> messages = findMatchingMessages(
                "退房后",
                reservation.getChannel().getName(),
                reservation.getRoom().getRoomNumber(),
                reservation.getStoreId()
        );

        for (AutoMessage message : messages) {
            sendAutoMessage(reservation, message);
        }
    }

    /**
     * 查找匹配的自动消息模板
     */
    private List<AutoMessage> findMatchingMessages(String rule, String channelName,
                                                   String roomNumber, Long storeId) {
        return autoMessageRepository.findByStoreIdAndEnabledTrue(storeId).stream()
                .filter(msg -> msg.getAutomationRule().equals(rule))
                .filter(msg -> msg.getChannel().equals("全部渠道") || msg.getChannel().equals(channelName))
                .filter(msg -> msg.getRoom().equals("全部房间") || msg.getRoom().equals(roomNumber))
                .toList();
    }

    /**
     * 发送自动消息
     */
    private void sendAutoMessage(Reservation reservation, AutoMessage autoMessage) {
        try {
            // 获取或创建虚拟邮箱
            VirtualMailbox mailbox = virtualMailboxService.getMailboxByReservation(reservation.getId())
                    .orElseGet(() -> virtualMailboxService.createMailboxForReservation(reservation.getId()));

            // 替换消息模板中的变量
            String content = replaceTemplateVariables(autoMessage.getMessage(), reservation);

            // 发送邮件消息
            emailMessageService.sendEmailMessage(
                    mailbox.getId(),
                    content,
                    EmailSenderType.SYSTEM,
                    "系统自动消息"
            );

            logger.info("自动消息已发送: 订单ID={}, 规则={}, 模板={}",
                    reservation.getId(), autoMessage.getAutomationRule(), autoMessage.getTitle());

        } catch (Exception e) {
            logger.error("发送自动消息失败: 订单ID={}, 模板ID={}, 错误={}",
                    reservation.getId(), autoMessage.getId(), e.getMessage(), e);
        }
    }

    /**
     * 替换消息模板中的变量
     * 支持的变量: {客人姓名}, {房间号}, {入住日期}, {退房日期}
     */
    private String replaceTemplateVariables(String template, Reservation reservation) {
        String result = template;

        // 替换客人姓名
        result = result.replace("{客人姓名}", reservation.getGuestName());

        // 替换房间号
        if (reservation.getRoom() != null) {
            result = result.replace("{房间号}", reservation.getRoom().getRoomNumber());
        }

        // 替换入住日期
        result = result.replace("{入住日期}", reservation.getCheckInDate().toString());

        // 替换退房日期
        result = result.replace("{退房日期}", reservation.getCheckOutDate().toString());

        return result;
    }
}
