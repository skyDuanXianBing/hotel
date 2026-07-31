package server.demo.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import server.demo.dto.ApiResponse;
import server.demo.dto.auth.*;
import server.demo.service.AuthService;

import server.demo.i18n.ApiMessages;
/**
 * 认证控制器
 * 提供用户认证相关的RESTful API接口
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    /**
     * 发送验证码
     *
     * @param request 发送验证码请求
     * @return API响应
     */
    @PostMapping("/send-code")
    public ApiResponse<Void> sendVerificationCode(@Valid @RequestBody SendVerificationCodeRequest request) {
        try {
            authService.sendVerificationCode(request);
            return ApiResponse.success(ApiMessages.get("api.t.e1aaa946cba7"), null);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 注册
     *
     * @param request 注册请求
     * @return API响应
     */
    @PostMapping("/register")
    public ApiResponse<UserDTO> register(@Valid @RequestBody RegisterRequest request) {
        try {
            UserDTO userDTO = authService.register(request);
            return ApiResponse.success(ApiMessages.get("api.t.4a1935bccfd3"), userDTO);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 密码登录
     *
     * @param request 登录请求
     * @return API响应
     */
    @PostMapping("/login/password")
    public ApiResponse<LoginResponse> loginByPassword(@Valid @RequestBody LoginByPasswordRequest request) {
        try {
            LoginResponse loginResponse = authService.loginByPassword(request);
            return ApiResponse.success(ApiMessages.get("api.t.2991317aba58"), loginResponse);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 验证码登录
     *
     * @param request 验证码登录请求
     * @return API响应
     */
    @PostMapping("/login/code")
    public ApiResponse<LoginResponse> loginByCode(@Valid @RequestBody LoginByCodeRequest request) {
        try {
            LoginResponse loginResponse = authService.loginByCode(request);
            return ApiResponse.success(ApiMessages.get("api.t.2991317aba58"), loginResponse);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 登出
     *
     * @param httpRequest HTTP请求
     * @return API响应
     */
    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest httpRequest) {
        try {
            String token = (String) httpRequest.getAttribute("token");
            authService.logout(token);
            return ApiResponse.success(ApiMessages.get("api.t.64765c6511c6"), null);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 更新个人资料
     *
     * @param request 更新请求
     * @param httpRequest HTTP请求
     * @return API响应
     */
    @PutMapping("/profile")
    public ApiResponse<UserDTO> updateProfile(@Valid @RequestBody UpdateProfileRequest request, HttpServletRequest httpRequest) {
        try {
            Long userId = (Long) httpRequest.getAttribute("userId");
            UserDTO userDTO = authService.updateProfile(userId, request);
            return ApiResponse.success(ApiMessages.get("api.t.e53b1212a761"), userDTO);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 修改密码
     *
     * @param request 修改密码请求
     * @param httpRequest HTTP请求
     * @return API响应
     */
    @PutMapping("/password")
    public ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request, HttpServletRequest httpRequest) {
        try {
            Long userId = (Long) httpRequest.getAttribute("userId");
            String token = (String) httpRequest.getAttribute("token");
            authService.changePassword(userId, token, request);
            return ApiResponse.success(ApiMessages.get("api.t.f0e65f05ada2"), null);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 获取当前用户信息
     *
     * @param httpRequest HTTP请求
     * @return API响应
     */
    @GetMapping("/me")
    public ApiResponse<UserDTO> getCurrentUser(HttpServletRequest httpRequest) {
        try {
            Long userId = (Long) httpRequest.getAttribute("userId");
            UserDTO userDTO = authService.getCurrentUser(userId);
            return ApiResponse.success(userDTO);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 重置密码（忘记密码）
     *
     * @param request 重置密码请求
     * @return API响应
     */
    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        try {
            authService.resetPassword(request);
            return ApiResponse.success(ApiMessages.get("api.t.f4908703524d"), null);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }
}
