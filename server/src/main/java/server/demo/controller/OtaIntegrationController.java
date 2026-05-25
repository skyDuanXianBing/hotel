package server.demo.controller;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import server.demo.annotation.StoreScoped;
import server.demo.dto.OtaIntegrationDTO;
import server.demo.service.OtaIntegrationService;
import server.demo.dto.ApiResponse;

import java.util.List;

/**
 * OTA直连配置控制器
 */
@RestController
@RequestMapping("/api/v1/ota-integrations")
@StoreScoped
public class OtaIntegrationController {

    private static final Logger logger = LoggerFactory.getLogger(OtaIntegrationController.class);

    private final OtaIntegrationService otaIntegrationService;

    @Autowired
    public OtaIntegrationController(OtaIntegrationService otaIntegrationService) {
        this.otaIntegrationService = otaIntegrationService;
    }

    /**
     * 获取所有OTA直连配置
     */
    @GetMapping
    public ApiResponse<List<OtaIntegrationDTO>> getAllOtaIntegrations() {
        List<OtaIntegrationDTO> integrations = otaIntegrationService.getAllOtaIntegrations();
        return ApiResponse.success(integrations);
    }

    /**
     * 根据ID获取OTA直连配置
     */
    @GetMapping("/{id}")
    public ApiResponse<OtaIntegrationDTO> getOtaIntegrationById(@PathVariable Long id) {
        OtaIntegrationDTO integration = otaIntegrationService.getOtaIntegrationById(id);
        return ApiResponse.success(integration);
    }

    /**
     * 根据渠道代码获取OTA直连配置
     */
    @GetMapping("/code/{code}")
    public ApiResponse<OtaIntegrationDTO> getOtaIntegrationByCode(@PathVariable String code) {
        OtaIntegrationDTO integration = otaIntegrationService.getOtaIntegrationByCode(code);
        return ApiResponse.success(integration);
    }

    /**
     * 更新OTA直连配置
     */
    @PutMapping("/{id}")
    public ApiResponse<OtaIntegrationDTO> updateOtaIntegration(
            @PathVariable Long id,
            @RequestBody OtaIntegrationDTO dto) {
        OtaIntegrationDTO updated = otaIntegrationService.updateOtaIntegration(id, dto);
        return ApiResponse.success("更新成功", updated);
    }

    /**
     * 连接OTA渠道
     */
    @PostMapping("/{id}/connect")
    public ApiResponse<OtaIntegrationDTO> connectOta(
            @PathVariable Long id,
            @RequestBody ConnectOtaRequest request) {
        OtaIntegrationDTO connected = otaIntegrationService.connectOta(id, request.apiKey(), request.apiSecret());
        return ApiResponse.success("连接成功", connected);
    }

    /**
     * 断开OTA渠道连接
     */
    @PostMapping("/{id}/disconnect")
    public ApiResponse<OtaIntegrationDTO> disconnectOta(@PathVariable Long id) {
        OtaIntegrationDTO disconnected = otaIntegrationService.disconnectOta(id);
        return ApiResponse.success("断开连接成功", disconnected);
    }

    /**
     * 更新价格调整配置
     */
    @PutMapping("/{id}/price-adjustment")
    public ApiResponse<OtaIntegrationDTO> updatePriceAdjustment(
            @PathVariable Long id,
            @RequestBody OtaIntegrationDTO dto) {
        OtaIntegrationDTO updated = otaIntegrationService.updatePriceAdjustment(id, dto);
        return ApiResponse.success("价格调整更新成功", updated);
    }

    /**
     * 生成Su Widget Token
     * 用于前端加载Su Channel Mapping Widget
     * GET /api/v1/ota-integrations/{id}/su-widget-token
     */
    @GetMapping("/{id}/su-widget-token")
    public ApiResponse<WidgetTokenResponse> getSuWidgetToken(
            @PathVariable Long id,
            @RequestParam(name = "syncContent", defaultValue = "false") boolean syncContent,
            @RequestParam(name = "language", required = false) String language
    ) {
        try {
            WidgetTokenResponse response = otaIntegrationService.generateSuWidgetToken(id, syncContent, language);
            return ApiResponse.success("获取Widget Token成功", response);
        } catch (RuntimeException e) {
            return ApiResponse.error("获取Widget Token失败：" + e.getMessage());
        }
    }

    /**
     * 连接OTA请求体
     */
    /**
     * 手动触发：PMS -> Su 房型/价格计划同步（用于排查 Widget 下拉 No Record Found）
     */
    @PostMapping("/{id}/su-content/sync")
    public ApiResponse<Object> syncSuContent(@PathVariable Long id) {
        try {
            Object summary = otaIntegrationService.syncSuContent(id);
            return ApiResponse.success("渠道房型/价格计划同步成功", summary);
        } catch (RuntimeException e) {
            return ApiResponse.error("渠道房型/价格计划同步失败: " + e.getMessage());
        }
    }

    /**
     * 查询 Su 侧当前已保存的映射（用于验证 Widget 保存是否生效）
     * GET /api/v1/ota-integrations/{id}/su-mappings?channelId=9
     */
    /**
     * 一键推送 PMS 房型列表（Room Types，认证要求：roomid=roomTypeId）。
     * POST /api/v1/ota-integrations/{id}/su-content/sync-rooms
     */
    @PostMapping("/{id}/su-content/sync-rooms")
    public ApiResponse<Object> syncSuRooms(@PathVariable Long id) {
        try {
            logger.info("[OneClickSync] start syncSuRooms. otaIntegrationId={}", id);
            Object summary = otaIntegrationService.syncSuRooms(id);
            logger.info("[OneClickSync] done syncSuRooms. otaIntegrationId={}", id);
            return ApiResponse.success("渠道房型列表同步成功", summary);
        } catch (RuntimeException e) {
            logger.error("[OneClickSync] failed syncSuRooms. otaIntegrationId={}", id, e);
            return ApiResponse.error("渠道房型列表同步失败: " + e.getMessage());
        }
    }

