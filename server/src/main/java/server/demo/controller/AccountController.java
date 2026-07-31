package server.demo.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import server.demo.dto.*;
import server.demo.service.AccountService;

import java.util.List;

import server.demo.i18n.ApiMessages;
/**
 * 账号管理控制器
 */
@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    @Autowired
    private AccountService accountService;

    /**
     * 获取所有账号
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<AccountDTO>>> getAllAccounts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long roleId,
            @RequestParam(required = false) Boolean isActive
    ) {
        try {
            List<AccountDTO> accounts;
            if (keyword != null || roleId != null || isActive != null) {
                accounts = accountService.getAccountsWithFilters(keyword, roleId, isActive);
            } else {
                accounts = accountService.getAllAccounts();
            }
            return ResponseEntity.ok(new ApiResponse<>(true, ApiMessages.get("api.t.1ebe0f46da66"), accounts));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }

    /**
     * 根据ID获取账号详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AccountDTO>> getAccountById(@PathVariable Long id) {
        try {
            AccountDTO account = accountService.getAccountById(id);
            return ResponseEntity.ok(new ApiResponse<>(true, ApiMessages.get("api.t.940b529fdc6c"), account));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }

    /**
     * 创建账号
     */
    @PostMapping
    public ResponseEntity<ApiResponse<AccountDTO>> createAccount(@Valid @RequestBody CreateAccountRequest request) {
        try {
            AccountDTO account = accountService.createAccount(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiResponse<>(true, ApiMessages.get("api.t.bafea8097500"), account));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }

    /**
     * 更新账号
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AccountDTO>> updateAccount(
            @PathVariable Long id,
            @Valid @RequestBody UpdateAccountRequest request
    ) {
        try {
            AccountDTO account = accountService.updateAccount(id, request);
            return ResponseEntity.ok(new ApiResponse<>(true, ApiMessages.get("api.t.b65f9f524a7a"), account));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }

    /**
     * 删除账号
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteAccount(@PathVariable Long id) {
        try {
            accountService.deleteAccount(id);
            return ResponseEntity.ok(new ApiResponse<>(true, ApiMessages.get("api.t.453fc4990037"), null));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }

    /**
     * 批量删除账号
     */
    @DeleteMapping("/batch")
    public ResponseEntity<ApiResponse<Void>> batchDeleteAccounts(@RequestBody List<Long> accountIds) {
        try {
            accountService.batchDeleteAccounts(accountIds);
            return ResponseEntity.ok(new ApiResponse<>(true, ApiMessages.get("api.t.b384ebbfac59"), null));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }

    /**
     * 更新账号状态
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<AccountDTO>> updateAccountStatus(
            @PathVariable Long id,
            @RequestParam Boolean isActive
    ) {
        try {
            AccountDTO account = accountService.updateAccountStatus(id, isActive);
            return ResponseEntity.ok(new ApiResponse<>(true, ApiMessages.get("api.t.5c392638ed12"), account));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }

    /**
     * 批量更新账号状态
     */
    @PutMapping("/batch/status")
    public ResponseEntity<ApiResponse<Void>> batchUpdateStatus(@Valid @RequestBody BatchUpdateStatusRequest request) {
        try {
            accountService.batchUpdateStatus(request);
            return ResponseEntity.ok(new ApiResponse<>(true, ApiMessages.get("api.t.d5badb40eabe"), null));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }

    /**
     * 批量调整角色
     */
    @PutMapping("/batch/roles")
    public ResponseEntity<ApiResponse<Void>> batchUpdateRoles(@Valid @RequestBody BatchUpdateRolesRequest request) {
        try {
            accountService.batchUpdateRoles(request);
            return ResponseEntity.ok(new ApiResponse<>(true, ApiMessages.get("api.t.d82f1d8ad041"), null));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }
}
