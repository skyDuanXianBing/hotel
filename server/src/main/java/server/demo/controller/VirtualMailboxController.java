package server.demo.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import server.demo.annotation.StoreScoped;
import server.demo.dto.ApiResponse;
import server.demo.dto.EmailMessageDTO;
import server.demo.dto.SendMessageRequest;
import server.demo.dto.VirtualMailboxDTO;
import server.demo.entity.EmailMessage;
import server.demo.entity.Reservation;
import server.demo.entity.VirtualMailbox;
import server.demo.enums.EmailSenderType;
import server.demo.repository.ReservationRepository;
import server.demo.service.EmailMessageService;
import server.demo.service.VirtualMailboxService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 虚拟邮箱控制器
 * 提供虚拟邮箱和邮件消息的API接口
 */
@RestController
@RequestMapping("/api/v1/virtual-mailbox")
@StoreScoped
public class VirtualMailboxController {

    @Autowired
    private VirtualMailboxService virtualMailboxService;

    @Autowired
    private EmailMessageService emailMessageService;

    @Autowired
    private ReservationRepository reservationRepository;

    /**
     * 获取订单的虚拟邮箱
     *
     * @param reservationId 订单ID
     * @return 虚拟邮箱信息
     */
    @GetMapping("/reservation/{reservationId}")
    public ApiResponse<VirtualMailboxDTO> getMailboxByReservation(@PathVariable Long reservationId) {
        VirtualMailbox mailbox = virtualMailboxService.getMailboxByReservation(reservationId)
                .orElseGet(() -> virtualMailboxService.createMailboxForReservation(reservationId));

        VirtualMailboxDTO dto = convertToDTO(mailbox);
        return ApiResponse.success(dto);
    }

    /**
     * 获取所有活跃的虚拟邮箱(用于收件箱列表)
     *
     * @return 虚拟邮箱列表
     */
    @GetMapping("/active")
    public ApiResponse<List<VirtualMailboxDTO>> getActiveMailboxes() {
        List<VirtualMailbox> mailboxes = virtualMailboxService.getActiveMailboxes();
        List<VirtualMailboxDTO> dtos = new ArrayList<>();

        for (VirtualMailbox mailbox : mailboxes) {
            dtos.add(convertToDTO(mailbox));
        }

        return ApiResponse.success(dtos);
    }

    /**
     * 获取虚拟邮箱的所有消息
     *
     * @param mailboxId 虚拟邮箱ID
     * @return 消息列表
     */
    @GetMapping("/mailbox/{mailboxId}/messages")
    public ApiResponse<List<EmailMessageDTO>> getMessages(@PathVariable Long mailboxId) {
        List<EmailMessage> messages = emailMessageService.getMessagesByMailbox(mailboxId);
        List<EmailMessageDTO> dtos = new ArrayList<>();

        for (EmailMessage message : messages) {
            dtos.add(convertMessageToDTO(message));
        }

        return ApiResponse.success(dtos);
    }

    /**
     * 轮询新消息
     *
     * @param mailboxId 虚拟邮箱ID
     * @param since 起始时间(ISO格式)
     * @return 新消息列表
     */
    @GetMapping("/mailbox/{mailboxId}/poll")
    public ApiResponse<List<EmailMessageDTO>> pollMessages(
            @PathVariable Long mailboxId,
            @RequestParam String since) {

        LocalDateTime sinceTime = LocalDateTime.parse(since, DateTimeFormatter.ISO_DATE_TIME);
        List<EmailMessage> messages = emailMessageService.pollNewMessages(mailboxId, sinceTime);
        List<EmailMessageDTO> dtos = new ArrayList<>();

        for (EmailMessage message : messages) {
            dtos.add(convertMessageToDTO(message));
        }

        return ApiResponse.success(dtos);
    }

    /**
     * 发送消息
     *
     * @param mailboxId 虚拟邮箱ID
     * @param request 发送消息请求
     * @return 发送的消息
     */
    @PostMapping("/mailbox/{mailboxId}/send")
    public ApiResponse<EmailMessageDTO> sendMessage(
            @PathVariable Long mailboxId,
            @Valid @RequestBody SendMessageRequest request) {

        String senderName = request.getSenderName() != null ? request.getSenderName() : "客服";

        EmailMessage message = emailMessageService.sendEmailMessage(
                mailboxId,
                request.getContent(),
                EmailSenderType.STAFF,
                senderName
        );

        return ApiResponse.success(convertMessageToDTO(message));
    }

    /**
     * 关闭虚拟邮箱
     *
     * @param mailboxId 虚拟邮箱ID
     * @return 成功响应
     */
    @PostMapping("/mailbox/{mailboxId}/close")
    public ApiResponse<Void> closeMailbox(@PathVariable Long mailboxId) {
        virtualMailboxService.closeMailbox(mailboxId);
        return ApiResponse.success("虚拟邮箱已关闭", null);
    }

    /**
     * 转换VirtualMailbox为DTO
     */
    private VirtualMailboxDTO convertToDTO(VirtualMailbox mailbox) {
        VirtualMailboxDTO dto = new VirtualMailboxDTO();
        dto.setId(mailbox.getId());
        dto.setReservationId(mailbox.getReservationId());
        dto.setEmailAddress(mailbox.getEmailAddress());
        dto.setDisplayName(mailbox.getDisplayName());
        dto.setStatus(mailbox.getStatus());

        // 获取订单信息
        reservationRepository.findById(mailbox.getReservationId()).ifPresent(reservation -> {
            dto.setGuestName(reservation.getGuestName());
            if (reservation.getRoom() != null) {
                dto.setGuestRoomNumber(reservation.getRoom().getRoomNumber());
            }
        });

        // 获取最后活动时间(最后一条消息的时间)
        List<EmailMessage> messages = emailMessageService.getMessagesByMailbox(mailbox.getId());
        if (!messages.isEmpty()) {
            dto.setLastActivity(messages.get(messages.size() - 1).getSentAt());
        } else {
            dto.setLastActivity(mailbox.getCreatedAt());
        }

        // 统计未读消息
        dto.setUnreadCount(emailMessageService.countUnreadMessages(mailbox.getId()));

        return dto;
    }

    /**
     * 转换EmailMessage为DTO
     */
    private EmailMessageDTO convertMessageToDTO(EmailMessage message) {
        EmailMessageDTO dto = new EmailMessageDTO();
        dto.setId(message.getId());
        dto.setMailboxId(message.getMailboxId());
        dto.setSenderEmail(message.getSenderEmail());
        dto.setSenderName(message.getSenderName());
        dto.setSenderType(message.getSenderType());
        dto.setContent(message.getContent());
        dto.setTimestamp(message.getSentAt());
        dto.setIsRead(message.getIsRead());
        return dto;
    }
}
