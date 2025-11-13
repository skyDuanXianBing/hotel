package server.demo.service.impl;

import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.demo.dto.CleanerInvitationDTO;
import server.demo.dto.CleanerRegistrationDTO;
import server.demo.entity.Cleaner;
import server.demo.entity.CleanerInvitation;
import server.demo.repository.CleanerInvitationRepository;
import server.demo.repository.CleanerRepository;
import server.demo.service.CleanerInvitationService;
import server.demo.service.EmailService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 保洁员邀请Service实现类
 */
@Service
public class CleanerInvitationServiceImpl implements CleanerInvitationService {

    @Autowired
    private CleanerInvitationRepository invitationRepository;

    @Autowired
    private CleanerRepository cleanerRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${app.frontend.url:http://localhost:8091}")
    private String frontendUrl;

    @Override
    @Transactional
    public CleanerInvitation sendInvitation(CleanerInvitationDTO invitationDTO) {
        // 检查是否已经有待处理的邀请
        List<CleanerInvitation> existingInvitations = invitationRepository
                .findByEmailAndStatus(invitationDTO.getEmail(), "pending");

        // 将已存在的待处理邀请标记为过期
        for (CleanerInvitation existing : existingInvitations) {
            existing.setStatus("expired");
            invitationRepository.save(existing);
        }

        // 生成唯一token
        String token = UUID.randomUUID().toString();

        // 创建新邀请
        CleanerInvitation invitation = new CleanerInvitation();
        invitation.setEmail(invitationDTO.getEmail());
        invitation.setName(invitationDTO.getName());
        invitation.setToken(token);
        invitation.setUserId(invitationDTO.getUserId());
        invitation.setStoreId(invitationDTO.getStoreId());
        invitation.setStatus("pending");
        invitation.setExpiresAt(LocalDateTime.now().plusDays(7)); // 7天有效期

        invitation = invitationRepository.save(invitation);

        // 构建邀请链接
        String invitationUrl = frontendUrl + "/cleaner/register?token=" + token;

        // 发送邀请邮件
        try {
            emailService.sendCleanerInvitation(
                    invitationDTO.getEmail(),
                    invitationDTO.getName(),
                    "楽途ホテル　池袋", // TODO: 从配置或数据库获取门店名称
                    invitationUrl
            );
        } catch (MessagingException e) {
            throw new RuntimeException("邮件发送失败: " + e.getMessage());
        }

        return invitation;
    }

    @Override
    public CleanerInvitation validateToken(String token) {
        CleanerInvitation invitation = invitationRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("无效的邀请链接"));

        if (!"pending".equals(invitation.getStatus())) {
            throw new RuntimeException("邀请已失效");
        }

        if (invitation.getExpiresAt().isBefore(LocalDateTime.now())) {
            invitation.setStatus("expired");
            invitationRepository.save(invitation);
            throw new RuntimeException("邀请已过期");
        }

        return invitation;
    }

    @Override
    @Transactional
    public Cleaner registerCleaner(CleanerRegistrationDTO registrationDTO) {
        // 验证token
        CleanerInvitation invitation = validateToken(registrationDTO.getToken());

        // 检查邮箱是否一致
        if (!invitation.getEmail().equals(registrationDTO.getEmail())) {
            throw new RuntimeException("邮箱地址不匹配");
        }

        // 检查邮箱是否已被注册为保洁员
        if (cleanerRepository.findByEmail(registrationDTO.getEmail()).isPresent()) {
            throw new RuntimeException("该邮箱已被注册为保洁员");
        }

        // 创建保洁员记录 - 直接在Cleaner表中存储认证信息
        Cleaner cleaner = new Cleaner();
        cleaner.setStoreId(invitation.getStoreId());
        cleaner.setName(registrationDTO.getName());
        cleaner.setEmail(registrationDTO.getEmail());
        cleaner.setPassword(passwordEncoder.encode(registrationDTO.getPassword())); // 加密密码
        cleaner.setIsActive(true); // 设置为激活状态
        cleaner = cleanerRepository.save(cleaner);

        // 标记邀请为已接受
        invitation.setStatus("accepted");
        invitationRepository.save(invitation);

        return cleaner;
    }

    @Override
    @Transactional
    public void cleanupExpiredInvitations() {
        List<CleanerInvitation> expiredInvitations = invitationRepository
                .findByStatusAndExpiresAtBefore("pending", LocalDateTime.now());

        for (CleanerInvitation invitation : expiredInvitations) {
            invitation.setStatus("expired");
            invitationRepository.save(invitation);
        }
    }
}
