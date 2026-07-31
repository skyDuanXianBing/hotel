package server.demo.controller.admin;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import server.demo.dto.ApiResponse;
import server.demo.dto.admin.AdminDtos.DashboardResponse;
import server.demo.service.admin.AdminDashboardService;

/**
 * 平台管理端：经营概览聚合。
 */
@RestController
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    public AdminDashboardController(AdminDashboardService adminDashboardService) {
        this.adminDashboardService = adminDashboardService;
    }

    @GetMapping
    public ApiResponse<DashboardResponse> getDashboard() {
        return ApiResponse.success(adminDashboardService.getDashboard());
    }
}
