package server.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.demo.config.SuApiConfig;
import server.demo.context.StoreContextHolder;
import server.demo.controller.OtaIntegrationController;
import server.demo.dto.OtaIntegrationDTO;
import server.demo.entity.OtaIntegration;
import server.demo.entity.Store;
import server.demo.repository.OtaIntegrationRepository;
import server.demo.repository.StoreRepository;
import server.demo.util.SuHotelIdUtil;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * OTA直连配置服务层
 */
@Service
public class OtaIntegrationService {

    private final OtaIntegrationRepository otaIntegrationRepository;
    private final SuApiClient suApiClient;
    private final SuContentSyncService suContentSyncService;
    private final SuAvailabilitySyncService suAvailabilitySyncService;
    private final SuRateSyncService suRateSyncService;
    private final SuAriSyncService suAriSyncService;
    private final SuApiConfig suApiConfig;
    private final StoreRepository storeRepository;
    private final SuAccessTokenService suAccessTokenService;

    /**
     * 默认OTA渠道配置
     */
    private static final List<OtaConfig> DEFAULT_OTA_CONFIGS = List.of(
            new OtaConfig("Airbnb", "AIRBNB", "https://upload.wikimedia.org/wikipedia/commons/6/69/Airbnb_Logo_B%C3%A9lo.svg"),
            new OtaConfig("Booking.com", "BOOKING", "https://upload.wikimedia.org/wikipedia/commons/b/be/Booking.com_logo.svg")
    );

    @Autowired
    public OtaIntegrationService(
            OtaIntegrationRepository otaIntegrationRepository,
            SuApiClient suApiClient,
            SuContentSyncService suContentSyncService,
            SuAvailabilitySyncService suAvailabilitySyncService,
            SuRateSyncService suRateSyncService,
            SuAriSyncService suAriSyncService,
            SuApiConfig suApiConfig,
            StoreRepository storeRepository,
            SuAccessTokenService suAccessTokenService
    ) {
        this.otaIntegrationRepository = otaIntegrationRepository;
        this.suApiClient = suApiClient;
        this.suContentSyncService = suContentSyncService;
        this.suAvailabilitySyncService = suAvailabilitySyncService;
        this.suRateSyncService = suRateSyncService;
        this.suAriSyncService = suAriSyncService;
        this.suApiConfig = suApiConfig;
        this.storeRepository = storeRepository;
        this.suAccessTokenService = suAccessTokenService;
    }

