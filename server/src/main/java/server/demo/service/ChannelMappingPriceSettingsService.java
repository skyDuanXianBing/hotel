package server.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.demo.context.StoreContextHolder;
import server.demo.dto.MappingPriceSettingRowDTO;
import server.demo.dto.MappingPriceSettingRowSaveRequestDTO;
import server.demo.dto.MappingPriceSettingsResponseDTO;
import server.demo.dto.MappingPriceSettingsRetryRequestDTO;
import server.demo.dto.MappingPriceSettingsSaveRequestDTO;
import server.demo.dto.MappingPriceSettingsSaveResponseDTO;
import server.demo.entity.Channel;
import server.demo.entity.ChannelMappingPriceSetting;
import server.demo.entity.OtaIntegration;
import server.demo.entity.Store;
import server.demo.enums.ChannelMappingPriceSyncStatus;
import server.demo.enums.PriceAdjustmentType;
import server.demo.repository.ChannelMappingPriceSettingRepository;
import server.demo.repository.ChannelRepository;
import server.demo.repository.OtaIntegrationRepository;
import server.demo.repository.StoreRepository;
import server.demo.util.OtaChannelPricePolicy;
import server.demo.util.SuHotelIdUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class ChannelMappingPriceSettingsService {

    private static final String CHANNEL_CODE_BOOKING = OtaChannelPricePolicy.CHANNEL_CODE_BOOKING;
    private static final String CHANNEL_CODE_BOOKING_COM = OtaChannelPricePolicy.CHANNEL_CODE_BOOKING_COM;
    private static final String CHANNEL_CODE_AIRBNB = OtaChannelPricePolicy.CHANNEL_CODE_AIRBNB;
    private static final String STATUS_ACTIVE = "Active";
    private static final String SU_CHANNEL_ID_BOOKING = "19";
    private static final String SU_CHANNEL_ID_AIRBNB = "244";
    private static final String MAPPING_KEY_VERSION = "v1";
    private static final BigDecimal DEFAULT_MULTIPLIER = BigDecimal.ONE;
    private static final BigDecimal DEFAULT_SURCHARGE = BigDecimal.ZERO;
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private final ChannelRepository channelRepository;
    private final OtaIntegrationRepository otaIntegrationRepository;
    private final StoreRepository storeRepository;
    private final ChannelMappingPriceSettingRepository settingRepository;
    private final SuApiClient suApiClient;
    private final SuAccessTokenService suAccessTokenService;

    public ChannelMappingPriceSettingsService(
            ChannelRepository channelRepository,
            OtaIntegrationRepository otaIntegrationRepository,
            StoreRepository storeRepository,
            ChannelMappingPriceSettingRepository settingRepository,
            SuApiClient suApiClient,
            SuAccessTokenService suAccessTokenService
    ) {
        this.channelRepository = channelRepository;
        this.otaIntegrationRepository = otaIntegrationRepository;
        this.storeRepository = storeRepository;
        this.settingRepository = settingRepository;
        this.suApiClient = suApiClient;
        this.suAccessTokenService = suAccessTokenService;
    }

    @Transactional(readOnly = true)
    public MappingPriceSettingsResponseDTO listMappingPriceSettings(Long channelId) {
        ResolvedContext context = resolveContext(channelId);
        JsonNode mappings = loadMappings(context);
        List<MappingTarget> targets = extractTargets(context, mappings);
        List<ChannelMappingPriceSetting> settings = settingRepository.findByStoreIdAndChannelIdAndSuPropertyId(
                context.storeId(),
                context.channel().getId(),
                context.hotelId()
        );
        Map<String, ChannelMappingPriceSetting> settingsByMappingKey = indexSettingsByMappingKey(settings);

        MappingPriceSettingsResponseDTO response = new MappingPriceSettingsResponseDTO();
        response.setChannelId(context.channel().getId());
        response.setChannelCode(context.channelCode());
        response.setChannelName(context.channel().getName());
        response.setOtaIntegrationId(context.integration().getId());
        response.setSuChannelId(context.suChannelId());
        response.setSuPropertyId(context.hotelId());
        PriceModifier defaultModifier = buildDefaultModifier(context.channel());
        response.setDefaultMultiplier(defaultModifier.multiplier());
        response.setDefaultSurcharge(defaultModifier.surcharge());

        List<MappingPriceSettingRowDTO> rows = new ArrayList<>();
        for (MappingTarget target : targets) {
            ChannelMappingPriceSetting setting = settingsByMappingKey.get(target.mappingKey());
            rows.add(toRowDto(context, target, setting));
        }
        response.setRows(rows);
        response.refreshCounts();
        return response;
    }

    @Transactional
    public MappingPriceSettingsSaveResponseDTO saveMappingPriceSettings(
            Long channelId,
            MappingPriceSettingsSaveRequestDTO request
    ) {
        ResolvedContext context = resolveContext(channelId);
        JsonNode mappings = loadMappings(context);
        List<MappingTarget> targets = extractTargets(context, mappings);
        Map<String, MappingTarget> targetsByRowKey = indexTargetsByRowKey(targets);
        List<MappingPriceSettingRowSaveRequestDTO> rows = request != null ? request.getRows() : List.of();
        String operationId = resolveOperationId(request != null ? request.getClientOperationId() : null);
        String batchId = UUID.randomUUID().toString();
        return saveRows(context, targetsByRowKey, rows, operationId, batchId, false);
    }

    @Transactional
    public MappingPriceSettingsSaveResponseDTO saveMappingPriceSettingRow(
            Long channelId,
            String rowKey,
            MappingPriceSettingRowSaveRequestDTO request
    ) {
        MappingPriceSettingRowSaveRequestDTO row = new MappingPriceSettingRowSaveRequestDTO();
        row.setRowKey(rowKey);
        if (request != null) {
            row.setMultiplier(request.getMultiplier());
            row.setSurcharge(request.getSurcharge());
        }

        MappingPriceSettingsSaveRequestDTO wrapped = new MappingPriceSettingsSaveRequestDTO();
        wrapped.setRows(List.of(row));
        return saveMappingPriceSettings(channelId, wrapped);
    }

    @Transactional
    public MappingPriceSettingsSaveResponseDTO retryMappingPriceSettings(
            Long channelId,
            MappingPriceSettingsRetryRequestDTO request
    ) {
        ResolvedContext context = resolveContext(channelId);
        JsonNode mappings = loadMappings(context);
        List<MappingTarget> targets = extractTargets(context, mappings);
        Map<String, MappingTarget> targetsByRowKey = indexTargetsByRowKey(targets);

        List<ChannelMappingPriceSetting> settings = loadRetrySettings(context, request);
        List<MappingPriceSettingRowSaveRequestDTO> rows = new ArrayList<>();
        for (ChannelMappingPriceSetting setting : settings) {
            MappingPriceSettingRowSaveRequestDTO row = new MappingPriceSettingRowSaveRequestDTO();
            row.setRowKey(setting.getRowKey());
            row.setMultiplier(setting.getMultiplier());
            row.setSurcharge(setting.getSurcharge());
            rows.add(row);
        }

        String operationId = resolveOperationId(request != null ? request.getClientOperationId() : null);
        String batchId = UUID.randomUUID().toString();
        return saveRows(context, targetsByRowKey, rows, operationId, batchId, true);
    }

    private MappingPriceSettingsSaveResponseDTO saveRows(
            ResolvedContext context,
            Map<String, MappingTarget> targetsByRowKey,
            List<MappingPriceSettingRowSaveRequestDTO> rows,
            String operationId,
            String batchId,
            boolean retrying
    ) {
        MappingPriceSettingsSaveResponseDTO response = new MappingPriceSettingsSaveResponseDTO();
        response.setChannelId(context.channel().getId());
        response.setChannelCode(context.channelCode());
        response.setSuChannelId(context.suChannelId());
        response.setSuPropertyId(context.hotelId());
        response.setOperationId(operationId);
        response.setBatchId(batchId);

        List<MappingPriceSettingRowDTO> resultRows = new ArrayList<>();
        for (MappingPriceSettingRowSaveRequestDTO rowRequest : rows) {
            resultRows.add(saveOneRow(context, targetsByRowKey, rowRequest, operationId, batchId, retrying));
        }

        response.setRows(resultRows);
        response.refreshCounts();
        response.setMessage(buildSaveMessage(response));
        return response;
    }

    private MappingPriceSettingRowDTO saveOneRow(
            ResolvedContext context,
            Map<String, MappingTarget> targetsByRowKey,
            MappingPriceSettingRowSaveRequestDTO rowRequest,
            String operationId,
            String batchId,
            boolean retrying
    ) {
        String rowKey = rowRequest != null ? normalizeText(rowRequest.getRowKey()) : null;
        BigDecimal multiplier = normalizeMultiplier(rowRequest != null ? rowRequest.getMultiplier() : null);
        BigDecimal surcharge = normalizeMoney(rowRequest != null ? rowRequest.getSurcharge() : null);

        if (isBlank(rowKey)) {
            MappingPriceSettingRowDTO failed = new MappingPriceSettingRowDTO();
            failed.setChannelCode(context.channelCode());
            failed.setSuChannelId(context.suChannelId());
            failed.setMultiplier(multiplier);
            failed.setSurcharge(surcharge);
            failed.setSyncStatus(ChannelMappingPriceSyncStatus.FAILED.name());
            failed.setLastError("缺少 rowKey，未同步该条映射");
            return failed;
        }

        MappingTarget target = targetsByRowKey.get(rowKey);
        ChannelMappingPriceSetting setting = findOrCreateSetting(context, rowKey, target);
        if (target != null) {
            applyTargetIdentity(setting, context, target);
        }
        setting.setMultiplier(multiplier);
        setting.setSurcharge(surcharge);
        setting.setLastOperationId(operationId);
        setting.setLastBatchId(batchId);
        setting.setLastAttemptedAt(LocalDateTime.now());
        if (retrying) {
            incrementRetryCount(setting);
        }

        String invalidValue = validateRequestedValues(multiplier);
        if (invalidValue != null) {
            markFailure(setting, ChannelMappingPriceSyncStatus.FAILED, invalidValue, retrying);
            ChannelMappingPriceSetting saved = settingRepository.save(setting);
            return toRowDto(context, target, saved);
        }

        if (target == null) {
            markFailure(setting, ChannelMappingPriceSyncStatus.STALE_MAPPING, "当前 Su mappings 中找不到该 rowKey，未同步该条映射", retrying);
            ChannelMappingPriceSetting saved = settingRepository.save(setting);
            return toRowDto(context, null, saved);
        }

        String validationError = validateTarget(context.channelCode(), target);
        if (validationError != null) {
            markFailure(setting, ChannelMappingPriceSyncStatus.STALE_MAPPING, validationError, retrying);
            ChannelMappingPriceSetting saved = settingRepository.save(setting);
            return toRowDto(context, target, saved);
        }

        PriceModifier modifier = new PriceModifier(multiplier, surcharge);
        try {
            JsonNode suResponse = postMappingUpdate(context, target, modifier);
            if (suApiClient.isSuSuccess(suResponse)) {
                markSuccess(setting);
            } else {
                String errorMessage = suApiClient.extractSuErrorMessage(suResponse);
                if (isBlank(errorMessage)) {
                    errorMessage = "Su 返回非成功状态";
                }
                markFailure(setting, ChannelMappingPriceSyncStatus.FAILED, errorMessage, retrying);
            }
        } catch (Exception e) {
            markFailure(setting, ChannelMappingPriceSyncStatus.FAILED, e.getMessage(), retrying);
        }

        ChannelMappingPriceSetting saved = settingRepository.save(setting);
        return toRowDto(context, target, saved);
    }

    private ResolvedContext resolveContext(Long channelId) {
        Long storeId = StoreContextHolder.getContext().getStoreId();
        Channel channel = channelRepository.findById(channelId)
                .filter(candidate -> storeId.equals(candidate.getStoreId()))
                .orElseThrow(() -> new RuntimeException("渠道不存在"));

        String channelCode = normalizeChannelCode(channel.getCode());
        String suChannelId = resolveSuChannelId(channelCode);
        if (suChannelId == null) {
            throw new RuntimeException("该渠道不支持映射级价格设置");
        }

        OtaIntegration integration = findOtaIntegration(storeId, channelCode, channel.getCode())
                .orElseThrow(() -> new RuntimeException("未找到当前门店的 OTA 配置"));
        String hotelId = resolveReadOnlySuHotelId(storeId, integration);
        return new ResolvedContext(storeId, channel, channelCode, suChannelId, integration, hotelId);
    }

    private Optional<OtaIntegration> findOtaIntegration(Long storeId, String channelCode, String originalCode) {
        Optional<OtaIntegration> direct = otaIntegrationRepository.findByStoreIdAndCode(storeId, channelCode);
        if (direct.isPresent()) {
            return direct;
        }
        if (!isBlank(originalCode)) {
            Optional<OtaIntegration> original = otaIntegrationRepository.findByStoreIdAndCode(
                    storeId,
                    originalCode.trim().toUpperCase()
            );
            if (original.isPresent()) {
                return original;
            }
        }
        if (CHANNEL_CODE_BOOKING_COM.equals(channelCode)) {
            return otaIntegrationRepository.findByStoreIdAndCode(storeId, CHANNEL_CODE_BOOKING);
        }
        return Optional.empty();
    }

    private String resolveReadOnlySuHotelId(Long storeId, OtaIntegration integration) {
        String integrationHotelId = normalizeText(integration.getSuPropertyId());
        if (integrationHotelId != null) {
            return integrationHotelId;
        }

        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new RuntimeException("门店不存在"));
        String storeHotelId = SuHotelIdUtil.normalize(store.getSuHotelId());
        if (storeHotelId == null) {
            return SuHotelIdUtil.buildDefault(storeId);
        }
        return storeHotelId;
    }

    private JsonNode loadMappings(ResolvedContext context) {
        return suAccessTokenService.executeWithTokenRetry(
                token -> suApiClient.getMappings(token, context.hotelId(), context.suChannelId()),
                "mappings"
        );
    }

    private List<MappingTarget> extractTargets(ResolvedContext context, JsonNode root) {
        List<MappingTarget> targets = new ArrayList<>();
        JsonNode channelArray = root != null ? root.get(context.suChannelId()) : null;
        if (channelArray == null || !channelArray.isArray()) {
            return targets;
        }

        int mappingIndex = 0;
        for (JsonNode mappingNode : channelArray) {
            if (mappingNode == null || !mappingNode.isObject()) {
                mappingIndex++;
                continue;
            }
            String channelHotelId = readText(mappingNode, "ChannelHotelID", "ChannelHotelId", "channelhotelid");
            String mappingStatus = readText(mappingNode, "Status", "status");
            List<String> roomIds = readStringArray(mappingNode.get("RoomIDs"));
            JsonNode ratePlans = mappingNode.get("Rateplans");

            if (ratePlans == null || !ratePlans.isArray() || ratePlans.size() == 0) {
                MappingTarget target = buildTarget(context, mappingIndex, 0, 0, mappingNode, null, null, channelHotelId, mappingStatus, first(roomIds));
                targets.add(target);
                mappingIndex++;
                continue;
            }

            int ratePlanIndex = 0;
            for (JsonNode ratePlanNode : ratePlans) {
                if (ratePlanNode == null || !ratePlanNode.isObject()) {
                    ratePlanIndex++;
                    continue;
                }
                List<JsonNode> pricingNodes = readPricingNodes(ratePlanNode);
                int pricingIndex = 0;
                for (JsonNode pricingNode : pricingNodes) {
                    MappingTarget target = buildTarget(
                            context,
                            mappingIndex,
                            ratePlanIndex,
                            pricingIndex,
                            mappingNode,
                            ratePlanNode,
                            pricingNode,
                            channelHotelId,
                            mappingStatus,
                            first(roomIds)
                    );
                    targets.add(target);
                    pricingIndex++;
                }
                ratePlanIndex++;
            }
            mappingIndex++;
        }

        return targets;
    }

    private MappingTarget buildTarget(
            ResolvedContext context,
            int mappingIndex,
            int ratePlanIndex,
            int pricingIndex,
            JsonNode mappingNode,
            JsonNode ratePlanNode,
            JsonNode pricingNode,
            String channelHotelId,
            String mappingStatus,
            String fallbackRoomId
    ) {
        String roomId = firstNonBlank(
                readText(ratePlanNode, "PMSRoomID", "PMSRoomId", "RoomID", "RoomId", "roomid"),
                fallbackRoomId
        );
        String rateId = readText(ratePlanNode, "PMSRateID", "PMSRateId", "RatePlanID", "RateID", "rateid");
        String channelRoomId = readText(
                ratePlanNode,
                "ChannelRoomID",
                "ChannelRoomId",
                "ChannelRoomTypeID",
                "ChannelRoomTypeId",
                "channelroomid"
        );
        String channelRateId = readText(
                ratePlanNode,
                "ChannelRateID",
                "ChannelRateId",
                "ChannelPricePlanID",
                "ChannelPricePlanId",
                "channelrateid"
        );
        String listingId = readText(ratePlanNode, "ListingID", "ListingId", "listingid");
        if (listingId == null && CHANNEL_CODE_AIRBNB.equals(context.channelCode())) {
            listingId = channelRoomId;
        }

        String dimension = firstNonBlank(
                readText(pricingNode, "ApplicableNoOfGuest", "applicablenoofguest", "Occupancy", "occupancy"),
                readText(ratePlanNode, "ApplicableNoOfGuest", "applicablenoofguest", "Occupancy", "occupancy", "fixed_occupany")
        );
        String applicableGuest = null;
        String occupancy = null;
        if (CHANNEL_CODE_AIRBNB.equals(context.channelCode())) {
            occupancy = dimension;
        } else {
            applicableGuest = dimension;
        }

        String targetStatus = firstNonBlank(readText(ratePlanNode, "MappingStatus", "mappingStatus"), mappingStatus);
        BigDecimal currentMultiplier = readBigDecimal(pricingNode, "Multiplier", "multiplier");
        BigDecimal currentSurcharge = readBigDecimal(pricingNode, "Surcharge", "surcharge");
        String mappingKey = buildMappingKey(
                context,
                channelHotelId,
                roomId,
                rateId,
                channelRoomId,
                channelRateId,
                listingId,
                applicableGuest,
                occupancy
        );
        String rowKey = encodeRowKey(mappingKey);

        return new MappingTarget(
                rowKey,
                mappingKey,
                context.channelCode(),
                context.suChannelId(),
                context.hotelId(),
                channelHotelId,
                roomId,
                rateId,
                channelRoomId,
                channelRateId,
                listingId,
                applicableGuest,
                occupancy,
                targetStatus,
                currentMultiplier,
                currentSurcharge,
                buildDisplayName(context.channelCode(), roomId, rateId, channelRoomId, channelRateId, listingId, applicableGuest, occupancy),
                mappingIndex,
                ratePlanIndex,
                pricingIndex
        );
    }

    private JsonNode postMappingUpdate(
            ResolvedContext context,
            MappingTarget target,
            PriceModifier modifier
    ) {
        if (CHANNEL_CODE_AIRBNB.equals(context.channelCode())) {
            Map<String, Object> payload = buildAirbnbListingMapPayload(context.hotelId(), target, modifier);
            return suAccessTokenService.executeWithTokenRetry(
                    token -> suApiClient.postAirbnbListingMap(token, payload),
                    "airbnb/listing/map"
            );
        }

        Map<String, Object> payload = buildBookingRatePlanMapPayload(context.hotelId(), target, modifier);
        return suAccessTokenService.executeWithTokenRetry(
                token -> suApiClient.postBookingRatePlanMap(token, payload),
                "OTA_RatePlanMap"
        );
    }

    private Map<String, Object> buildBookingRatePlanMapPayload(
            String hotelId,
            MappingTarget target,
            PriceModifier modifier
    ) {
        Map<String, Object> pricing = new LinkedHashMap<>();
        pricing.put("applicablenoofguest", parseIntegerOrText(target.applicableNoOfGuest()));
        pricing.put("multiplier", modifier.multiplier());
        pricing.put("surcharge", modifier.surcharge());

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("hotelid", hotelId);
        payload.put("action", "setup");
        payload.put("channelid", Integer.parseInt(SU_CHANNEL_ID_BOOKING));
        payload.put("status", STATUS_ACTIVE);
        payload.put("channelhotelid", target.channelHotelId());
        payload.put("roomid", target.roomId());
        payload.put("rateid", target.rateId());
        payload.put("channelroomid", target.channelRoomId());
        payload.put("channelrateid", target.channelRateId());
        payload.put("pricing", List.of(pricing));
        return payload;
    }

    private Map<String, Object> buildAirbnbListingMapPayload(
            String hotelId,
            MappingTarget target,
            PriceModifier modifier
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("hotelid", hotelId);
        payload.put("channelhotelid", target.channelHotelId());
        payload.put("listingid", target.listingId());
        payload.put("roomid", target.roomId());
        payload.put("rateid", target.rateId());
        payload.put("multiplier", modifier.multiplier());
        payload.put("surcharge", modifier.surcharge());
        payload.put("occupancy", parseIntegerOrText(target.occupancy()));
        return payload;
    }

    private List<ChannelMappingPriceSetting> loadRetrySettings(
            ResolvedContext context,
            MappingPriceSettingsRetryRequestDTO request
    ) {
        List<String> requestedRowKeys = request != null ? request.getRowKeys() : List.of();
        if (requestedRowKeys != null && !requestedRowKeys.isEmpty()) {
            List<String> normalizedKeys = new ArrayList<>();
            for (String rowKey : requestedRowKeys) {
                String normalized = normalizeText(rowKey);
                if (normalized != null) {
                    normalizedKeys.add(normalized);
                }
            }
            if (normalizedKeys.isEmpty()) {
                return List.of();
            }
            return settingRepository.findByStoreIdAndChannelIdAndRowKeyIn(
                    context.storeId(),
                    context.channel().getId(),
                    normalizedKeys
            );
        }

        return settingRepository.findByStoreIdAndChannelIdAndSyncStatusIn(
                context.storeId(),
                context.channel().getId(),
                List.of(ChannelMappingPriceSyncStatus.FAILED, ChannelMappingPriceSyncStatus.STALE_MAPPING)
        );
    }

    private ChannelMappingPriceSetting findOrCreateSetting(
            ResolvedContext context,
            String rowKey,
            MappingTarget target
    ) {
        if (target != null) {
            Optional<ChannelMappingPriceSetting> existing = settingRepository
                    .findByStoreIdAndChannelIdAndSuPropertyIdAndMappingKey(
                            context.storeId(),
                            context.channel().getId(),
                            context.hotelId(),
                            target.mappingKey()
                    );
            if (existing.isPresent()) {
                return existing.get();
            }
        }

        Optional<ChannelMappingPriceSetting> existingByRowKey = settingRepository.findByStoreIdAndChannelIdAndRowKey(
                context.storeId(),
                context.channel().getId(),
                rowKey
        );
        if (existingByRowKey.isPresent()) {
            return existingByRowKey.get();
        }

        ChannelMappingPriceSetting created = new ChannelMappingPriceSetting();
        created.setStoreId(context.storeId());
        created.setChannelId(context.channel().getId());
        created.setOtaIntegrationId(context.integration().getId());
        created.setChannelCode(context.channelCode());
        created.setSuChannelId(context.suChannelId());
        created.setSuPropertyId(context.hotelId());
        created.setMappingKeyVersion(MAPPING_KEY_VERSION);
        created.setMappingKey(target != null ? target.mappingKey() : rowKey);
        created.setRowKey(rowKey);
        return created;
    }

    private void applyTargetIdentity(
            ChannelMappingPriceSetting setting,
            ResolvedContext context,
            MappingTarget target
    ) {
        setting.setStoreId(context.storeId());
        setting.setChannelId(context.channel().getId());
        setting.setOtaIntegrationId(context.integration().getId());
        setting.setChannelCode(context.channelCode());
        setting.setSuChannelId(context.suChannelId());
        setting.setSuPropertyId(context.hotelId());
        setting.setChannelHotelId(target.channelHotelId());
        setting.setMappingKey(target.mappingKey());
        setting.setRowKey(target.rowKey());
        setting.setMappingKeyVersion(MAPPING_KEY_VERSION);
        setting.setRoomId(target.roomId());
        setting.setRateId(target.rateId());
        setting.setChannelRoomId(target.channelRoomId());
        setting.setChannelRateId(target.channelRateId());
        setting.setListingId(target.listingId());
        setting.setApplicableNoOfGuest(target.applicableNoOfGuest());
        setting.setOccupancy(target.occupancy());
        setting.setLastSeenAt(LocalDateTime.now());
    }

    private void markSuccess(ChannelMappingPriceSetting setting) {
        LocalDateTime now = LocalDateTime.now();
        setting.setSyncStatus(ChannelMappingPriceSyncStatus.SUCCESS);
        setting.setLastError(null);
        setting.setLastSyncedAt(now);
        setting.setLastFailedAt(null);
    }

    private void markFailure(
            ChannelMappingPriceSetting setting,
            ChannelMappingPriceSyncStatus status,
            String message,
            boolean retrying
    ) {
        setting.setSyncStatus(status);
        setting.setLastError(message);
        setting.setLastFailedAt(LocalDateTime.now());
    }

    private void incrementRetryCount(ChannelMappingPriceSetting setting) {
        Integer current = setting.getRetryCount();
        if (current == null) {
            current = 0;
        }
        setting.setRetryCount(current + 1);
    }

    private MappingPriceSettingRowDTO toRowDto(
            ResolvedContext context,
            MappingTarget target,
            ChannelMappingPriceSetting setting
    ) {
        MappingPriceSettingRowDTO dto = new MappingPriceSettingRowDTO();
        dto.setChannelCode(context.channelCode());
        dto.setSuChannelId(context.suChannelId());

        if (target != null) {
            dto.setRowKey(target.rowKey());
            dto.setDisplayName(target.displayName());
            dto.setMappingStatus(target.status());
            dto.setChannelHotelId(target.channelHotelId());
            dto.setLocalRoomId(target.roomId());
            dto.setLocalRatePlanId(target.rateId());
            dto.setRemoteRoomId(target.channelRoomId());
            dto.setRemoteRatePlanId(target.channelRateId());
            dto.setListingId(target.listingId());
            dto.setApplicableNoOfGuest(target.applicableNoOfGuest());
            dto.setOccupancy(target.occupancy());
            dto.setMultiplier(valueOrDefault(target.currentMultiplier(), DEFAULT_MULTIPLIER));
            dto.setSurcharge(valueOrDefault(target.currentSurcharge(), DEFAULT_SURCHARGE));
            String validationError = validateTarget(context.channelCode(), target);
            if (validationError != null) {
                dto.setSyncStatus(ChannelMappingPriceSyncStatus.STALE_MAPPING.name());
                dto.setLastError(validationError);
            } else {
                dto.setSyncStatus(ChannelMappingPriceSyncStatus.UNSYNCED.name());
            }
        }

        if (setting == null) {
            return dto;
        }

        dto.setRowKey(setting.getRowKey());
        if (isBlank(dto.getDisplayName())) {
            dto.setDisplayName(buildDisplayName(
                    setting.getChannelCode(),
                    setting.getRoomId(),
                    setting.getRateId(),
                    setting.getChannelRoomId(),
                    setting.getChannelRateId(),
                    setting.getListingId(),
                    setting.getApplicableNoOfGuest(),
                    setting.getOccupancy()
            ));
        }
        dto.setChannelHotelId(firstNonBlank(setting.getChannelHotelId(), dto.getChannelHotelId()));
        dto.setLocalRoomId(firstNonBlank(setting.getRoomId(), dto.getLocalRoomId()));
        dto.setLocalRatePlanId(firstNonBlank(setting.getRateId(), dto.getLocalRatePlanId()));
        dto.setRemoteRoomId(firstNonBlank(setting.getChannelRoomId(), dto.getRemoteRoomId()));
        dto.setRemoteRatePlanId(firstNonBlank(setting.getChannelRateId(), dto.getRemoteRatePlanId()));
        dto.setListingId(firstNonBlank(setting.getListingId(), dto.getListingId()));
        dto.setApplicableNoOfGuest(firstNonBlank(setting.getApplicableNoOfGuest(), dto.getApplicableNoOfGuest()));
        dto.setOccupancy(firstNonBlank(setting.getOccupancy(), dto.getOccupancy()));
        dto.setMultiplier(valueOrDefault(setting.getMultiplier(), dto.getMultiplier()));
        dto.setSurcharge(valueOrDefault(setting.getSurcharge(), dto.getSurcharge()));
        if (setting.getSyncStatus() != null) {
            dto.setSyncStatus(setting.getSyncStatus().name());
        }
        dto.setLastError(setting.getLastError());
        dto.setRetryCount(setting.getRetryCount());
        dto.setLastOperationId(setting.getLastOperationId());
        dto.setLastBatchId(setting.getLastBatchId());
        dto.setLastAttemptedAt(setting.getLastAttemptedAt());
        dto.setLastSyncedAt(setting.getLastSyncedAt());
        dto.setLastFailedAt(setting.getLastFailedAt());
        return dto;
    }

    private Map<String, ChannelMappingPriceSetting> indexSettingsByMappingKey(
            Collection<ChannelMappingPriceSetting> settings
    ) {
        Map<String, ChannelMappingPriceSetting> result = new LinkedHashMap<>();
        for (ChannelMappingPriceSetting setting : settings) {
            if (setting == null || isBlank(setting.getMappingKey())) {
                continue;
            }
            result.put(setting.getMappingKey(), setting);
        }
        return result;
    }

    private Map<String, MappingTarget> indexTargetsByRowKey(List<MappingTarget> targets) {
        Map<String, MappingTarget> result = new LinkedHashMap<>();
        for (MappingTarget target : targets) {
            if (target == null || isBlank(target.rowKey())) {
                continue;
            }
            if (!result.containsKey(target.rowKey())) {
                result.put(target.rowKey(), target);
            }
        }
        return result;
    }

    private String validateTarget(String channelCode, MappingTarget target) {
        if (!isActive(target.status())) {
            return "Su 映射未启用，未同步该条映射";
        }
        if (isBlank(target.channelHotelId())) {
            return "Su 映射缺少 ChannelHotelID，未同步该条映射";
        }
        if (isBlank(target.roomId())) {
            return "Su 映射缺少 PMS roomid，未同步该条映射";
        }
        if (isBlank(target.rateId())) {
            return "Su 映射缺少 PMS rateid，未同步该条映射";
        }
        if (CHANNEL_CODE_AIRBNB.equals(channelCode)) {
            if (isBlank(target.listingId())) {
                return "Airbnb 映射缺少 listingid，未同步该条映射";
            }
            if (isBlank(target.occupancy())) {
                return "Airbnb 映射缺少 occupancy，未同步该条映射";
            }
            return null;
        }
        if (isBlank(target.applicableNoOfGuest())) {
            return "Booking 映射缺少 applicablenoofguest，未同步该条映射";
        }
        if (isBlank(target.channelRoomId())) {
            return "Booking 映射缺少 channelroomid，未同步该条映射";
        }
        if (isBlank(target.channelRateId())) {
            return "Booking 映射缺少 channelrateid，未同步该条映射";
        }
        return null;
    }

    private String validateRequestedValues(BigDecimal multiplier) {
        if (multiplier == null) {
            return "缺少 multiplier，未同步该条映射";
        }
        if (multiplier.compareTo(BigDecimal.ZERO) < 0) {
            return "multiplier 不能小于 0，未同步该条映射";
        }
        return null;
    }

    private PriceModifier buildDefaultModifier(Channel channel) {
        PriceAdjustmentType type = channel.getPriceAdjustmentType();
        if (type == null) {
            type = PriceAdjustmentType.PERCENTAGE;
        }
        BigDecimal value = channel.getPriceAdjustmentValue();
        if (value == null) {
            if (type == PriceAdjustmentType.COMMISSION && channel.getCommissionRate() != null) {
                value = BigDecimal.valueOf(channel.getCommissionRate());
            } else {
                value = BigDecimal.ZERO;
            }
        }
        if (type == PriceAdjustmentType.FIXED) {
            return new PriceModifier(DEFAULT_MULTIPLIER, normalizeMoney(value));
        }
        BigDecimal multiplier = BigDecimal.ONE.add(value.divide(ONE_HUNDRED, 6, RoundingMode.HALF_UP));
        return new PriceModifier(normalizeMultiplier(multiplier), DEFAULT_SURCHARGE);
    }

    private String buildMappingKey(
            ResolvedContext context,
            String channelHotelId,
            String roomId,
            String rateId,
            String channelRoomId,
            String channelRateId,
            String listingId,
            String applicableGuest,
            String occupancy
    ) {
        List<String> parts = new ArrayList<>();
        parts.add(MAPPING_KEY_VERSION);
        parts.add(context.channelCode());
        parts.add(context.suChannelId());
        parts.add(context.hotelId());
        parts.add(valueForKey(channelHotelId));
        if (CHANNEL_CODE_AIRBNB.equals(context.channelCode())) {
            parts.add(valueForKey(listingId));
            parts.add(valueForKey(roomId));
            parts.add(valueForKey(rateId));
            parts.add(valueForKey(occupancy));
        } else {
            parts.add(valueForKey(roomId));
            parts.add(valueForKey(rateId));
            parts.add(valueForKey(channelRoomId));
            parts.add(valueForKey(channelRateId));
            parts.add(valueForKey(applicableGuest));
        }
        return String.join("|", parts);
    }

    private String buildSaveMessage(MappingPriceSettingsSaveResponseDTO response) {
        if ("SUCCESS".equals(response.getStatus())) {
            return "映射级价格设置已全部同步";
        }
        if ("PARTIAL".equals(response.getStatus())) {
            return "映射级价格设置部分同步失败";
        }
        return "映射级价格设置同步失败";
    }

    private static String buildDisplayName(
            String channelCode,
            String roomId,
            String rateId,
            String channelRoomId,
            String channelRateId,
            String listingId,
            String applicableGuest,
            String occupancy
    ) {
        if (CHANNEL_CODE_AIRBNB.equals(channelCode)) {
            return "Airbnb listing " + valueForDisplay(listingId)
                    + " / room " + valueForDisplay(roomId)
                    + " / rate " + valueForDisplay(rateId)
                    + " / occupancy " + valueForDisplay(occupancy);
        }
        return "Booking room " + valueForDisplay(channelRoomId)
                + " / rate " + valueForDisplay(channelRateId)
                + " / PMS " + valueForDisplay(roomId)
                + "-" + valueForDisplay(rateId)
                + " / guests " + valueForDisplay(applicableGuest);
    }

    private static List<JsonNode> readPricingNodes(JsonNode ratePlanNode) {
        List<JsonNode> nodes = new ArrayList<>();
        JsonNode pricingNode = ratePlanNode != null ? ratePlanNode.get("Pricing") : null;
        if (pricingNode == null || pricingNode.isNull()) {
            nodes.add(null);
            return nodes;
        }
        if (pricingNode.isArray()) {
            if (pricingNode.size() == 0) {
                nodes.add(null);
                return nodes;
            }
            for (JsonNode item : pricingNode) {
                nodes.add(item);
            }
            return nodes;
        }
        nodes.add(pricingNode);
        return nodes;
    }

    private static List<String> readStringArray(JsonNode arrayNode) {
        List<String> values = new ArrayList<>();
        if (arrayNode == null || !arrayNode.isArray()) {
            return values;
        }
        for (JsonNode node : arrayNode) {
            if (node == null || node.isNull()) {
                continue;
            }
            String text = normalizeText(node.asText(""));
            if (text != null) {
                values.add(text);
            }
        }
        return values;
    }

    private static String readText(JsonNode node, String... fields) {
        if (node == null || node.isNull() || fields == null) {
            return null;
        }
        for (String field : fields) {
            if (isBlank(field)) {
                continue;
            }
            JsonNode valueNode = node.get(field);
            if (valueNode == null || valueNode.isNull()) {
                continue;
            }
            String value = normalizeText(valueNode.asText(""));
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private static BigDecimal readBigDecimal(JsonNode node, String... fields) {
        String value = readText(node, fields);
        if (value == null) {
            return null;
        }
        try {
            return new BigDecimal(value).stripTrailingZeros();
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Object parseIntegerOrText(String value) {
        String normalized = normalizeText(value);
        if (normalized == null) {
            return value;
        }
        try {
            return Integer.parseInt(normalized);
        } catch (NumberFormatException e) {
            return normalized;
        }
    }

    private static boolean isActive(String status) {
        return status == null || status.isBlank() || STATUS_ACTIVE.equalsIgnoreCase(status.trim());
    }

    private static String first(List<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        return values.get(0);
    }

    private static String firstNonBlank(String first, String second) {
        if (!isBlank(first)) {
            return first.trim();
        }
        if (!isBlank(second)) {
            return second.trim();
        }
        return null;
    }

    private static String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isBlank()) {
            return null;
        }
        return trimmed;
    }

    private static String normalizeChannelCode(String channelCode) {
        if (channelCode == null) {
            return "";
        }
        String normalized = channelCode.trim().toUpperCase();
        if (CHANNEL_CODE_BOOKING_COM.equals(normalized)) {
            return CHANNEL_CODE_BOOKING;
        }
        return normalized;
    }

    private static String resolveSuChannelId(String channelCode) {
        if (CHANNEL_CODE_BOOKING.equals(channelCode)) {
            return SU_CHANNEL_ID_BOOKING;
        }
        if (CHANNEL_CODE_AIRBNB.equals(channelCode)) {
            return SU_CHANNEL_ID_AIRBNB;
        }
        return null;
    }

    private static String resolveOperationId(String clientOperationId) {
        String normalized = normalizeText(clientOperationId);
        if (normalized != null) {
            return normalized;
        }
        return UUID.randomUUID().toString();
    }

    private static String encodeRowKey(String mappingKey) {
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(mappingKey.getBytes(StandardCharsets.UTF_8));
    }

    private static String valueForKey(String value) {
        String normalized = normalizeText(value);
        if (normalized == null) {
            return "";
        }
        return normalized.replace("|", "%7C");
    }

    private static String valueForDisplay(String value) {
        String normalized = normalizeText(value);
        if (normalized == null) {
            return "-";
        }
        return normalized;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isBlank();
    }

    private static BigDecimal normalizeMultiplier(BigDecimal value) {
        BigDecimal effective = value != null ? value : DEFAULT_MULTIPLIER;
        return effective.setScale(6, RoundingMode.HALF_UP).stripTrailingZeros();
    }

    private static BigDecimal normalizeMoney(BigDecimal value) {
        BigDecimal effective = value != null ? value : DEFAULT_SURCHARGE;
        return effective.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros();
    }

    private static BigDecimal valueOrDefault(BigDecimal value, BigDecimal defaultValue) {
        if (value != null) {
            return value;
        }
        return defaultValue;
    }

    private record ResolvedContext(
            Long storeId,
            Channel channel,
            String channelCode,
            String suChannelId,
            OtaIntegration integration,
            String hotelId
    ) {}

    private record PriceModifier(BigDecimal multiplier, BigDecimal surcharge) {}

    private record MappingTarget(
            String rowKey,
            String mappingKey,
            String channelCode,
            String suChannelId,
            String suPropertyId,
            String channelHotelId,
            String roomId,
            String rateId,
            String channelRoomId,
            String channelRateId,
            String listingId,
            String applicableNoOfGuest,
            String occupancy,
            String status,
            BigDecimal currentMultiplier,
            BigDecimal currentSurcharge,
            String displayName,
            int mappingIndex,
            int ratePlanIndex,
            int pricingIndex
    ) {}
}
