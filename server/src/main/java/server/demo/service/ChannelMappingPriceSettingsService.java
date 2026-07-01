package server.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import server.demo.exception.SuApiUnauthorizedException;
import server.demo.repository.ChannelMappingPriceSettingRepository;
import server.demo.repository.ChannelRepository;
import server.demo.repository.OtaIntegrationRepository;
import server.demo.repository.StoreRepository;
import server.demo.util.OtaChannelPricePolicy;
import server.demo.util.SmartLockMaskingUtils;
import server.demo.util.SuHotelIdUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class ChannelMappingPriceSettingsService {

    private static final Logger logger = LoggerFactory.getLogger(ChannelMappingPriceSettingsService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

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
    private static final int AIRBNB_LISTING_NAME_MAX_CODE_POINTS = 50;
    private static final int AUDIT_TEXT_MAX_LENGTH = 500;
    private static final String REDACTED_AUDIT_VALUE = "[REDACTED]";
    private static final Pattern AUDIT_EMAIL_PATTERN = Pattern.compile(
            "[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}"
    );
    private static final Pattern AUDIT_PHONE_PATTERN = Pattern.compile(
            "(?<!\\w)(?:\\+?\\d[\\d\\s().-]{7,}\\d)(?!\\w)"
    );
    private static final Pattern AUDIT_URL_PATTERN = Pattern.compile(
            "https?://\\S+",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern AUDIT_TOKEN_PATTERN = Pattern.compile(
            "(?i)\\b(?:tok|sk|sec|ghp|xoxb|xoxp|bearer)_[A-Za-z0-9._~-]{6,}\\b"
    );
    private static final Pattern AUDIT_TOKEN_KEYWORD_PATTERN = Pattern.compile(
            "(?i)(\\b(?:access[-_\\s]*token|refresh[-_\\s]*token|client[-_\\s]*secret|api[-_\\s]*key|secret|token)\\b\\s*(?:is|was|value)?\\s*[:=]?\\s*)([A-Za-z0-9._~+/=-]{8,})"
    );
    private static final Pattern AUDIT_SENSITIVE_LABEL_VALUE_PATTERN = Pattern.compile(
            "(?i)(\\b(?:check[_\\s-]*in(?:[_\\s-]*(?:option|instruction|instructions))?(?:\\.[A-Za-z0-9_.-]+)?|instruction|instructions|address|street|location|directions?|door(?:\\s*code)?|entry(?:\\s*(?:instruction|code))?|access(?:\\s*code)?|gate(?:\\s*code)?|lockbox|keypad|room(?:\\s*(?:access|entry|code))?)\\b\\s*[:=]\\s*)(\"[^\"]*\"|'[^']*'|[^,;{}\\]\\r\\n]+)"
    );
    private static final Pattern AUDIT_STREET_ADDRESS_PATTERN = Pattern.compile(
            "(?i)\\b\\d{1,6}\\s+[A-Za-z0-9 .'-]{2,60}\\s(?:street|st\\.?|avenue|ave\\.?|road|rd\\.?|boulevard|blvd\\.?|lane|ln\\.?|drive|dr\\.?|court|ct\\.?|terrace|ter\\.?|way|place|pl\\.?|circle|cir\\.?)\\b(?:\\s*(?:apt|apartment|suite|unit|#)\\s*[A-Za-z0-9-]+)?"
    );
    private static final Pattern AUDIT_ENTRY_INSTRUCTION_SENTENCE_PATTERN = Pattern.compile(
            "(?i)(?:^|[.;]\\s+)([^.;\\r\\n]*(?:lockbox|front door|back door|keypad|door code|gate code|entry code|access code|钥匙|密码|门口|门牌|入内|入住)[^.;\\r\\n]*)"
    );
    private static final String AIRBNB_LISTING_DETAILS_REQUIRED =
            "Airbnb listing update requires listing details before multiplier can be changed";
    private static final String AIRBNB_LISTING_NAME_REQUIRED =
            "Airbnb listing name is required; please set it in Airbnb/Su before retrying.";
    private static final String AIRBNB_LISTING_NAME_TOO_LONG =
            "Airbnb listing name exceeds Su limit of 50 characters; please shorten it in Airbnb/Su before retrying.";
    private static final String AIRBNB_CHECK_IN_REQUIRED =
            "Airbnb check-in instructions are incomplete in Su/Airbnb; please complete check-in instructions in Su/Airbnb before retrying.";

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
        Map<String, PriceModifier> successfulModifiersByRowKey = new LinkedHashMap<>();
        for (MappingPriceSettingRowSaveRequestDTO rowRequest : rows) {
            MappingPriceSettingRowDTO resultRow = saveOneRow(
                    context,
                    targetsByRowKey,
                    successfulModifiersByRowKey,
                    rowRequest,
                    operationId,
                    batchId,
                    retrying
            );
            resultRows.add(resultRow);
            if (ChannelMappingPriceSyncStatus.SUCCESS.name().equals(resultRow.getSyncStatus())) {
                String syncedRowKey = normalizeText(resultRow.getRowKey());
                if (syncedRowKey != null) {
                    successfulModifiersByRowKey.put(
                            syncedRowKey,
                            new PriceModifier(resultRow.getMultiplier(), resultRow.getSurcharge())
                    );
                }
            }
        }

        response.setRows(resultRows);
        response.refreshCounts();
        response.setMessage(buildSaveMessage(response));
        return response;
    }

    private MappingPriceSettingRowDTO saveOneRow(
            ResolvedContext context,
            Map<String, MappingTarget> targetsByRowKey,
            Map<String, PriceModifier> preservedModifiersByRowKey,
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
            MappingUpdateResult updateResult = postMappingUpdate(
                    context,
                    target,
                    modifier,
                    preservedModifiersByRowKey,
                    operationId,
                    batchId,
                    rowKey
            );
            applySuAudit(setting, updateResult);
            JsonNode suResponse = updateResult.response();
            if (suApiClient.isSuSuccess(suResponse)) {
                markSuccess(setting);
            } else {
                String errorMessage = buildUserFacingSuErrorMessage(suResponse);
                if (isBlank(errorMessage)) {
                    errorMessage = "Su 返回非成功状态";
                }
                markFailure(setting, ChannelMappingPriceSyncStatus.FAILED, errorMessage, retrying);
                logSuRowFailure(context, rowKey, operationId, batchId, updateResult);
            }
        } catch (MappingUpdateValidationException e) {
            applySuAudit(setting, e.toUpdateResult());
            markFailure(setting, ChannelMappingPriceSyncStatus.FAILED, e.getMessage(), retrying);
            logSuRowFailure(context, rowKey, operationId, batchId, e.toUpdateResult());
        } catch (MappingUpdateCallException e) {
            applySuAudit(setting, e.toUpdateResult());
            markFailure(setting, ChannelMappingPriceSyncStatus.FAILED, e.getMessage(), retrying);
            logSuRowFailure(context, rowKey, operationId, batchId, e.toUpdateResult());
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
                MappingTarget target = buildTarget(
                        context,
                        mappingIndex,
                        0,
                        0,
                        mappingNode,
                        null,
                        null,
                        List.of(),
                        channelHotelId,
                        mappingStatus,
                        first(roomIds)
                );
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
                            pricingNodes,
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
            List<JsonNode> pricingNodes,
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
        List<BookingPricingTarget> bookingPricingTargets = List.of();
        Map<String, Object> bookingRatePlanFields = Map.of();
        if (!CHANNEL_CODE_AIRBNB.equals(context.channelCode())) {
            bookingPricingTargets = buildBookingPricingTargets(
                    context,
                    pricingNodes,
                    channelHotelId,
                    roomId,
                    rateId,
                    channelRoomId,
                    channelRateId
            );
            bookingRatePlanFields = buildBookingRatePlanFields(ratePlanNode);
        }
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
                bookingPricingTargets,
                bookingRatePlanFields,
                buildDisplayName(context.channelCode(), roomId, rateId, channelRoomId, channelRateId, listingId, applicableGuest, occupancy),
                mappingIndex,
                ratePlanIndex,
                pricingIndex
        );
    }

    private MappingUpdateResult postMappingUpdate(
            ResolvedContext context,
            MappingTarget target,
            PriceModifier modifier,
            Map<String, PriceModifier> preservedModifiersByRowKey,
            String operationId,
            String batchId,
            String rowKey
    ) {
        if (CHANNEL_CODE_AIRBNB.equals(context.channelCode())) {
            return suAccessTokenService.executeWithTokenRetry(
                    token -> {
                        Map<String, Object> retrievePayload = buildAirbnbListingRetrievePayload(context.hotelId(), target);
                        JsonNode listingResponse = suApiClient.retrieveAirbnbListing(token, retrievePayload);
                        AirbnbUpdatePayloadResult payloadResult = buildAirbnbListingUpdatePayload(
                                context.hotelId(),
                                target,
                                modifier,
                                listingResponse,
                                operationId,
                                batchId,
                                rowKey
                        );
                        JsonNode response;
                        try {
                            response = suApiClient.postAirbnbListingUpdate(token, payloadResult.payload());
                        } catch (RuntimeException e) {
                            if (e instanceof SuApiUnauthorizedException) {
                                throw e;
                            }
                            throw new MappingUpdateCallException(
                                    e.getMessage(),
                                    "airbnb/listing/update",
                                    payloadResult.payloadSummary(),
                                    null,
                                    payloadResult.airbnbListingNameSnapshot(),
                                    e
                            );
                        }
                        return new MappingUpdateResult(
                                "airbnb/listing/update",
                                payloadResult.payloadSummary(),
                                buildResponseSummary("airbnb/listing/update", response),
                                response,
                                payloadResult.airbnbListingNameSnapshot()
                        );
                    },
                    "airbnb/listing/update"
            );
        }

        Map<String, Object> payload = buildBookingRatePlanMapPayload(
                context.hotelId(),
                target,
                modifier,
                preservedModifiersByRowKey
        );
        Map<String, Object> payloadSummary = buildPayloadSummary(
                "OTA_RatePlanMap",
                context,
                target,
                payload,
                operationId,
                batchId,
                rowKey,
                null,
                null,
                null,
                null
        );
        return suAccessTokenService.executeWithTokenRetry(
                token -> {
                    JsonNode response;
                    try {
                        response = suApiClient.postBookingRatePlanMap(token, payload);
                    } catch (RuntimeException e) {
                        if (e instanceof SuApiUnauthorizedException) {
                            throw e;
                        }
                        throw new MappingUpdateCallException(
                                e.getMessage(),
                                "OTA_RatePlanMap",
                                payloadSummary,
                                null,
                                null,
                                e
                        );
                    }
                    return new MappingUpdateResult(
                            "OTA_RatePlanMap",
                            payloadSummary,
                            buildResponseSummary("OTA_RatePlanMap", response),
                            response,
                            null
                    );
                },
                "OTA_RatePlanMap"
        );
    }

    private Map<String, Object> buildBookingRatePlanMapPayload(
            String hotelId,
            MappingTarget target,
            PriceModifier modifier,
            Map<String, PriceModifier> preservedModifiersByRowKey
    ) {
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
        for (Map.Entry<String, Object> entry : target.bookingRatePlanFields().entrySet()) {
            payload.put(entry.getKey(), entry.getValue());
        }
        payload.putIfAbsent("derivedrateids", List.of());
        payload.put("pricing", buildBookingPricingPayload(target, modifier, preservedModifiersByRowKey));
        return payload;
    }

    private List<Map<String, Object>> buildBookingPricingPayload(
            MappingTarget target,
            PriceModifier modifier,
            Map<String, PriceModifier> preservedModifiersByRowKey
    ) {
        List<Map<String, Object>> pricingPayload = new ArrayList<>();
        boolean includedTargetGuest = false;
        for (BookingPricingTarget pricingTarget : target.bookingPricingTargets()) {
            if (pricingTarget == null || isBlank(pricingTarget.applicableNoOfGuest())) {
                continue;
            }
            PriceModifier effectiveModifier = preservedModifiersByRowKey.get(pricingTarget.rowKey());
            if (effectiveModifier == null && pricingTarget.applicableNoOfGuest().equals(target.applicableNoOfGuest())) {
                effectiveModifier = modifier;
            }
            if (effectiveModifier == null) {
                effectiveModifier = new PriceModifier(
                        valueOrDefault(pricingTarget.multiplier(), DEFAULT_MULTIPLIER),
                        valueOrDefault(pricingTarget.surcharge(), DEFAULT_SURCHARGE)
                );
            }

            Map<String, Object> pricing = new LinkedHashMap<>();
            pricing.put("applicablenoofguest", parseIntegerOrText(pricingTarget.applicableNoOfGuest()));
            pricing.put("multiplier", effectiveModifier.multiplier());
            pricing.put("surcharge", effectiveModifier.surcharge());
            pricingPayload.add(pricing);
            if (pricingTarget.applicableNoOfGuest().equals(target.applicableNoOfGuest())) {
                includedTargetGuest = true;
            }
        }

        if (!includedTargetGuest) {
            Map<String, Object> pricing = new LinkedHashMap<>();
            pricing.put("applicablenoofguest", parseIntegerOrText(target.applicableNoOfGuest()));
            pricing.put("multiplier", modifier.multiplier());
            pricing.put("surcharge", modifier.surcharge());
            pricingPayload.add(pricing);
        }

        return pricingPayload;
    }

    private Map<String, Object> buildAirbnbListingRetrievePayload(
            String hotelId,
            MappingTarget target
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("hotelid", hotelId);
        payload.put("channelhotelid", target.channelHotelId());
        payload.put("listingid", target.listingId());
        return payload;
    }

    private AirbnbUpdatePayloadResult buildAirbnbListingUpdatePayload(
            String hotelId,
            MappingTarget target,
            PriceModifier modifier,
            JsonNode listingResponse,
            String operationId,
            String batchId,
            String rowKey
    ) {
        JsonNode listingDetails = extractAirbnbListingDetails(listingResponse);
        if (listingDetails == null || !listingDetails.isObject()) {
            String errorMessage = suApiClient.extractSuErrorMessage(listingResponse);
            if (isBlank(errorMessage)) {
                errorMessage = "listing details not found";
            }
            Map<String, Object> payloadSummary = buildPayloadSummary(
                    "airbnb/listing/update",
                    null,
                    target,
                    Map.of(),
                    operationId,
                    batchId,
                    rowKey,
                    null,
                    null,
                    false,
                    false
            );
            throw new MappingUpdateValidationException(
                    AIRBNB_LISTING_DETAILS_REQUIRED + ": " + errorMessage,
                    "airbnb/listing/update",
                    payloadSummary,
                    buildValidationResponseSummary(
                            "airbnb/listing/update",
                            AIRBNB_LISTING_DETAILS_REQUIRED + ": " + errorMessage,
                            listingResponse
                    ),
                    new AirbnbListingNameSnapshot(null, null)
            );
        }

        String listingName = readStringValue(listingDetails, "name", "Name", "listing_name", "listingName");
        if (listingName == null || listingName.trim().isBlank()) {
            AirbnbListingNameSnapshot listingNameSnapshot = new AirbnbListingNameSnapshot(null, 0);
            Map<String, Object> payloadSummary = buildPayloadSummary(
                    "airbnb/listing/update",
                    null,
                    target,
                    Map.of(),
                    operationId,
                    batchId,
                    rowKey,
                    0,
                    false,
                    hasCheckInOptionObject(listingDetails),
                    hasCheckInInstruction(listingDetails)
            );
            throw new MappingUpdateValidationException(
                    AIRBNB_LISTING_NAME_REQUIRED,
                    "airbnb/listing/update",
                    payloadSummary,
                    buildValidationResponseSummary("airbnb/listing/update", AIRBNB_LISTING_NAME_REQUIRED, listingResponse),
                    listingNameSnapshot
            );
        }

        int nameLength = listingName.codePointCount(0, listingName.length());
        AirbnbListingNameSnapshot listingNameSnapshot = new AirbnbListingNameSnapshot(listingName, nameLength);
        boolean hasCheckInOption = hasCheckInOptionObject(listingDetails);
        boolean hasCheckInInstruction = hasCheckInInstruction(listingDetails);
        boolean hasCheckInCategory = hasCheckInCategory(listingDetails);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("hotelid", hotelId);
        payload.put("channelhotelid", target.channelHotelId());
        payload.put("listingid", target.listingId());
        payload.put("roomid", target.roomId());
        payload.put("rateid", target.rateId());
        payload.put("name", listingName);
        payload.put("multiplier", modifier.multiplier());
        payload.put("surcharge", modifier.surcharge());
        payload.put("occupancy", parseIntegerOrText(target.occupancy()));

        Map<String, Object> checkInOption = buildCheckInOptionPayload(listingDetails);
        if (checkInOption != null) {
            payload.put("check_in_option", checkInOption);
        }

        Map<String, Object> payloadSummary = buildPayloadSummary(
                "airbnb/listing/update",
                null,
                target,
                payload,
                operationId,
                batchId,
                rowKey,
                nameLength,
                payload.containsKey("check_in_option"),
                hasCheckInOption,
                hasCheckInInstruction
        );
        payloadSummary.put("hasCheckInCategory", hasCheckInCategory);

        if (nameLength > AIRBNB_LISTING_NAME_MAX_CODE_POINTS) {
            throw new MappingUpdateValidationException(
                    AIRBNB_LISTING_NAME_TOO_LONG,
                    "airbnb/listing/update",
                    payloadSummary,
                    buildValidationResponseSummary("airbnb/listing/update", AIRBNB_LISTING_NAME_TOO_LONG, listingResponse),
                    listingNameSnapshot
            );
        }

        return new AirbnbUpdatePayloadResult(payload, payloadSummary, listingNameSnapshot);
    }

    private Map<String, Object> buildPayloadSummary(
            String action,
            ResolvedContext context,
            MappingTarget target,
            Map<String, Object> payload,
            String operationId,
            String batchId,
            String rowKey,
            Integer nameLength,
            Boolean sentCheckInOption,
            Boolean hasCheckInOption,
            Boolean hasCheckInInstruction
    ) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("endpoint", action);
        summary.put("action", action);
        summary.put("operationId", operationId);
        summary.put("batchId", batchId);
        summary.put("rowKey", rowKey);
        if (context != null) {
            summary.put("channelCode", context.channelCode());
            summary.put("suChannelId", context.suChannelId());
            summary.put("suPropertyId", context.hotelId());
        } else if (target != null) {
            summary.put("channelCode", target.channelCode());
            summary.put("suChannelId", target.suChannelId());
            summary.put("suPropertyId", target.suPropertyId());
        }
        if (target != null) {
            summary.put("channelHotelId", target.channelHotelId());
            summary.put("listingid", target.listingId());
            summary.put("roomid", target.roomId());
            summary.put("rateid", target.rateId());
            summary.put("channelroomid", target.channelRoomId());
            summary.put("channelrateid", target.channelRateId());
            summary.put("occupancy", target.occupancy());
            summary.put("applicableNoOfGuest", target.applicableNoOfGuest());
        }
        summary.put("payloadKeys", payload != null ? new ArrayList<>(payload.keySet()) : List.of());
        if (nameLength != null) {
            summary.put("nameLength", nameLength);
        }
        if (sentCheckInOption != null) {
            summary.put("sentCheckInOption", sentCheckInOption);
        }
        if (hasCheckInOption != null) {
            summary.put("hasCheckInOption", hasCheckInOption);
        }
        if (hasCheckInInstruction != null) {
            summary.put("hasCheckInInstruction", hasCheckInInstruction);
        }
        return summary;
    }

    private Map<String, Object> buildResponseSummary(String action, JsonNode response) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("endpoint", action);
        summary.put("action", action);
        if (response == null || response.isNull()) {
            return summary;
        }

        summary.put("httpStatus", suApiClient.extractHttpStatus(response));
        summary.put("status", readStringValue(response, "Status", "status", "Success", "success"));
        summary.put("message", redactAuditText(readStringValue(response, "Message", "message")));
        JsonNode errorsNode = firstExistingNode(response, "Errors", "errors");
        if (errorsNode != null && !errorsNode.isNull()) {
            summary.put("errors", sanitizeJsonForAudit(errorsNode));
        }
        return summary;
    }

    private Map<String, Object> buildValidationResponseSummary(
            String action,
            String message,
            JsonNode retrieveResponse
    ) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("endpoint", action);
        summary.put("action", action);
        summary.put("validationFailure", true);
        summary.put("message", redactAuditText(message));
        if (retrieveResponse != null && !retrieveResponse.isNull()) {
            summary.put("retrieveHttpStatus", suApiClient.extractHttpStatus(retrieveResponse));
            summary.put("retrieveStatus", readStringValue(retrieveResponse, "Status", "status", "Success", "success"));
            JsonNode errorsNode = firstExistingNode(retrieveResponse, "Errors", "errors");
            if (errorsNode != null && !errorsNode.isNull()) {
                summary.put("retrieveErrors", sanitizeJsonForAudit(errorsNode));
            }
        }
        return summary;
    }

    private void applySuAudit(ChannelMappingPriceSetting setting, MappingUpdateResult result) {
        if (setting == null || result == null) {
            return;
        }
        Map<String, Object> responseSummary = sanitizeMapForAudit(result.responseSummary());
        setting.setLastSuAction(result.action());
        setting.setLastSuHttpStatus(asInteger(responseSummary.get("httpStatus")));
        setting.setLastSuResponseStatus(stringValue(responseSummary.get("status")));
        setting.setLastSuResponseMessage(stringValue(responseSummary.get("message")));
        Object errors = responseSummary.get("errors");
        setting.setLastSuResponseErrors(errors != null ? toJsonString(errors) : null);
        setting.setLastSuPayloadSummary(toJsonString(result.payloadSummary()));
        setting.setLastSuResponseSummary(toJsonString(responseSummary));
        if (result.airbnbListingNameSnapshot() != null) {
            setting.setLastAirbnbListingName(result.airbnbListingNameSnapshot().name());
            setting.setLastAirbnbListingNameLength(result.airbnbListingNameSnapshot().length());
        }
    }

    private void logSuRowFailure(
            ResolvedContext context,
            String rowKey,
            String operationId,
            String batchId,
            MappingUpdateResult result
    ) {
        if (result == null) {
            return;
        }
        logger.warn(
                "Su mapping price row failed. action={}, channelCode={}, operationId={}, batchId={}, rowKey={}, responseSummary={}",
                result.action(),
                context.channelCode(),
                operationId,
                batchId,
                rowKey,
                toJsonString(result.responseSummary())
        );
    }

    private String buildUserFacingSuErrorMessage(JsonNode suResponse) {
        String errorMessage = suApiClient.extractSuErrorMessage(suResponse);
        if (requiresAirbnbCheckInInstructions(errorMessage)) {
            return AIRBNB_CHECK_IN_REQUIRED;
        }
        return redactAuditText(errorMessage);
    }

    private boolean requiresAirbnbCheckInInstructions(String errorMessage) {
        if (errorMessage == null) {
            return false;
        }
        String normalized = errorMessage.toLowerCase();
        return normalized.contains("check_in_option")
                || (normalized.contains("check") && normalized.contains("instruction"));
    }

    private Map<String, Object> buildCheckInOptionPayload(JsonNode listingDetails) {
        JsonNode checkInOption = firstExistingNode(listingDetails, "check_in_option", "checkInOption");
        if (checkInOption == null || !checkInOption.isObject()) {
            return null;
        }
        String category = readStringValue(checkInOption, "category", "Category");
        String instruction = readStringValue(checkInOption, "instruction", "Instruction");
        if (category == null || category.trim().isBlank() || instruction == null || instruction.trim().isBlank()) {
            return null;
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("category", category);
        payload.put("instruction", instruction);
        return payload;
    }

    private boolean hasCheckInOptionObject(JsonNode listingDetails) {
        JsonNode checkInOption = firstExistingNode(listingDetails, "check_in_option", "checkInOption");
        return checkInOption != null && checkInOption.isObject();
    }

    private boolean hasCheckInInstruction(JsonNode listingDetails) {
        JsonNode checkInOption = firstExistingNode(listingDetails, "check_in_option", "checkInOption");
        if (checkInOption == null || !checkInOption.isObject()) {
            return false;
        }
        String instruction = readStringValue(checkInOption, "instruction", "Instruction");
        return instruction != null && !instruction.trim().isBlank();
    }

    private boolean hasCheckInCategory(JsonNode listingDetails) {
        JsonNode checkInOption = firstExistingNode(listingDetails, "check_in_option", "checkInOption");
        if (checkInOption == null || !checkInOption.isObject()) {
            return false;
        }
        String category = readStringValue(checkInOption, "category", "Category");
        return category != null && !category.trim().isBlank();
    }

    private List<BookingPricingTarget> buildBookingPricingTargets(
            ResolvedContext context,
            List<JsonNode> pricingNodes,
            String channelHotelId,
            String roomId,
            String rateId,
            String channelRoomId,
            String channelRateId
    ) {
        List<BookingPricingTarget> result = new ArrayList<>();
        for (JsonNode pricingNode : pricingNodes) {
            String applicableGuest = readText(pricingNode, "ApplicableNoOfGuest", "applicablenoofguest");
            String mappingKey = buildMappingKey(
                    context,
                    channelHotelId,
                    roomId,
                    rateId,
                    channelRoomId,
                    channelRateId,
                    null,
                    applicableGuest,
                    null
            );
            result.add(new BookingPricingTarget(
                    encodeRowKey(mappingKey),
                    applicableGuest,
                    readBigDecimal(pricingNode, "Multiplier", "multiplier"),
                    readBigDecimal(pricingNode, "Surcharge", "surcharge")
            ));
        }
        return result;
    }

    private Map<String, Object> buildBookingRatePlanFields(JsonNode ratePlanNode) {
        Map<String, Object> fields = new LinkedHashMap<>();
        Object derivedRateIds = readRawValue(ratePlanNode, "DerivedRateIDs", "DerivedRateIds", "derivedrateids");
        fields.put("derivedrateids", derivedRateIds != null ? derivedRateIds : List.of());
        copyRawField(fields, ratePlanNode, "disablerates", "DisableRates", "disablerates");
        copyRawField(fields, ratePlanNode, "disableavailablity", "DisableAvailablity", "disableavailablity");
        copyRawField(fields, ratePlanNode, "advance_purchase_days", "AdvancePurchaseDays", "advance_purchase_days");
        copyRawField(fields, ratePlanNode, "fixedminstay", "FixedMinStay", "fixedminstay");
        return fields;
    }

    private JsonNode extractAirbnbListingDetails(JsonNode response) {
        JsonNode dataNode = firstExistingNode(response, "Data", "data");
        JsonNode listingNode = firstExistingNode(dataNode, "listing", "Listing");
        if (listingNode != null && listingNode.isObject()) {
            return listingNode;
        }
        JsonNode listingsNode = firstExistingNode(dataNode, "listings", "Listings");
        JsonNode firstListing = firstArrayObject(listingsNode);
        if (firstListing != null) {
            return firstListing;
        }
        if (dataNode != null && dataNode.isObject() && readText(dataNode, "name", "Name") != null) {
            return dataNode;
        }
        listingNode = firstExistingNode(response, "listing", "Listing");
        if (listingNode != null && listingNode.isObject()) {
            return listingNode;
        }
        listingsNode = firstExistingNode(response, "listings", "Listings");
        return firstArrayObject(listingsNode);
    }

    private static JsonNode firstExistingNode(JsonNode node, String... fields) {
        if (node == null || node.isNull() || fields == null) {
            return null;
        }
        for (String field : fields) {
            if (isBlank(field)) {
                continue;
            }
            JsonNode value = node.get(field);
            if (value != null && !value.isNull()) {
                return value;
            }
        }
        return null;
    }

    private static JsonNode firstArrayObject(JsonNode node) {
        if (node == null || !node.isArray()) {
            return null;
        }
        for (JsonNode item : node) {
            if (item != null && item.isObject()) {
                return item;
            }
        }
        return null;
    }

    private static String readStringValue(JsonNode node, String... fields) {
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
            String value = valueNode.asText(null);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private static Object sanitizeJsonForAudit(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isObject()) {
            Map<String, Object> result = new LinkedHashMap<>();
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                if (isSensitiveAuditField(field.getKey())) {
                    result.put(field.getKey(), "[REDACTED]");
                } else {
                    result.put(field.getKey(), sanitizeJsonForAudit(field.getValue()));
                }
            }
            return result;
        }
        if (node.isArray()) {
            List<Object> result = new ArrayList<>();
            int index = 0;
            for (JsonNode item : node) {
                if (index >= 10) {
                    result.add("...");
                    break;
                }
                result.add(sanitizeJsonForAudit(item));
                index++;
            }
            return result;
        }
        if (node.isNumber()) {
            return node.numberValue();
        }
        if (node.isBoolean()) {
            return node.booleanValue();
        }
        return redactAuditText(node.asText());
    }

    private static boolean isSensitiveAuditField(String fieldName) {
        if (fieldName == null) {
            return false;
        }
        String normalized = fieldName.toLowerCase();
        return normalized.contains("authorization")
                || normalized.contains("token")
                || normalized.contains("secret")
                || normalized.contains("password")
                || normalized.contains("address")
                || normalized.contains("street")
                || normalized.contains("location")
                || normalized.contains("latitude")
                || normalized.contains("longitude")
                || normalized.contains("direction")
                || normalized.contains("instruction")
                || normalized.contains("access_code");
    }

    private static Map<String, Object> sanitizeMapForAudit(Map<String, Object> source) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (source == null) {
            return result;
        }
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            String key = entry.getKey();
            if (isSensitiveAuditField(key)) {
                result.put(key, REDACTED_AUDIT_VALUE);
            } else {
                result.put(key, sanitizePlainValueForAudit(entry.getValue()));
            }
        }
        return result;
    }

    private static Object sanitizePlainValueForAudit(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof JsonNode node) {
            return sanitizeJsonForAudit(node);
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String key = String.valueOf(entry.getKey());
                if (isSensitiveAuditField(key)) {
                    result.put(key, REDACTED_AUDIT_VALUE);
                } else {
                    result.put(key, sanitizePlainValueForAudit(entry.getValue()));
                }
            }
            return result;
        }
        if (value instanceof Collection<?> collection) {
            List<Object> result = new ArrayList<>();
            int index = 0;
            for (Object item : collection) {
                if (index >= 10) {
                    result.add("...");
                    break;
                }
                result.add(sanitizePlainValueForAudit(item));
                index++;
            }
            return result;
        }
        if (value instanceof Number || value instanceof Boolean) {
            return value;
        }
        return redactAuditText(String.valueOf(value));
    }

    private static String redactAuditText(String value) {
        if (value == null) {
            return null;
        }
        String redacted = value;
        redacted = AUDIT_SENSITIVE_LABEL_VALUE_PATTERN.matcher(redacted).replaceAll("$1" + REDACTED_AUDIT_VALUE);
        redacted = AUDIT_STREET_ADDRESS_PATTERN.matcher(redacted).replaceAll("[REDACTED_ADDRESS]");
        redacted = AUDIT_ENTRY_INSTRUCTION_SENTENCE_PATTERN.matcher(redacted).replaceAll(" " + REDACTED_AUDIT_VALUE);
        redacted = SmartLockMaskingUtils.redactSensitiveMessage(redacted);
        redacted = AUDIT_URL_PATTERN.matcher(redacted).replaceAll("[REDACTED_URL]");
        redacted = AUDIT_EMAIL_PATTERN.matcher(redacted).replaceAll("[REDACTED_EMAIL]");
        redacted = AUDIT_PHONE_PATTERN.matcher(redacted).replaceAll("[REDACTED_PHONE]");
        redacted = AUDIT_TOKEN_PATTERN.matcher(redacted).replaceAll(REDACTED_AUDIT_VALUE);
        redacted = AUDIT_TOKEN_KEYWORD_PATTERN.matcher(redacted).replaceAll("$1" + REDACTED_AUDIT_VALUE);
        redacted = redacted.replaceAll("[\\r\\n\\t]+", " ");
        redacted = redacted.replaceAll("\\s{2,}", " ").trim();
        return abbreviate(redacted, AUDIT_TEXT_MAX_LENGTH);
    }

    private static String toJsonString(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }

    private static Integer asInteger(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text) {
            try {
                return Integer.parseInt(text);
            } catch (NumberFormatException ignore) {
                return null;
            }
        }
        return null;
    }

    private static String stringValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String text) {
            return abbreviate(text, 1000);
        }
        if (value instanceof Number || value instanceof Boolean) {
            return String.valueOf(value);
        }
        return abbreviate(toJsonString(value), 1000);
    }

    private static String abbreviate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        int safeMaxLength = Math.max(1, maxLength);
        if (value.length() <= safeMaxLength) {
            return value;
        }
        return value.substring(0, safeMaxLength) + "...";
    }

    private static Map<String, Object> jsonObjectToMap(JsonNode node) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (node == null || !node.isObject()) {
            return result;
        }
        Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            result.put(field.getKey(), jsonToPlainValue(field.getValue()));
        }
        return result;
    }

    private static Object readRawValue(JsonNode node, String... fields) {
        JsonNode value = firstExistingNode(node, fields);
        if (value == null) {
            return null;
        }
        return jsonToPlainValue(value);
    }

    private static void copyRawField(
            Map<String, Object> target,
            JsonNode source,
            String payloadField,
            String... sourceFields
    ) {
        Object value = readRawValue(source, sourceFields);
        if (value != null) {
            target.put(payloadField, value);
        }
    }

    private static Object jsonToPlainValue(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isObject()) {
            return jsonObjectToMap(node);
        }
        if (node.isArray()) {
            List<Object> values = new ArrayList<>();
            for (JsonNode item : node) {
                values.add(jsonToPlainValue(item));
            }
            return values;
        }
        if (node.isNumber()) {
            return node.numberValue();
        }
        if (node.isBoolean()) {
            return node.booleanValue();
        }
        return node.asText();
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
                    .findByStoreIdAndChannelIdAndMappingKey(
                            context.storeId(),
                            context.channel().getId(),
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
        setting.setLastError(redactAuditText(message));
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
        dto.setLastAirbnbListingName(setting.getLastAirbnbListingName());
        dto.setLastAirbnbListingNameLength(setting.getLastAirbnbListingNameLength());
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
        String pricingError = validateBookingPricingTargets(target);
        if (pricingError != null) {
            return pricingError;
        }
        return null;
    }

    private String validateBookingPricingTargets(MappingTarget target) {
        List<BookingPricingTarget> pricingTargets = target.bookingPricingTargets();
        if (pricingTargets == null || pricingTargets.isEmpty()) {
            return "Booking 映射缺少 pricing，未同步该条映射";
        }
        for (BookingPricingTarget pricingTarget : pricingTargets) {
            if (pricingTarget == null || isBlank(pricingTarget.applicableNoOfGuest())) {
                return "Booking 映射缺少 applicablenoofguest，未同步该条映射";
            }
            boolean currentGuest = pricingTarget.applicableNoOfGuest().equals(target.applicableNoOfGuest());
            if (!currentGuest && (pricingTarget.multiplier() == null || pricingTarget.surcharge() == null)) {
                return "Booking 映射缺少其它 applicable guest 的现有价格设置，未同步该条映射";
            }
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

    private record MappingUpdateResult(
            String action,
            Map<String, Object> payloadSummary,
            Map<String, Object> responseSummary,
            JsonNode response,
            AirbnbListingNameSnapshot airbnbListingNameSnapshot
    ) {}

    private record AirbnbUpdatePayloadResult(
            Map<String, Object> payload,
            Map<String, Object> payloadSummary,
            AirbnbListingNameSnapshot airbnbListingNameSnapshot
    ) {}

    private record AirbnbListingNameSnapshot(
            String name,
            Integer length
    ) {}

    private static class MappingUpdateValidationException extends RuntimeException {
        private final String action;
        private final Map<String, Object> payloadSummary;
        private final Map<String, Object> responseSummary;
        private final AirbnbListingNameSnapshot airbnbListingNameSnapshot;

        MappingUpdateValidationException(
                String message,
                String action,
                Map<String, Object> payloadSummary,
                Map<String, Object> responseSummary
        ) {
            this(message, action, payloadSummary, responseSummary, null);
        }

        MappingUpdateValidationException(
                String message,
                String action,
                Map<String, Object> payloadSummary,
                Map<String, Object> responseSummary,
                AirbnbListingNameSnapshot airbnbListingNameSnapshot
        ) {
            super(redactAuditText(message));
            this.action = action;
            this.payloadSummary = payloadSummary;
            this.responseSummary = responseSummary;
            this.airbnbListingNameSnapshot = airbnbListingNameSnapshot;
        }

        MappingUpdateResult toUpdateResult() {
            return new MappingUpdateResult(action, payloadSummary, responseSummary, null, airbnbListingNameSnapshot);
        }
    }

    private static class MappingUpdateCallException extends RuntimeException {
        private final String action;
        private final Map<String, Object> payloadSummary;
        private final Map<String, Object> responseSummary;
        private final AirbnbListingNameSnapshot airbnbListingNameSnapshot;

        MappingUpdateCallException(
                String message,
                String action,
                Map<String, Object> payloadSummary,
                Map<String, Object> responseSummary,
                AirbnbListingNameSnapshot airbnbListingNameSnapshot,
                Throwable cause
        ) {
            super(redactAuditText(message), cause);
            this.action = action;
            this.payloadSummary = payloadSummary;
            this.responseSummary = responseSummary != null ? responseSummary : new LinkedHashMap<>();
            this.airbnbListingNameSnapshot = airbnbListingNameSnapshot;
            this.responseSummary.put("endpoint", action);
            this.responseSummary.put("action", action);
            this.responseSummary.put("message", redactAuditText(message));
        }

        MappingUpdateResult toUpdateResult() {
            return new MappingUpdateResult(action, payloadSummary, responseSummary, null, airbnbListingNameSnapshot);
        }
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

    private record BookingPricingTarget(
            String rowKey,
            String applicableNoOfGuest,
            BigDecimal multiplier,
            BigDecimal surcharge
    ) {}

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
            List<BookingPricingTarget> bookingPricingTargets,
            Map<String, Object> bookingRatePlanFields,
            String displayName,
            int mappingIndex,
            int ratePlanIndex,
            int pricingIndex
    ) {}
}
