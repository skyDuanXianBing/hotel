package server.demo.service;

import server.demo.dto.CleanerInvitationDTO;
import server.demo.dto.CleanerRegistrationDTO;
import server.demo.entity.Cleaner;
import server.demo.entity.CleanerInvitation;

/**
 * 保洁员邀请Service接口
 */
public interface CleanerInvitationService {

    /**
     * 发送邀请邮件
     */
    CleanerInvitation sendInvitation(CleanerInvitationDTO invitationDTO);

    /**
     * 验证邀请token
     */
    CleanerInvitation validateToken(String token);

    /**
     * 注册保洁员
     */
    Cleaner registerCleaner(CleanerRegistrationDTO registrationDTO);

    /**
     * 清理过期邀请
     */
    void cleanupExpiredInvitations();
}
