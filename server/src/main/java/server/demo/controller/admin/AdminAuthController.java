package server.demo.controller.admin;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import server.demo.dto.ApiResponse;
import server.demo.dto.admin.AdminDtos.ChangePasswordRequest;
import server.demo.dto.admin.AdminDtos.LoginRequest;
import server.demo.dto.admin.AdminDtos.LoginResponse;
import server.demo.interceptor.AdminAuthInterceptor;
import server.demo.service.admin.AdminAuthService;

/**
 * 平台管理员认证（独立登录端点，WebMvcConfig 中对 /api/admin/** 唯一放行）。
 */
@RestController
@RequestMapping("/api/admin/auth")
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    public AdminAuthController(AdminAuthService adminAuthService) {
        this.adminAuthService = adminAuthService;
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success("登录成功", adminAuthService.login(request));
    }

    /**
     * 修改密码。不在 WebMvcConfig 放行清单中，由 AdminAuthInterceptor 保证已登录。
     */
    @PostMapping("/change-password")
    public ApiResponse<Void> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            HttpServletRequest httpRequest
    ) {
        String username = (String) httpRequest.getAttribute(AdminAuthInterceptor.ATTR_ADMIN_USERNAME);
        adminAuthService.changePassword(username, request);
        return ApiResponse.success("密码已修改", null);
    }
}
