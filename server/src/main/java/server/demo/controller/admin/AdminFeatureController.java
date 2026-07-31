package server.demo.controller.admin;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import server.demo.dto.ApiResponse;
import server.demo.dto.admin.AdminDtos.FeatureUpdateRequest;
import server.demo.entity.saas.SaasFeature;
import server.demo.service.admin.AdminPackageService;

import java.util.List;

/**
 * 平台管理端：功能字典维护。
 */
@RestController
@RequestMapping("/api/admin/features")
public class AdminFeatureController {

    private final AdminPackageService adminPackageService;

    public AdminFeatureController(AdminPackageService adminPackageService) {
        this.adminPackageService = adminPackageService;
    }

    @GetMapping
    public ApiResponse<List<SaasFeature>> listFeatures() {
        return ApiResponse.success(adminPackageService.listFeatures());
    }

    /** 名称/类型/单位/描述可改；feature_code 不可改。 */
    @PutMapping("/{id}")
    public ApiResponse<SaasFeature> updateFeature(
            @PathVariable Long id,
            @Valid @RequestBody FeatureUpdateRequest request
    ) {
        return ApiResponse.success("功能已更新", adminPackageService.updateFeature(id, request));
    }
}