    /**
     * 一键推送 PMS 价格计划列表（用于 Su Widget 费率计划映射）。
     * POST /api/v1/ota-integrations/{id}/su-content/sync-rate-plans
     */
    @PostMapping("/{id}/su-content/sync-rate-plans")
    public ApiResponse<Object> syncSuRatePlans(@PathVariable Long id) {
        try {
            logger.info("[OneClickSync] start syncSuRatePlans. otaIntegrationId={}", id);
            Object summary = otaIntegrationService.syncSuRatePlans(id);
            logger.info("[OneClickSync] done syncSuRatePlans. otaIntegrationId={}", id);
            return ApiResponse.success("渠道价格计划列表同步成功", summary);
        } catch (RuntimeException e) {
            logger.error("[OneClickSync] failed syncSuRatePlans. otaIntegrationId={}", id, e);
            return ApiResponse.error("渠道价格计划列表同步失败: " + e.getMessage());
        }
    }

    /**
     * 一键推送基础 ARI（Rates & Availability，/availability）。
     * 这个接口用于修复外联网页面显示 “No rates pushed / No availability pushed”。
     * POST /api/v1/ota-integrations/{id}/su-content/sync-ari?days=365
     */
    @PostMapping("/{id}/su-content/sync-ari")
    public ApiResponse<Object> syncSuAri(
            @PathVariable Long id,
            @RequestParam(required = false) Integer days
    ) {
        try {
            logger.info("[OneClickSync] start syncSuAri. otaIntegrationId={}, days={}", id, days);
            Object summary = otaIntegrationService.syncSuAri(id, days);
            logger.info("[OneClickSync] done syncSuAri. otaIntegrationId={}, days={}", id, days);
            return ApiResponse.success("渠道基础ARI同步成功", summary);
        } catch (RuntimeException e) {
            logger.error("[OneClickSync] failed syncSuAri. otaIntegrationId={}, days={}", id, days, e);
            return ApiResponse.error("渠道基础ARI同步失败: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/su-content/sync-rates")
    public ApiResponse<Object> syncSuRates(
            @PathVariable Long id,
            @RequestParam(required = false) Integer days
    ) {
        try {
            logger.info("[OneClickSync] start syncSuRates. otaIntegrationId={}, days={}", id, days);
            Object summary = otaIntegrationService.syncSuRates(id, days);
            logger.info("[OneClickSync] done syncSuRates. otaIntegrationId={}, days={}", id, days);
            return ApiResponse.success("渠道房价同步成功", summary);
        } catch (RuntimeException e) {
            logger.error("[OneClickSync] failed syncSuRates. otaIntegrationId={}, days={}", id, days, e);
            return ApiResponse.error("渠道房价同步失败: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/su-content/sync-availability")
    public ApiResponse<Object> syncSuAvailability(
            @PathVariable Long id,
            @RequestParam(required = false) Integer days
    ) {
        try {
            logger.info("[OneClickSync] start syncSuAvailability. otaIntegrationId={}, days={}", id, days);
            Object summary = otaIntegrationService.syncSuAvailability(id, days);
            logger.info("[OneClickSync] done syncSuAvailability. otaIntegrationId={}, days={}", id, days);
            return ApiResponse.success("渠道可用性同步成功", summary);
        } catch (RuntimeException e) {
            logger.error("[OneClickSync] failed syncSuAvailability. otaIntegrationId={}, days={}", id, days, e);
            return ApiResponse.error("渠道可用性同步失败: " + e.getMessage());
        }
    }

    @GetMapping("/{id}/su-mappings")
    public ApiResponse<JsonNode> getSuMappings(
            @PathVariable Long id,
            @RequestParam(required = false) String channelId
    ) {
        try {
            JsonNode mappings = otaIntegrationService.getSuMappings(id, channelId);
            return ApiResponse.success("获取渠道映射成功", mappings);
        } catch (RuntimeException e) {
            return ApiResponse.error("获取渠道映射失败: " + e.getMessage());
        }
    }

    /**
     * 查询 Su 映射是否完成（必须房型 + 价格计划都已映射）。
     * GET /api/v1/ota-integrations/{id}/su-mapping-status?channelId=19
     */
    @GetMapping("/{id}/su-mapping-status")
    public ApiResponse<OtaIntegrationService.SuMappingStatusSummary> getSuMappingStatus(
            @PathVariable Long id,
            @RequestParam(required = false) String channelId
    ) {
        try {
            OtaIntegrationService.SuMappingStatusSummary status = otaIntegrationService.getSuMappingStatus(id, channelId);
            return ApiResponse.success("获取映射状态成功", status);
        } catch (RuntimeException e) {
            return ApiResponse.error("获取映射状态失败: " + e.getMessage());
        }
    }

    public record ConnectOtaRequest(String apiKey, String apiSecret) {}

    /**
     * Widget Token响应
     */
    public record WidgetTokenResponse(
            String tokenId,
            String propertyId,
            String appId,
            String channelId,
            String channelCode,
            String scriptUrl,
            String type,
            String language
    ) {}
}
