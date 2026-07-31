package server.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import server.demo.dto.ApiResponse;
import server.demo.dto.auth.CleanerLoginRequest;
import server.demo.dto.auth.CleanerLoginResponse;
import server.demo.service.CleanerAuthService;

import server.demo.i18n.ApiMessages;
/**
 * 保洁员认证控制器
 */
@RestController
@RequestMapping("/api/v1/auth/cleaner")
public class CleanerAuthController {

    @Autowired
    private CleanerAuthService cleanerAuthService;

    /**
     * 保洁员登录 - 使用邮箱和密码
     */
    @PostMapping("/login/password")
    public ApiResponse<CleanerLoginResponse> loginByPassword(@RequestBody CleanerLoginRequest request) {
        try {
            CleanerLoginResponse response = cleanerAuthService.loginByPassword(request);
            return ApiResponse.success(ApiMessages.get("api.t.2991317aba58"), response);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }
}
