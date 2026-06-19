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
import server.demo.util.StoreTimeZoneUtil;
import server.demo.util.SuHotelIdUtil;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
    private final Clock clock;

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
            SuAccessTokenService suAccessTokenService,
            Clock clock
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
        this.clock = clock;
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
        return generateSuWidgetToken(id, false, null);
    }

    /**
     * 生成 Su Widget Token，用于前端加载 Su Channel Mapping Widget。
     * <p>
     * 说明：为避免前端加载 Widget 时因“顺带同步房型/价格计划”导致超时，默认不做同步；
     * 仅当 syncContent=true 时才同步 rooms/plans 到 Su（用于刷新 Widget 下拉选项）。
     */
    @Transactional
    public OtaIntegrationController.WidgetTokenResponse generateSuWidgetToken(Long id, boolean syncContent) {
        return generateSuWidgetToken(id, syncContent, null);
    }

    /**
     * 生成 Su Widget Token（支持按请求指定 Widget 展示语言）。
     */
    @Transactional
    public OtaIntegrationController.WidgetTokenResponse generateSuWidgetToken(
            Long id,
            boolean syncContent,
            String language
    ) {
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
        if (syncContent) {
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
        String channelId = resolveSuWidgetChannelId(integration.getCode());
        String channelCode = suApiClient.getEncryptedChannelCode(integration.getCode());
        String widgetLanguage = resolveWidgetLanguage(language);

        // 7. 返回 Widget 配置
        return new OtaIntegrationController.WidgetTokenResponse(
                widgetResponse.getData().getTokenId(),
                widgetResponse.getData().getProppmsid(),
                suApiConfig.getClientId(),
                channelId,
                channelCode,
                "https://static.otaswitch.com/JS/script.js",
                "channel-Mapping",
                widgetLanguage
        );
    }

    private static String resolveWidgetLanguage(String language) {
        if (language == null) {
            return "zn";
        }
        String normalized = language.trim().toLowerCase();
        if ("en".equals(normalized) || "en-us".equals(normalized) || "en_gb".equals(normalized) || "en-gb".equals(normalized)) {
            return "en";
        }
        return "zn";
    }

    private static String resolveSuWidgetChannelId(String otaCode) {
        if (otaCode == null) {
            return "";
        }
        String code = otaCode.trim().toUpperCase();
        if ("BOOKING".equals(code) || "BOOKING.COM".equals(code)) {
            return "19";
        }
        if ("AIRBNB".equals(code)) {
            return "244";
        }
        return "";
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
     * PMS -> Su：一键推送“PMS 房型列表（Room Types）”（认证要求：roomid=roomTypeId）。
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
        int d = days != null ? days : 365;
        if (d < 1) {
            d = 1;
        }
        if (d > 500) {
            d = 500;
        }
        LocalDate startDate = currentStoreDate(storeId);
        LocalDate endDate = startDate.plusDays(d - 1L);
        return suAriSyncService.syncAriForDateRange(storeId, hotelId, startDate, endDate, null, null, true, true, true, false);
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
        List<Integer> otaCodes = resolveSuOtaCodes(integration);
        return suRateSyncService.syncRoomRatesForNextDays(storeId, hotelId, days, otaCodes);
    }

    @Transactional
    public SuAvailabilitySyncService.SuAvailabilitySyncSummary syncSuAvailability(Long id, Integer days) {
        Long storeId = StoreContextHolder.getContext().getStoreId();
        OtaIntegration integration = otaIntegrationRepository.findById(id)
                .filter(ota -> ota.getStoreId().equals(storeId))
                .orElseThrow(() -> new RuntimeException("OTA配置不存在"));

        String hotelId = resolveOrInitSuHotelId(storeId, integration);
        int d = days != null ? days : 365;
        if (d < 1) {
            d = 1;
        }
        if (d > 500) {
            d = 500;
        }

        LocalDate startDate = currentStoreDate(storeId);
        LocalDate endDate = startDate.plusDays(d - 1L);
        SuAriSyncService.SuAriSyncSummary summary = suAriSyncService.syncAriForDateRange(
                storeId,
                hotelId,
                startDate,
                endDate,
                null,
                null,
                true,
                false,
                false,
                false
        );

        return new SuAvailabilitySyncService.SuAvailabilitySyncSummary(
                summary.roomCount(),
                summary.days(),
                summary.startDate(),
                summary.endDate(),
                summary.availabilityPushed(),
                summary.error()
        );
    }

    private static List<Integer> resolveSuOtaCodes(OtaIntegration integration) {
        if (integration == null || integration.getCode() == null) {
            return List.of(19, 244);
        }
        String code = integration.getCode().trim().toUpperCase();
        if ("BOOKING".equals(code) || "BOOKING.COM".equals(code)) {
            return List.of(19);
        }
        if ("AIRBNB".equals(code)) {
            return List.of(244);
        }
        return List.of(19, 244);
    }

    @Transactional(readOnly = true)
    public JsonNode getSuMappings(Long id, String channelId) {
        Long storeId = StoreContextHolder.getContext().getStoreId();
        OtaIntegration integration = otaIntegrationRepository.findById(id)
                .filter(ota -> ota.getStoreId().equals(storeId))
                .orElseThrow(() -> new RuntimeException("OTA配置不存在"));

        return getSuMappingsForIntegration(storeId, integration, channelId);
    }

    @Transactional(readOnly = true)
    public JsonNode getSuMappingsByStoreAndCode(Long storeId, String code, String channelId) {
        if (storeId == null) {
            throw new IllegalArgumentException("storeId 不能为空");
        }
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("OTA code 不能为空");
        }
        OtaIntegration integration = otaIntegrationRepository.findByStoreIdAndCode(storeId, code.trim().toUpperCase())
                .orElseThrow(() -> new RuntimeException("OTA配置不存在: " + code));

        return getSuMappingsForIntegration(storeId, integration, channelId);
    }

    private JsonNode getSuMappingsForIntegration(Long storeId, OtaIntegration integration, String channelId) {
        String hotelId = resolveReadOnlySuHotelId(storeId, integration);

        final String resolvedHotelId = hotelId;
        return suAccessTokenService.executeWithTokenRetry(
                token -> suApiClient.getMappings(token, resolvedHotelId, channelId),
                "mappings"
        );
    }

    private String resolveReadOnlySuHotelId(Long storeId, OtaIntegration integration) {
        String hotelId = integration.getSuPropertyId();
        if (hotelId != null && !hotelId.isBlank()) {
            return hotelId;
        }

        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new RuntimeException("门店不存在"));
        String storeHotelId = SuHotelIdUtil.normalize(store.getSuHotelId());
        if (storeHotelId == null) {
            storeHotelId = SuHotelIdUtil.buildDefault(storeId);
        }
        return storeHotelId;
    }

    public record SuMappingStatusSummary(
            String channelId,
            boolean mappingReady,
            int mappedRoomIdCount,
            int mappedRatePlanCount,
            int activeRatePlanCount,
            String error
    ) {}

    /**
     * 判断 Su 侧映射是否完成：必须“房型 + 价格计划”都已映射。
     * <p>
     * 数据来源：POST /SUAPI/jservice/mappings（见 docs/映射/映射.txt）。
     */
    @Transactional(readOnly = true)
    public SuMappingStatusSummary getSuMappingStatus(Long id, String channelId) {
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
        try {
            JsonNode mappings = suAccessTokenService.executeWithTokenRetry(
                    token -> suApiClient.getMappings(token, resolvedHotelId, channelId),
                    "mappings"
            );
            return parseSuMappingStatus(channelId, mappings);
        } catch (Exception e) {
            return new SuMappingStatusSummary(channelId, false, 0, 0, 0, e.getMessage());
        }
    }

    private static SuMappingStatusSummary parseSuMappingStatus(String channelId, JsonNode root) {
        if (root == null || root.isNull()) {
            return new SuMappingStatusSummary(channelId, false, 0, 0, 0, "Empty response");
        }

        JsonNode statusNode = root.get("Status");
        if (statusNode == null) {
            statusNode = root.get("status");
        }
        if (statusNode != null && "Fail".equalsIgnoreCase(statusNode.asText(""))) {
            String err = null;
            JsonNode errors = root.get("Errors");
            if (errors != null && errors.isObject()) {
                JsonNode shortText = errors.get("ShortText");
                if (shortText != null && !shortText.asText("").isBlank()) {
                    err = shortText.asText("");
                }
            }
            return new SuMappingStatusSummary(channelId, false, 0, 0, 0, err != null ? err : "Status=Fail");
        }

        // Success shape example:
        // { "9": [ { "RoomIDs": ["STD"], "Rateplans": [ ... ] } ] }
        JsonNode channelArray = null;
        if (channelId != null && !channelId.isBlank()) {
            channelArray = root.get(channelId);
        }
        if (channelArray == null || !channelArray.isArray()) {
            channelArray = firstArrayField(root);
        }
        if (channelArray == null || !channelArray.isArray()) {
            return new SuMappingStatusSummary(channelId, false, 0, 0, 0, "Unexpected mappings response shape");
        }

        int roomIdCount = 0;
        int ratePlanCount = 0;
        int activeRatePlanCount = 0;
        boolean ready = false;

        for (JsonNode item : channelArray) {
            if (item == null || item.isNull() || !item.isObject()) {
                continue;
            }
            String mappingStatus = text(item, "Status");

            JsonNode roomIds = item.get("RoomIDs");
            int rooms = roomIds != null && roomIds.isArray() ? roomIds.size() : 0;

            JsonNode ratePlans = item.get("Rateplans");
            int plans = ratePlans != null && ratePlans.isArray() ? ratePlans.size() : 0;
            int activePlans = 0;
            if (ratePlans != null && ratePlans.isArray()) {
                for (JsonNode rp : ratePlans) {
                    if (rp == null || rp.isNull() || !rp.isObject()) {
                        continue;
                    }
                    String rpStatus = text(rp, "MappingStatus");
                    if (rpStatus != null && "Active".equalsIgnoreCase(rpStatus)) {
                        activePlans++;
                    }
                }
            }

            roomIdCount = Math.max(roomIdCount, rooms);
            ratePlanCount = Math.max(ratePlanCount, plans);
            activeRatePlanCount = Math.max(activeRatePlanCount, activePlans);

            boolean statusOk = mappingStatus == null || mappingStatus.isBlank() || "Active".equalsIgnoreCase(mappingStatus);
            if (statusOk && rooms > 0 && activePlans > 0) {
                ready = true;
            }
        }

        return new SuMappingStatusSummary(channelId, ready, roomIdCount, ratePlanCount, activeRatePlanCount, null);
    }

    private static JsonNode firstArrayField(JsonNode root) {
        if (root == null || !root.isObject()) {
            return null;
        }
        Iterator<Map.Entry<String, JsonNode>> it = root.fields();
        while (it.hasNext()) {
            Map.Entry<String, JsonNode> e = it.next();
            JsonNode v = e.getValue();
            if (v != null && v.isArray()) {
                return v;
            }
        }
        return null;
    }

    private static String text(JsonNode node, String field) {
        if (node == null || node.isNull() || field == null) {
            return null;
        }
        JsonNode v = node.get(field);
        if (v == null || v.isNull()) {
            return null;
        }
        String s = v.asText(null);
        if (s == null) {
            return null;
        }
        s = s.trim();
        return s.isBlank() ? null : s;
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

    private LocalDate currentStoreDate(Long storeId) {
        ZoneId zoneId = StoreTimeZoneUtil.resolveZoneId(storeRepository.findById(storeId).orElse(null));
        return LocalDate.now(clock.withZone(zoneId));
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
