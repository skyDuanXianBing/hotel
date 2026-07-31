package server.demo.controller.admin;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import server.demo.dto.ApiResponse;
import server.demo.dto.admin.AdminDtos.StoreSearchItem;
import server.demo.service.admin.AdminStoreService;

import java.util.List;

/**
 * 平台管理端：门店查询（管理端各表单的可搜索门店选择器数据源）。
 */
@RestController
@RequestMapping("/api/admin/stores")
public class AdminStoreController {

    private final AdminStoreService adminStoreService;

    public AdminStoreController(AdminStoreService adminStoreService) {
        this.adminStoreService = adminStoreService;
    }

    /** 门店选择器搜索：名称模糊 + ID 精确匹配，最多 20 条（id + name）。 */
    @GetMapping("/search")
    public ApiResponse<List<StoreSearchItem>> searchStores(@RequestParam(required = false) String keyword) {
        return ApiResponse.success(adminStoreService.searchStores(keyword));
    }
}
