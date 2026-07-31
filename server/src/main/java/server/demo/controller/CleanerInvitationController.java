package server.demo.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import server.demo.annotation.StoreScoped;
import server.demo.dto.ApiResponse;
import server.demo.dto.CleanerInvitationDTO;
import server.demo.dto.CleanerRegistrationDTO;
import server.demo.entity.Cleaner;
import server.demo.entity.CleanerInvitation;
import server.demo.service.CleanerInvitationService;

import server.demo.i18n.ApiMessages;
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
    @StoreScoped
    public ApiResponse<String> sendInvitation(
            @Valid @RequestBody CleanerInvitationDTO invitationDTO,
            HttpServletRequest request) {
        try {
            // 从请求中获取userId和storeId
            Long userId = (Long) request.getAttribute("userId");
            server.demo.context.StoreContext storeContext = server.demo.context.StoreContextHolder.getContext();

            if (storeContext == null || storeContext.getStoreId() == null) {
                return ApiResponse.error(ApiMessages.get("api.t.642b7e97c7d4"));
            }

            // 设置userId和storeId
            invitationDTO.setUserId(userId);
            invitationDTO.setStoreId(storeContext.getStoreId());

            invitationService.sendInvitation(invitationDTO);
            return ApiResponse.success(ApiMessages.get("api.t.0ebd4acf6773"));
        } catch (Exception e) {
            return ApiResponse.error(ApiMessages.get("api.t.be3a3dc89a95") + e.getMessage());
        }
    }

    /**
     * 验证邀请token
     */
    @GetMapping("/validate/{token}")
    public ApiResponse<CleanerInvitation> validateToken(@PathVariable String token) {
        try {
            CleanerInvitation invitation = invitationService.validateToken(token);
            return ApiResponse.success(ApiMessages.get("api.t.b421f63de13d"), invitation);
        } catch (Exception e) {
            return ApiResponse.error(ApiMessages.get("api.t.c7f2a93923f3") + e.getMessage());
        }
    }

    /**
     * 保洁员注册
     */
    @PostMapping("/register")
    public ApiResponse<Cleaner> registerCleaner(@Valid @RequestBody CleanerRegistrationDTO registrationDTO) {
        try {
            Cleaner cleaner = invitationService.registerCleaner(registrationDTO);
            return ApiResponse.success(ApiMessages.get("api.t.4a1935bccfd3"), cleaner);
        } catch (Exception e) {
            return ApiResponse.error(ApiMessages.get("api.t.7a4004595000") + e.getMessage());
        }
    }
}
