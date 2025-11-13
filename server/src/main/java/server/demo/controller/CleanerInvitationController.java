package server.demo.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import server.demo.dto.ApiResponse;
import server.demo.dto.CleanerInvitationDTO;
import server.demo.dto.CleanerRegistrationDTO;
import server.demo.entity.Cleaner;
import server.demo.entity.CleanerInvitation;
import server.demo.service.CleanerInvitationService;

/**
 * 保洁员邀请Controller
 */
@RestController
@RequestMapping("/api/v1/cleaner-invitations")
public class CleanerInvitationController {

    @Autowired
    private CleanerInvitationService invitationService;

    /**
     * 发送邀请邮件
     */
    @PostMapping("/send")
    public ApiResponse<String> sendInvitation(@Valid @RequestBody CleanerInvitationDTO invitationDTO) {
        try {
            invitationService.sendInvitation(invitationDTO);
            return ApiResponse.success("邀请邮件已发送");
        } catch (Exception e) {
            return ApiResponse.error("发送邀请失败: " + e.getMessage());
        }
    }

    /**
     * 验证邀请token
     */
    @GetMapping("/validate/{token}")
    public ApiResponse<CleanerInvitation> validateToken(@PathVariable String token) {
        try {
            CleanerInvitation invitation = invitationService.validateToken(token);
            return ApiResponse.success("验证成功", invitation);
        } catch (Exception e) {
            return ApiResponse.error("验证失败: " + e.getMessage());
        }
    }

    /**
     * 保洁员注册
     */
    @PostMapping("/register")
    public ApiResponse<Cleaner> registerCleaner(@Valid @RequestBody CleanerRegistrationDTO registrationDTO) {
        try {
            Cleaner cleaner = invitationService.registerCleaner(registrationDTO);
            return ApiResponse.success("注册成功", cleaner);
        } catch (Exception e) {
            return ApiResponse.error("注册失败: " + e.getMessage());
        }
    }
}
