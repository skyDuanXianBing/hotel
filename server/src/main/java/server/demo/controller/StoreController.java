package server.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import server.demo.dto.ApiResponse;
import server.demo.entity.Store;
import server.demo.entity.StorePolicy;
import server.demo.service.StoreService;

import java.util.List;

/**
 * 门店管理控制器
 */
@RestController
@RequestMapping("/api/v1/stores")
public class StoreController {

    @Autowired
    private StoreService storeService;

    /**
     * 获取所有门店
     */
    @GetMapping
    public ApiResponse<List<Store>> getAllStores() {
        List<Store> stores = storeService.getAllStores();
        return ApiResponse.success("获取门店列表成功", stores);
    }

    /**
     * 根据ID获取门店详情
     */
    @GetMapping("/{id}")
    public ApiResponse<Store> getStoreById(@PathVariable Long id) {
        return storeService.getStoreById(id)
                .map(store -> ApiResponse.success("获取门店详情成功", store))
                .orElse(ApiResponse.error("门店不存在"));
    }

    /**
     * 创建门店
     */
    @PostMapping
    public ApiResponse<Store> createStore(@RequestBody Store store) {
        Store createdStore = storeService.createStore(store);
        return ApiResponse.success("创建门店成功", createdStore);
    }

    /**
     * 更新门店
     */
    @PutMapping("/{id}")
    public ApiResponse<Store> updateStore(@PathVariable Long id, @RequestBody Store store) {
        try {
            Store updatedStore = storeService.updateStore(id, store);
            return ApiResponse.success("更新门店成功", updatedStore);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 删除门店
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteStore(@PathVariable Long id) {
        storeService.deleteStore(id);
        return ApiResponse.success("删除门店成功", null);
    }

    /**
     * 获取门店政策
     */
    @GetMapping("/{id}/policy")
    public ApiResponse<StorePolicy> getStorePolicy(@PathVariable Long id) {
        return storeService.getStorePolicy(id)
                .map(policy -> ApiResponse.success("获取门店政策成功", policy))
                .orElse(ApiResponse.success("暂无政策信息", null));
    }

    /**
     * 更新或创建门店政策
     */
    @PutMapping("/{id}/policy")
    public ApiResponse<StorePolicy> saveStorePolicy(@PathVariable Long id, @RequestBody StorePolicy policy) {
        try {
            StorePolicy savedPolicy = storeService.saveStorePolicy(id, policy);
            return ApiResponse.success("保存门店政策成功", savedPolicy);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }
}
