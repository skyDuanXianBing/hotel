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

import server.demo.i18n.ApiMessages;
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
                    ApiMessages.get("api.t.210d726078f7"),
                    priceLabsService.getAccounts()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error(ApiMessages.get("api.t.d75fa208dfd3") + e.getMessage()));
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
            return ResponseEntity.ok(ApiResponse.success(ApiMessages.get("api.t.3f2336efce50"), account));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error(ApiMessages.get("api.t.4158158ba094") + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @StoreScoped
    public ResponseEntity<ApiResponse<PriceLabsAccountDTO>> updateAccount(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        try {
            return priceLabsService.updateAccount(id, request.get("accountName"), request.get("priceLabsEmail"))
                    .map(account -> ResponseEntity.ok(ApiResponse.success(ApiMessages.get("api.t.05e93ab84a1e"), account)))
                    .orElse(ResponseEntity.status(404).body(ApiResponse.error(ApiMessages.get("api.t.a0a401089fec"))));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error(ApiMessages.get("api.t.6192048a84f0") + e.getMessage()));
        }
    }

    @PatchMapping("/{id}/status")
    @StoreScoped
    public ResponseEntity<ApiResponse<PriceLabsAccountDTO>> updateAccountStatus(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> request) {
        try {
            return priceLabsService.updateAccountStatus(id, request.get("enabled"))
                    .map(account -> ResponseEntity.ok(ApiResponse.success(ApiMessages.get("api.t.7a0a4a07840f"), account)))
                    .orElse(ResponseEntity.status(404).body(ApiResponse.error(ApiMessages.get("api.t.a0a401089fec"))));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error(ApiMessages.get("api.t.6dc41f767848") + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @StoreScoped
    public ResponseEntity<ApiResponse<Void>> deleteAccount(@PathVariable Long id) {
        try {
            if (priceLabsService.deleteAccount(id)) {
                return ResponseEntity.ok(ApiResponse.success(ApiMessages.get("api.t.d95798ae35f2"), null));
            }
            return ResponseEntity.status(404).body(ApiResponse.error(ApiMessages.get("api.t.a0a401089fec")));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error(ApiMessages.get("api.t.356ccccf36c3") + e.getMessage()));
        }
    }
}