    /**
     * 获取当前门店的所有OTA直连配置
     */
    public List<OtaIntegrationDTO> getAllOtaIntegrations() {
        Long storeId = StoreContextHolder.getContext().getStoreId();
        List<OtaIntegration> integrations = otaIntegrationRepository.findByStoreId(storeId);

        // 如果门店没有OTA配置，则初始化默认配置
        if (integrations.isEmpty()) {
            integrations = initializeDefaultOtaIntegrations(storeId);
        }

        return integrations.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 根据ID获取OTA直连配置
     */
    public OtaIntegrationDTO getOtaIntegrationById(Long id) {
        Long storeId = StoreContextHolder.getContext().getStoreId();
        OtaIntegration integration = otaIntegrationRepository.findById(id)
                .filter(ota -> ota.getStoreId().equals(storeId))
                .orElseThrow(() -> new RuntimeException("OTA配置不存在"));
        return convertToDTO(integration);
    }

    /**
     * 根据渠道代码获取OTA直连配置
     */
    public OtaIntegrationDTO getOtaIntegrationByCode(String code) {
        Long storeId = StoreContextHolder.getContext().getStoreId();
        OtaIntegration integration = otaIntegrationRepository.findByStoreIdAndCode(storeId, code)
                .orElseThrow(() -> new RuntimeException("OTA配置不存在: " + code));
        return convertToDTO(integration);
    }

    /**
     * 更新OTA直连配置
     */
    @Transactional
    public OtaIntegrationDTO updateOtaIntegration(Long id, OtaIntegrationDTO dto) {
        Long storeId = StoreContextHolder.getContext().getStoreId();
        OtaIntegration integration = otaIntegrationRepository.findById(id)
                .filter(ota -> ota.getStoreId().equals(storeId))
                .orElseThrow(() -> new RuntimeException("OTA配置不存在"));

        // 更新可修改的字段
        if (dto.getApiUrl() != null) {
            integration.setApiUrl(dto.getApiUrl());
        }
        if (dto.getPropertyId() != null) {
            integration.setPropertyId(dto.getPropertyId());
        }
        if (dto.getIsConnected() != null) {
            integration.setIsConnected(dto.getIsConnected());
        }
        if (dto.getEnabled() != null) {
            integration.setEnabled(dto.getEnabled());
        }

        OtaIntegration saved = otaIntegrationRepository.save(integration);
        return convertToDTO(saved);
    }

    /**
     * 连接OTA渠道
     */
    @Transactional
    public OtaIntegrationDTO connectOta(Long id, String apiKey, String apiSecret) {
        Long storeId = StoreContextHolder.getContext().getStoreId();
        OtaIntegration integration = otaIntegrationRepository.findById(id)
                .filter(ota -> ota.getStoreId().equals(storeId))
                .orElseThrow(() -> new RuntimeException("OTA配置不存在"));

        integration.setApiKey(apiKey);
        integration.setApiSecret(apiSecret);
        integration.setIsConnected(true);

        OtaIntegration saved = otaIntegrationRepository.save(integration);
        return convertToDTO(saved);
    }

    /**
     * 断开OTA渠道连接
     */
    @Transactional
    public OtaIntegrationDTO disconnectOta(Long id) {
        Long storeId = StoreContextHolder.getContext().getStoreId();
        OtaIntegration integration = otaIntegrationRepository.findById(id)
                .filter(ota -> ota.getStoreId().equals(storeId))
                .orElseThrow(() -> new RuntimeException("OTA配置不存在"));

        integration.setApiKey(null);
        integration.setApiSecret(null);
        integration.setIsConnected(false);

        OtaIntegration saved = otaIntegrationRepository.save(integration);
        return convertToDTO(saved);
    }

    /**
     * 初始化门店默认OTA配置
     */
    @Transactional
    public List<OtaIntegration> initializeDefaultOtaIntegrations(Long storeId) {
        return DEFAULT_OTA_CONFIGS.stream()
                .filter(config -> !otaIntegrationRepository.existsByStoreIdAndCode(storeId, config.code()))
                .map(config -> {
                    OtaIntegration ota = new OtaIntegration(config.name(), config.code());
                    ota.setStoreId(storeId);
                    ota.setLogoUrl(config.logoUrl());
                    ota.setIsConnected(false);
                    ota.setEnabled(true);
                    return otaIntegrationRepository.save(ota);
                })
                .collect(Collectors.toList());
    }

    /**
     * 更新价格调整配置
     */
    @Transactional
    public OtaIntegrationDTO updatePriceAdjustment(Long id, OtaIntegrationDTO dto) {
        Long storeId = StoreContextHolder.getContext().getStoreId();
        OtaIntegration integration = otaIntegrationRepository.findById(id)
                .filter(ota -> ota.getStoreId().equals(storeId))
                .orElseThrow(() -> new RuntimeException("OTA配置不存在"));

        if (dto.getPriceAdjustmentType() != null) {
            integration.setPriceAdjustmentType(dto.getPriceAdjustmentType());
        }
        if (dto.getPriceAdjustmentValue() != null) {
            integration.setPriceAdjustmentValue(dto.getPriceAdjustmentValue());
        }
        if (dto.getAutoSyncPrice() != null) {
            integration.setAutoSyncPrice(dto.getAutoSyncPrice());
        }

        OtaIntegration saved = otaIntegrationRepository.save(integration);
        return convertToDTO(saved);
    }

    /**
     * 生成 Su Widget Token，用于前端加载 Su Channel Mapping Widget。
     */
    @Transactional
    public OtaIntegrationController.WidgetTokenResponse generateSuWidgetToken(Long id) {
        Long storeId = StoreContextHolder.getContext().getStoreId();
        OtaIntegration integration = otaIntegrationRepository.findById(id)
                .filter(ota -> ota.getStoreId().equals(storeId))
                .orElseThrow(() -> new RuntimeException("OTA配置不存在"));

        // 1. 初始化 Su Property ID（如果尚未初始化）
        String hotelId = integration.getSuPropertyId();
        if (hotelId == null || hotelId.isBlank()) {
            Store store = storeRepository.findById(storeId)
                    .orElseThrow(() -> new RuntimeException("门店不存在"));

            String storeHotelId = SuHotelIdUtil.normalize(store.getSuHotelId());
            if (storeHotelId == null) {
                storeHotelId = SuHotelIdUtil.buildDefault(storeId);
                store.setSuHotelId(storeHotelId);
                storeRepository.save(store);
            }

            hotelId = storeHotelId;
            integration.setSuPropertyId(hotelId);
        }

        // 2. PMS -> Su 同步房型/价格计划（用于 Su Widget 下拉，避免 No Record Found）
        // 注意：同步失败不应阻断 Widget Token 生成；失败原因可通过手动同步接口排查
        try {
            suContentSyncService.syncRoomsForWidget(storeId, hotelId);
            suContentSyncService.syncRatePlansForWidget(storeId, hotelId);
        } catch (RuntimeException e) {
            org.slf4j.LoggerFactory.getLogger(OtaIntegrationService.class)
                    .warn(
                            "Su content sync failed before widget token. storeId={}, hotelId={}, err={}",
                            storeId,
                            hotelId,
                            e.getMessage()
                    );
        }

        // 3. 使用 Access Token 生成 Widget Token
        final String resolvedHotelId = hotelId;
        SuApiClient.SuWidgetTokenResponse widgetResponse = suAccessTokenService.executeWithTokenRetry(
                token -> suApiClient.getWidgetToken(resolvedHotelId, token),
                "getWidgetToken"
        );

        // 4. 保存 Widget Token（可选：token 会过期）
        integration.setSuWidgetToken(widgetResponse.getData().getTokenId());
        integration.setSuTokenExpiresAt(LocalDateTime.now().plusHours(1));

        // 5. 更新连接状态
        if (!Boolean.TRUE.equals(integration.getIsConnected())) {
            integration.setConnectedAt(LocalDateTime.now());
        }
        otaIntegrationRepository.save(integration);

        // 6. 获取渠道代码
        String channelCode = suApiClient.getEncryptedChannelCode(integration.getCode());

        // 7. 返回 Widget 配置
        return new OtaIntegrationController.WidgetTokenResponse(
                widgetResponse.getData().getTokenId(),
                widgetResponse.getData().getProppmsid(),
                suApiConfig.getClientId(),
                channelCode,
                "https://static.otaswitch.com/JS/script.js",
                "channel-Mapping",
                "zn"
        );
    }

    /**
     * 手动触发一次 PMS -> Su 房型/价格计划同步（用于排查 Widget 下拉 No Record Found）。
     */
    @Transactional
    public Object syncSuContent(Long id) {
        Long storeId = StoreContextHolder.getContext().getStoreId();
        OtaIntegration integration = otaIntegrationRepository.findById(id)
                .filter(ota -> ota.getStoreId().equals(storeId))
                .orElseThrow(() -> new RuntimeException("OTA配置不存在"));

        String hotelId = resolveOrInitSuHotelId(storeId, integration);
        SuContentSyncService.SuRoomSyncSummary rooms = suContentSyncService.syncRoomsForWidget(storeId, hotelId);
        SuContentSyncService.SuRatePlanSyncSummary ratePlans = suContentSyncService.syncRatePlansForWidget(storeId, hotelId);
        return java.util.Map.of("rooms", rooms, "ratePlans", ratePlans);
    }

    /**
     * PMS -> Su：一键推送“PMS 房间号列表”（用于 Su Widget 映射到具体房间）。
     */
    @Transactional
    public SuContentSyncService.SuRoomSyncSummary syncSuRooms(Long id) {
        Long storeId = StoreContextHolder.getContext().getStoreId();
        OtaIntegration integration = otaIntegrationRepository.findById(id)
                .filter(ota -> ota.getStoreId().equals(storeId))
                .orElseThrow(() -> new RuntimeException("OTA配置不存在"));

        String hotelId = resolveOrInitSuHotelId(storeId, integration);
        return suContentSyncService.syncRoomsForWidget(storeId, hotelId);
    }

    /**
     * PMS -> Su：一键推送“PMS 价格计划列表”（用于 Su Widget 费率计划映射）。
     */
    @Transactional
    public SuContentSyncService.SuRatePlanSyncSummary syncSuRatePlans(Long id) {
        Long storeId = StoreContextHolder.getContext().getStoreId();
        OtaIntegration integration = otaIntegrationRepository.findById(id)
                .filter(ota -> ota.getStoreId().equals(storeId))
                .orElseThrow(() -> new RuntimeException("OTA配置不存在"));

        String hotelId = resolveOrInitSuHotelId(storeId, integration);
        return suContentSyncService.syncRatePlansForWidget(storeId, hotelId);
    }

    /**
     * PMS -> Su: 推送基础 ARI（Rates & Availability，/availability），用于外联网显示 “No rates pushed / No availability pushed”。
     */
    @Transactional
    public SuAriSyncService.SuAriSyncSummary syncSuAri(Long id, Integer days) {
        Long storeId = StoreContextHolder.getContext().getStoreId();
        OtaIntegration integration = otaIntegrationRepository.findById(id)
                .filter(ota -> ota.getStoreId().equals(storeId))
                .orElseThrow(() -> new RuntimeException("OTA配置不存在"));

        String hotelId = resolveOrInitSuHotelId(storeId, integration);
        return suAriSyncService.syncBaseAriForNextDays(storeId, hotelId, days);
    }

    /**
     * 推送 Su 可用性（按房间号维度，未来 N 天）。
     */
    @Transactional
    /**
     * PMS -> Su: 推送房价（按房间号 + 价格计划，未来 N 天；默认 365 天）。
     */
    public SuRateSyncService.SuRateSyncSummary syncSuRates(Long id, Integer days) {
        Long storeId = StoreContextHolder.getContext().getStoreId();
        OtaIntegration integration = otaIntegrationRepository.findById(id)
                .filter(ota -> ota.getStoreId().equals(storeId))
                .orElseThrow(() -> new RuntimeException("OTA配置不存在"));

        String hotelId = resolveOrInitSuHotelId(storeId, integration);
        return suRateSyncService.syncRoomRatesForNextDays(storeId, hotelId, days);
    }

    @Transactional
    public SuAvailabilitySyncService.SuAvailabilitySyncSummary syncSuAvailability(Long id, Integer days) {
        Long storeId = StoreContextHolder.getContext().getStoreId();
        OtaIntegration integration = otaIntegrationRepository.findById(id)
                .filter(ota -> ota.getStoreId().equals(storeId))
                .orElseThrow(() -> new RuntimeException("OTA配置不存在"));

        String hotelId = resolveOrInitSuHotelId(storeId, integration);
        return suAvailabilitySyncService.syncRoomAvailabilityForNextDays(storeId, hotelId, days);
    }

    @Transactional(readOnly = true)
    public JsonNode getSuMappings(Long id, String channelId) {
        Long storeId = StoreContextHolder.getContext().getStoreId();
        OtaIntegration integration = otaIntegrationRepository.findById(id)
                .filter(ota -> ota.getStoreId().equals(storeId))
                .orElseThrow(() -> new RuntimeException("OTA配置不存在"));

        String hotelId = integration.getSuPropertyId();
        if (hotelId == null || hotelId.isBlank()) {
            Store store = storeRepository.findById(storeId)
                    .orElseThrow(() -> new RuntimeException("门店不存在"));
            String storeHotelId = SuHotelIdUtil.normalize(store.getSuHotelId());
            if (storeHotelId == null) {
                storeHotelId = SuHotelIdUtil.buildDefault(storeId);
            }
            hotelId = storeHotelId;
        }

        final String resolvedHotelId = hotelId;
        return suAccessTokenService.executeWithTokenRetry(
                token -> suApiClient.getMappings(token, resolvedHotelId, channelId),
                "mappings"
        );
    }

    private String resolveOrInitSuHotelId(Long storeId, OtaIntegration integration) {
        String hotelId = integration.getSuPropertyId();
        if (hotelId != null && !hotelId.isBlank()) {
            return hotelId;
        }

        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new RuntimeException("门店不存在"));

        String storeHotelId = SuHotelIdUtil.normalize(store.getSuHotelId());
        if (storeHotelId == null) {
            storeHotelId = SuHotelIdUtil.buildDefault(storeId);
            store.setSuHotelId(storeHotelId);
            storeRepository.save(store);
        }

        integration.setSuPropertyId(storeHotelId);
        otaIntegrationRepository.save(integration);
        return storeHotelId;
    }

    private OtaIntegrationDTO convertToDTO(OtaIntegration entity) {
        OtaIntegrationDTO dto = new OtaIntegrationDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setCode(entity.getCode());
        dto.setLogoUrl(entity.getLogoUrl());
        dto.setIsConnected(entity.getIsConnected());
        dto.setApiUrl(entity.getApiUrl());
        dto.setPropertyId(entity.getPropertyId());
        dto.setEnabled(entity.getEnabled());
        dto.setPriceAdjustmentType(entity.getPriceAdjustmentType());
        dto.setPriceAdjustmentValue(entity.getPriceAdjustmentValue());
        dto.setAutoSyncPrice(entity.getAutoSyncPrice());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }

    /**
     * 默认OTA配置记录
     */
    private record OtaConfig(String name, String code, String logoUrl) {}
}
