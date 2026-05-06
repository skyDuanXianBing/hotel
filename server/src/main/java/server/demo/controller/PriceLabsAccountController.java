package server.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import server.demo.annotation.StoreScoped;
import server.demo.dto.ApiResponse;
import server.demo.dto.PriceLabsAccountDTO;
import server.demo.service.PriceLabsService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/pricelabs/accounts")
public class PriceLabsAccountController {

    @Autowired
    private PriceLabsService priceLabsService;

    @GetMapping
    @StoreScoped
    public ResponseEntity<ApiResponse<List<PriceLabsAccountDTO>>> getAccounts() {
        try {
            return ResponseEntity.ok(ApiResponse.success(
                    "获取 PriceLabs 账号列表成功",
                    priceLabsService.getAccounts()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("获取 PriceLabs 账号列表失败: " + e.getMessage()));
        }
    }

    @PostMapping
    @StoreScoped
    public ResponseEntity<ApiResponse<PriceLabsAccountDTO>> createAccount(@RequestBody Map<String, String> request) {
        try {
            PriceLabsAccountDTO account = priceLabsService.createAccount(
                    request.get("accountName"),
                    request.get("priceLabsEmail")
            );
            return ResponseEntity.ok(ApiResponse.success("创建 PriceLabs 账号成功", account));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("创建 PriceLabs 账号失败: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @StoreScoped
    public ResponseEntity<ApiResponse<PriceLabsAccountDTO>> updateAccount(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        try {
            return priceLabsService.updateAccount(id, request.get("accountName"), request.get("priceLabsEmail"))
                    .map(account -> ResponseEntity.ok(ApiResponse.success("更新 PriceLabs 账号成功", account)))
                    .orElse(ResponseEntity.status(404).body(ApiResponse.error("PriceLabs 账号不存在")));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("更新 PriceLabs 账号失败: " + e.getMessage()));
        }
    }

    @PatchMapping("/{id}/status")
    @StoreScoped
    public ResponseEntity<ApiResponse<PriceLabsAccountDTO>> updateAccountStatus(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> request) {
        try {
            return priceLabsService.updateAccountStatus(id, request.get("enabled"))
                    .map(account -> ResponseEntity.ok(ApiResponse.success("更新 PriceLabs 账号状态成功", account)))
                    .orElse(ResponseEntity.status(404).body(ApiResponse.error("PriceLabs 账号不存在")));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("更新 PriceLabs 账号状态失败: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @StoreScoped
    public ResponseEntity<ApiResponse<Void>> deleteAccount(@PathVariable Long id) {
        try {
            if (priceLabsService.deleteAccount(id)) {
                return ResponseEntity.ok(ApiResponse.success("删除 PriceLabs 账号成功", null));
            }
            return ResponseEntity.status(404).body(ApiResponse.error("PriceLabs 账号不存在"));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("删除 PriceLabs 账号失败: " + e.getMessage()));
        }
    }
}
