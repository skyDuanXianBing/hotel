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
import server.demo.entity.User;
import server.demo.repository.CleanerInvitationRepository;
import server.demo.repository.CleanerRepository;
import server.demo.repository.StoreRepository;
import server.demo.service.CleanerIdentityService;
import server.demo.service.CleanerInvitationService;
import server.demo.service.EmailService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

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

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private CleanerIdentityService cleanerIdentityService;

    @Value("${app.frontend.url:http://localhost:8091}")
    private String frontendUrl;

    private static final String DEFAULT_STORE_NAME = "门店";
    private static final String DEFAULT_FRONTEND_URL = "http://localhost:8091";

    @Override
    @Transactional
    public CleanerInvitation sendInvitation(CleanerInvitationDTO invitationDTO) {
        List<CleanerInvitation> existingInvitations = invitationRepository
                .findByEmailAndStatus(invitationDTO.getEmail(), "pending");

        for (CleanerInvitation existing : existingInvitations) {
            existing.setStatus("expired");
            invitationRepository.save(existing);
        }

        String token = UUID.randomUUID().toString();

        CleanerInvitation invitation = new CleanerInvitation();
        invitation.setEmail(invitationDTO.getEmail());
        invitation.setName(invitationDTO.getName());
        invitation.setToken(token);
        invitation.setUserId(invitationDTO.getUserId());
        invitation.setStoreId(invitationDTO.getStoreId());
        invitation.setStatus("pending");
        invitation.setExpiresAt(LocalDateTime.now().plusDays(7));

        invitation = invitationRepository.save(invitation);

        String invitationUrl = buildInvitationUrl(token);
        String storeName = resolveStoreName(invitationDTO.getStoreId());

        try {
            emailService.sendCleanerInvitation(
                    invitationDTO.getEmail(),
                    invitationDTO.getName(),
                    storeName,
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
        CleanerInvitation invitation = validateToken(registrationDTO.getToken());

        String registrationEmail = registrationDTO.getEmail() == null
                ? null
                : registrationDTO.getEmail().trim();
        if (!invitation.getEmail().equalsIgnoreCase(registrationEmail)) {
            throw new RuntimeException("邮箱地址不匹配");
        }

        String cleanerName = registrationDTO.getName() == null
                ? ""
                : registrationDTO.getName().trim();
        String encodedPassword = passwordEncoder.encode(registrationDTO.getPassword());

        List<Cleaner> storeCleaners = cleanerRepository.findByStoreIdAndEmailIgnoreCase(
                invitation.getStoreId(),
                registrationEmail
        );
        if (storeCleaners.size() > 1) {
            throw new RuntimeException("当前门店下存在重复的保洁员档案，请联系管理员检查数据");
        }

        Cleaner existingCleaner = storeCleaners.isEmpty() ? null : storeCleaners.get(0);
        if (existingCleaner != null && Boolean.TRUE.equals(existingCleaner.getIsActive())) {
            throw new RuntimeException("该账号已是当前门店保洁员");
        }

        User user = cleanerIdentityService.createOrReuseCleanerUserAccount(
                registrationEmail,
                cleanerName,
                encodedPassword,
                invitation.getStoreId(),
                invitation.getUserId()
        );

        Cleaner cleaner;
        if (existingCleaner != null) {
            existingCleaner.setUserId(user.getId());
            existingCleaner.setStoreId(invitation.getStoreId());
            existingCleaner.setName(cleanerName);
            existingCleaner.setEmail(registrationEmail);
            existingCleaner.setPassword(user.getPassword());
            existingCleaner.setIsActive(true);
            cleaner = cleanerRepository.save(existingCleaner);
        } else {
            cleaner = new Cleaner();
            cleaner.setUserId(user.getId());
            cleaner.setStoreId(invitation.getStoreId());
            cleaner.setName(cleanerName);
            cleaner.setEmail(registrationEmail);
            cleaner.setPassword(user.getPassword());
            cleaner.setIsActive(true);
            cleaner = cleanerRepository.save(cleaner);
        }

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

    private String resolveStoreName(Long storeId) {
        if (storeId == null) {
            return DEFAULT_STORE_NAME;
        }
        return storeRepository.findById(storeId)
                .map(store -> {
                    String name = store.getName();
                    if (name == null || name.isBlank()) {
                        return DEFAULT_STORE_NAME;
                    }
                    return name;
                })
                .orElse(DEFAULT_STORE_NAME);
    }

    private String buildInvitationUrl(String token) {
        String base = frontendUrl == null ? "" : frontendUrl.trim();
        if (base.isEmpty()) {
            base = DEFAULT_FRONTEND_URL;
        }
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + "/cleaner/register?token=" + token;
    }
}
