package server.demo.controller.advice;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import server.demo.dto.ApiResponse;
import server.demo.exception.NeedUpgradeException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * SaaS 权益不足统一返回 HTTP 402，错误体携带 featureCode/limit/used，供前端做升级引导。
 */
@Order(Ordered.HIGHEST_PRECEDENCE) // 必须优先于各业务域 catch-all advice（如 IndependentSiteApiExceptionHandler），否则 402 会被截胡成 500
@RestControllerAdvice
public class SaasEntitlementExceptionHandler {

    @ExceptionHandler(NeedUpgradeException.class)
    public ResponseEntity<ApiResponse<Object>> handleNeedUpgrade(NeedUpgradeException e) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("featureCode", e.getFeatureCode());
        body.put("limit", e.getLimit());
        body.put("used", e.getUsed());
        // reason 供前端区分"未开通套餐（先购买）"与"套餐不含该权益/额度用尽（升级）"的引导文案
        body.put("reason", e.getReason().name());
        return ResponseEntity.status(402).body(ApiResponse.error(e.getMessage(), body));
    }
}
