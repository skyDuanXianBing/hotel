package server.demo.controller.admin;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import server.demo.dto.ApiResponse;
import server.demo.dto.admin.AdminDtos.PackageStatusRequest;
import server.demo.dto.admin.AdminDtos.PackageUpsertRequest;
import server.demo.dto.admin.AdminDtos.ReplacePackageFeaturesRequest;
import server.demo.entity.saas.SaasPackage;
import server.demo.entity.saas.SaasPackageFeature;
import server.demo.service.admin.AdminPackageService;

import java.util.List;

/**
 * 平台管理端：套餐模板与套餐权益维护。
 */
@RestController
@RequestMapping("/api/admin/packages")
public class AdminPackageController {

    private final AdminPackageService adminPackageService;

    public AdminPackageController(AdminPackageService adminPackageService) {
        this.adminPackageService = adminPackageService;
    }

    @GetMapping
    public ApiResponse<List<SaasPackage>> listPackages() {
        return ApiResponse.success(adminPackageService.listPackages());
    }

    @PostMapping
    public ApiResponse<SaasPackage> createPackage(@Valid @RequestBody PackageUpsertRequest request) {
        return ApiResponse.success("套餐已创建（默认下架，配置权益后可上架）",
                adminPackageService.createPackage(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<SaasPackage> updatePackage(
            @PathVariable Long id,
            @Valid @RequestBody PackageUpsertRequest request
    ) {
        return ApiResponse.success("套餐已更新", adminPackageService.updatePackage(id, request));
    }

    /** 上下架：改价建议新建更高 version 的套餐行并下架旧行；存量订阅不受上下架影响。 */
    @PutMapping("/{id}/status")
    public ApiResponse<SaasPackage> updatePackageStatus(
            @PathVariable Long id,
            @Valid @RequestBody PackageStatusRequest request
    ) {
        return ApiResponse.success("套餐状态已更新",
                adminPackageService.updatePackageStatus(id, request.status()));
    }

    @GetMapping("/{id}/features")
    public ApiResponse<List<SaasPackageFeature>> listPackageFeatures(@PathVariable Long id) {
        return ApiResponse.success(adminPackageService.listPackageFeatures(id));
    }

    /** 整体替换套餐权益模板（feature + quotaLimit 列表）。 */
    @PutMapping("/{id}/features")
    public ApiResponse<List<SaasPackageFeature>> replacePackageFeatures(
            @PathVariable Long id,
            @Valid @RequestBody ReplacePackageFeaturesRequest request
    ) {
        return ApiResponse.success("套餐权益已更新",
                adminPackageService.replacePackageFeatures(id, request.features()));
    }
}
