package server.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.demo.constants.PriceLabsSyncDefaults;
import server.demo.context.StoreContextHolder;
import server.demo.dto.*;
import server.demo.entity.*;
import server.demo.enums.PriceAdjustmentType;
import server.demo.enums.SyncDirection;
import server.demo.enums.SyncStatus;
import server.demo.enums.SyncType;
import server.demo.repository.*;
import server.demo.util.PriceLabsIdUtil;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * PriceLabs 集成服务
 * 处理与 PriceLabs 的所有集成相关业务逻辑
 */
@Service
public class PriceLabsService {

    private static final Logger logger = LoggerFactory.getLogger(PriceLabsService.class);

    private static final int DEFAULT_FALLBACK_DAYS = 365;
    private static final int MAX_FALLBACK_DAYS = 500;

    private static final String OTA_CHANNEL_CODE_AIRBNB = "AIRBNB";
    private static final String OTA_CHANNEL_CODE_BOOKING = "BOOKING";

    @Autowired
    private PriceLabsIntegrationRepository integrationRepository;

    @Autowired
    private PriceLabsConnectionRepository connectionRepository;

    @Autowired
    private ChannelPriceRepository channelPriceRepository;

    @Autowired
    private PriceLabsSyncLogRepository syncLogRepository;

    @Autowired
    private ChannelRepository channelRepository;

    @Autowired
    private RoomTypeRepository roomTypeRepository;

    @Autowired
    private PricePlanRepository pricePlanRepository;

    @Autowired
    private RoomTypePricePlanRepository roomTypePricePlanRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private RoomPriceRepository roomPriceRepository;

    @Autowired
    private PriceChangeHistoryRepository priceChangeHistoryRepository;

    @Autowired
    private PriceLabsApiClient priceLabsApiClient;

    @Autowired(required = false)
    private SuAriAutoSyncService suAriAutoSyncService;

    @Autowired
    private server.demo.config.PriceLabsConfig priceLabsConfig;

    @Autowired
    private ChannelPriceFallbackService channelPriceFallbackService;

    @Autowired
    private PriceLabsSyncService priceLabsSyncService;

    // ==================== 集成配置管理 ====================

    /**
     * 获取当前门店的 PriceLabs 集成配置
     */
    public PriceLabsIntegrationDTO getIntegration() {
        Long storeId = StoreContextHolder.getContext().getStoreId();
        return integrationRepository.findByStoreId(storeId)
                .map(this::convertToIntegrationDTO)
                .orElse(createDefaultIntegrationDTO(storeId));
    }

    /**
     * 保存或更新集成配置
     */
    @Transactional
    public PriceLabsIntegrationDTO saveIntegration(PriceLabsIntegrationDTO dto) {
        Long storeId = StoreContextHolder.getContext().getStoreId();

        PriceLabsIntegration integration = integrationRepository.findByStoreId(storeId)
                .orElse(new PriceLabsIntegration(storeId));

        integration.setIsEnabled(dto.getIsEnabled());
        integration.setSyncUrl(dto.getSyncUrl());
        integration.setCalendarTriggerUrl(dto.getCalendarTriggerUrl());
        integration.setHookUrl(dto.getHookUrl());

        PriceLabsIntegration saved = integrationRepository.save(integration);
        return convertToIntegrationDTO(saved);
    }

    /**
     * 启用/禁用集成
     */
    @Transactional
    public PriceLabsIntegrationDTO toggleIntegration(Boolean enabled) {
        Long storeId = StoreContextHolder.getContext().getStoreId();

        PriceLabsIntegration integration = integrationRepository.findByStoreId(storeId)
                .orElse(new PriceLabsIntegration(storeId));

        if (enabled) {
            // 启用集成时，调用 PriceLabs API 注册集成
            try {
                // 调用 PriceLabs API 注册集成
                priceLabsApiClient.registerIntegration();

                // 保存 webhook URL 到数据库
                integration.setSyncUrl(priceLabsConfig.getSyncUrl());
                integration.setCalendarTriggerUrl(priceLabsConfig.getCalendarTriggerUrl());
                integration.setHookUrl(priceLabsConfig.getHookUrl());
                integration.setIsEnabled(true);

                // 记录同步日志
                PriceLabsSyncLog log = PriceLabsSyncLog.success(
                    storeId,
                    SyncType.LISTING,
                    SyncDirection.OUTBOUND,
                    0
                );
                log.setRequestData("PriceLabs 集成注册");
                syncLogRepository.save(log);

            } catch (Exception e) {
                // 注册失败，记录错误日志
                PriceLabsSyncLog log = PriceLabsSyncLog.failure(
                    storeId,
                    SyncType.LISTING,
                    SyncDirection.OUTBOUND,
                    "集成注册失败: " + e.getMessage()
                );
                syncLogRepository.save(log);
                throw new RuntimeException("PriceLabs 集成注册失败: " + e.getMessage(), e);
            }
        } else {
            // 禁用集成
            integration.setIsEnabled(false);
        }

        PriceLabsIntegration saved = integrationRepository.save(integration);
        return convertToIntegrationDTO(saved);
    }

    /**
     * 更新集成配置
     */
    @Transactional
    public PriceLabsIntegrationDTO updateIntegrationConfig(String priceLabsEmail) {
        Long storeId = StoreContextHolder.getContext().getStoreId();

        PriceLabsIntegration integration = integrationRepository.findByStoreId(storeId)
                .orElse(new PriceLabsIntegration(storeId));

        integration.setPriceLabsEmail(priceLabsEmail);
        PriceLabsIntegration saved = integrationRepository.save(integration);
        return convertToIntegrationDTO(saved);
    }

    // ==================== 连接配置管理 ====================

    /**
     * 获取当前门店的所有连接配置
     */
    public List<PriceLabsConnectionDTO> getConnections() {
        Long storeId = StoreContextHolder.getContext().getStoreId();
        return connectionRepository.findByStoreId(storeId).stream()
                .map(this::convertToConnectionDTO)
                .collect(Collectors.toList());
    }

    /**
     * 创建连接配置
     */
    @Transactional
    public PriceLabsConnectionDTO createConnection(Long roomTypeId, Long pricePlanId) {
        Long storeId = StoreContextHolder.getContext().getStoreId();

        if (roomTypeId == null) {
            throw new RuntimeException("房型不能为空");
        }
        if (pricePlanId == null) {
            throw new RuntimeException("价格计划不能为空");
        }

        // 稳定优先：同一房型仅允许存在一个“启用”的连接，避免 webhook 推价（缺少 rate_plan_id）产生歧义
        connectionRepository.findByStoreIdAndRoomTypeIdAndIsEnabledTrue(storeId, roomTypeId).ifPresent(existing -> {
            String planName = null;
            try {
                if (existing.getPricePlan() != null) {
                    planName = existing.getPricePlan().getName();
                }
            } catch (Exception ignored) {
                // ignore lazy load errors for message building
            }
            if (planName == null || planName.isBlank()) {
                throw new RuntimeException("该房型已存在启用的 PriceLabs 连接，请先禁用或删除后再添加");
            }
            throw new RuntimeException("该房型已绑定价格计划：" + planName + "，请先禁用或删除后再添加");
        });

        // 检查是否已存在
        if (connectionRepository.findByStoreIdAndRoomTypeIdAndPricePlanId(storeId, roomTypeId, pricePlanId).isPresent()) {
            throw new RuntimeException("该房型与价格计划的连接已存在");
        }

        RoomType roomType = roomTypeRepository.findByStoreIdAndId(storeId, roomTypeId)
                .orElseThrow(() -> new RuntimeException("房型不存在"));
        PricePlan pricePlan = pricePlanRepository.findByStoreIdAndId(storeId, pricePlanId)
                .orElseThrow(() -> new RuntimeException("价格计划不存在"));

        Integer totalRooms = roomType.getTotalRooms();
        if (totalRooms == null || totalRooms <= 0) {
            throw new RuntimeException("房型总房量必须大于 0，请先检查房型设置后再创建连接");
        }
        int roomCount = roomRepository.findByStoreIdAndRoomTypeId(storeId, roomTypeId).size();
        if (roomCount > 0 && totalRooms < roomCount) {
            throw new RuntimeException("房型总房量小于房间列表数量，请先调整房型总房量后再创建连接");
        }
        boolean isBoundInPricePlanSettings = roomTypePricePlanRepository
                .existsByStoreIdAndRoomTypeIdAndPricePlanId(storeId, roomTypeId, pricePlanId);
        if (!isBoundInPricePlanSettings) {
            throw new RuntimeException("请先在价格计划页面完成房型绑定后再创建连接");
        }
        PriceLabsConnection connection = new PriceLabsConnection(roomType, pricePlan);
        connection.setStoreId(storeId);
        connection.generatePriceLabsListingId();

        PriceLabsConnection saved = connectionRepository.save(connection);
        priceLabsSyncService.syncListingRatePlanAndCalendar(
                storeId,
                roomType,
                pricePlan,
                PriceLabsSyncDefaults.DEFAULT_SYNC_DAYS
        );
        return convertToConnectionDTO(saved);
    }

    /**
     * 更新连接状态
     */
    @Transactional
    public Optional<PriceLabsConnectionDTO> updateConnectionStatus(Long connectionId, Boolean enabled) {
        Long storeId = StoreContextHolder.getContext().getStoreId();

        return connectionRepository.findById(connectionId)
                .filter(conn -> conn.getStoreId().equals(storeId))
                .map(conn -> {
                    // 稳定优先：启用连接前，确保同一房型没有其它启用连接
                    if (Boolean.TRUE.equals(enabled) && conn.getRoomType() != null && conn.getRoomType().getId() != null) {
                        connectionRepository.findByStoreIdAndRoomTypeIdAndIsEnabledTrue(storeId, conn.getRoomType().getId())
                                .ifPresent(existing -> {
                                    if (existing.getId() != null && !existing.getId().equals(conn.getId())) {
                                        String planName = null;
                                        try {
                                            if (existing.getPricePlan() != null) {
                                                planName = existing.getPricePlan().getName();
                                            }
                                        } catch (Exception ignored) {
                                            // ignore
                                        }
                                        if (planName == null || planName.isBlank()) {
                                            throw new RuntimeException("该房型已存在其它启用的 PriceLabs 连接，请先禁用后再启用");
                                        }
                                        throw new RuntimeException("该房型已绑定价格计划：" + planName + "，请先禁用后再启用");
                                    }
                                });
                    }
                    conn.setIsEnabled(enabled);
                    return convertToConnectionDTO(connectionRepository.save(conn));
                });
    }

    /**
     * 删除连接配置
     */
    @Transactional
    public boolean deleteConnection(Long connectionId) {
        Long storeId = StoreContextHolder.getContext().getStoreId();

        return connectionRepository.findById(connectionId)
                .filter(conn -> conn.getStoreId().equals(storeId))
                .map(conn -> {
                    connectionRepository.delete(conn);
                    return true;
                })
                .orElse(false);
    }

    // ==================== 价格 Webhook 处理 ====================

    /**
     * 处理 PriceLabs 推送的价格更新
     * 这是 PriceLabs 调用的 webhook 端点
     */
    @Transactional
    public void handleWebhookPriceUpdate(PriceLabsWebhookRequest request) {
        if (request.getData() == null || request.getData().isEmpty()) {
            throw new RuntimeException("Webhook 数据为空");
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        int totalAffectedCount = 0;
        Long storeId = null;
        Set<Long> affectedStoreIds = new HashSet<>();

        // 遍历所有房源数据
        for (PriceLabsWebhookRequest.ListingData listingData : request.getData()) {
            String listingId = listingData.getListingId();
            String ratePlanId = listingData.getRatePlanId();

            if (listingId == null || listingId.isBlank()) {
                throw new RuntimeException("listingId 为空");
            }

            Long roomTypeId = PriceLabsIdUtil.parseRoomTypeId(listingId)
                    .orElseThrow(() -> new RuntimeException("listingId 格式不正确: " + listingId));

            PriceLabsConnection connection = resolveConnectionForWebhook(roomTypeId, listingId, ratePlanId);

            storeId = connection.getStoreId();
            if (storeId != null) {
                affectedStoreIds.add(storeId);
            }
            RoomType roomType = connection.getRoomType();
            Set<LocalDate> affectedDates = new TreeSet<>();
            boolean hasRestrictionUpdate = false;
            PricePlan pricePlan = connection.getPricePlan(); // 获取价格计划

            // 仅同步到 SU 直连的 OTA 渠道（Airbnb/Booking）
            List<Channel> channels = new ArrayList<>(2);
            channels.addAll(channelRepository.findByStoreId(storeId).stream()
                    .filter(PriceLabsService::isChannelAutoSyncEnabled)
                    .collect(Collectors.toList()));

            if (channels.isEmpty()) {
                logger.warn("[PriceLabsWebhook] no enabled+autoSyncPrice channels for storeId={}, will update room_prices only", storeId);
            }

            // 处理日历数据
            if (listingData.getCalendar() != null) {
                for (PriceLabsWebhookRequest.CalendarData calendarData : listingData.getCalendar()) {
                    LocalDate priceDate = LocalDate.parse(calendarData.getDate(), formatter);
                    BigDecimal basePrice = calendarData.getPrice();

                    if (basePrice == null) {
                        continue;
                    }

                    affectedDates.add(priceDate);

                    // 写入 room_prices（房型 + 价格计划 + 日期 的基础房价），保证 PMS 房价管理可见
                    Optional<RoomPrice> existingRpOpt = roomPriceRepository
                            .findByRoomTypeIdAndPricePlanIdAndPriceDate(roomType.getId(), pricePlan.getId(), priceDate);
                    BigDecimal previousPrice = existingRpOpt.map(RoomPrice::getPrice).orElse(null);

                    RoomPrice rp = existingRpOpt
                            .orElse(new RoomPrice(roomType, pricePlan, priceDate, basePrice));
                    boolean skipPriceOverrideByManual = shouldSkipPriceOverrideByManual(rp);
                    if (skipPriceOverrideByManual) {
                        logger.info(
                                "[PriceLabsWebhook] skip price override due to manual override. storeId={}, roomTypeId={}, pricePlanId={}, date={}",
                                storeId,
                                roomType.getId(),
                                pricePlan.getId(),
                                priceDate
                        );
                    } else {
                        rp.setPrice(basePrice);
                        rp.setPriceSource(RoomPrice.PRICE_SOURCE_PRICELABS);
                        rp.setManualOverride(Boolean.FALSE);
                        rp.setManualOverrideUntil(null);
                    }
                    Integer minStay = normalizeMinStay(calendarData.getMinStay());
                    if (minStay != null) {
                        rp.setMinStay(minStay);
                        hasRestrictionUpdate = true;
                    }
                    if (calendarData.getMaxStay() != null) {
                        rp.setMaxStay(calendarData.getMaxStay());
                        hasRestrictionUpdate = true;
                    }
                    roomPriceRepository.save(rp);

                    // 写入改价记录：每个日期一条，仅当价格确实变化时才记录
                    BigDecimal effectiveBasePrice = resolveEffectiveBasePrice(rp, basePrice);
                    boolean isPriceChanged = previousPrice == null || previousPrice.compareTo(effectiveBasePrice) != 0;
                    if (isPriceChanged) {
                        PriceChangeHistory history = new PriceChangeHistory();
                        history.setRoomType(roomType);
                        history.setPricePlan(pricePlan);
                        history.setPriceDateStart(priceDate);
                        history.setPriceDateEnd(priceDate);
                        history.setApplyWeekdays("特定日期");
                        history.setChangeType("价格");
                        history.setChangeValue(effectiveBasePrice);
                        history.setPreviousValue(previousPrice);
                        history.setOperator("系统");
                        history.setStoreId(storeId);
                        history.setOperateTime(LocalDateTime.now());
                        history.setNotes("PriceLabs同步");
                        priceChangeHistoryRepository.save(history);
                    }

                    // 为每个渠道计算并保存价格
                    for (Channel channel : channels) {
                        BigDecimal channelPrice = channel.calculateChannelPrice(effectiveBasePrice);

                        // 查找或创建渠道价格记录（门店级隔离 + 价格计划隔离）
                        ChannelPrice cp = channelPriceRepository
                                .findByStoreIdAndRoomTypeIdAndPricePlanIdAndChannelIdAndPriceDate(
                                    storeId, roomType.getId(), pricePlan.getId(), channel.getId(), priceDate)
                                .orElse(new ChannelPrice());

                        cp.setStoreId(storeId);
                        cp.setRoomType(roomType);
                        cp.setPricePlan(pricePlan); // 设置价格计划
                        cp.setChannel(channel);
                        cp.setPriceDate(priceDate);
                        cp.setBasePrice(effectiveBasePrice);
                        cp.setChannelPrice(channelPrice);

                        // 设置最小/最大入住天数
                        Integer channelMinStay = normalizeMinStay(calendarData.getMinStay());
                        if (channelMinStay != null) {
                            cp.setMinStay(channelMinStay);
                        }
                        if (calendarData.getMaxStay() != null) {
                            cp.setMaxStay(calendarData.getMaxStay());
                        }

                        cp.setPriceLabsUpdatedAt(LocalDateTime.now());
                        cp.setIsSyncedToOta(false); // 标记需要同步到 OTA

                        channelPriceRepository.save(cp);
                        totalAffectedCount++;
                    }
                }
            }

            if (suAriAutoSyncService != null && storeId != null && !affectedDates.isEmpty()) {
                List<SuAriAutoSyncService.DateRange> ranges = new ArrayList<>();
                LocalDate rangeStart = null;
                LocalDate prev = null;
                for (LocalDate d : affectedDates) {
                    if (rangeStart == null) {
                        rangeStart = d;
                        prev = d;
                    } else if (prev != null && d.equals(prev.plusDays(1))) {
                        prev = d;
                    } else {
                        ranges.add(new SuAriAutoSyncService.DateRange(rangeStart, prev));
                        rangeStart = d;
                        prev = d;
                    }
                }
                if (rangeStart != null && prev != null) {
                    ranges.add(new SuAriAutoSyncService.DateRange(rangeStart, prev));
                }

                suAriAutoSyncService.enqueueForStoreDateRanges(
                        storeId,
                        "pricelabs_webhook",
                        ranges,
                        Set.of(roomType.getId()),
                        Set.of(pricePlan.getId()),
                        false,
                        true,
                        hasRestrictionUpdate,
                        false
                );
            }

            // 更新连接状态
            connection.setLastSyncAt(LocalDateTime.now());
            connection.setSyncStatus("connected");
            connectionRepository.save(connection);
        }

        // 记录同步日志
        if (storeId != null) {
            PriceLabsSyncLog log = PriceLabsSyncLog.success(
                storeId,
                SyncType.PRICE_UPDATE,
                SyncDirection.INBOUND,
                totalAffectedCount
            );
            log.setRequestData("type: " + request.getType() + ", listings: " + request.getData().size());
            syncLogRepository.save(log);

            // 更新集成的最后价格同步时间
            integrationRepository.findByStoreId(storeId).ifPresent(integration -> {
                integration.setLastPriceSyncAt(LocalDateTime.now());
                integrationRepository.save(integration);
            });
        }

        // SU 推送已在 listing 维度按严格日期范围入队（避免发送多余日期/房型/计划）
    }

    private PriceLabsConnection resolveConnectionForWebhook(Long roomTypeId, String listingId, String ratePlanId) {
        Optional<Long> parsedStoreIdOpt = PriceLabsIdUtil.parseStoreId(listingId);

        if (ratePlanId != null && !ratePlanId.isBlank()) {
            Long pricePlanId = PriceLabsIdUtil.parsePricePlanId(ratePlanId)
                    .orElseThrow(() -> new RuntimeException("ratePlanId 格式不正确: " + ratePlanId));

            PriceLabsConnection conn = connectionRepository.findByRoomTypeIdAndPricePlanId(roomTypeId, pricePlanId)
                    .orElseThrow(() -> new RuntimeException("未找到对应的连接配置: listingId=" + listingId + ", ratePlanId=" + ratePlanId));

            if (Boolean.FALSE.equals(conn.getIsEnabled())) {
                throw new RuntimeException("连接已禁用: listingId=" + listingId + ", ratePlanId=" + ratePlanId);
            }

            parsedStoreIdOpt.ifPresent(parsedStoreId -> {
                if (!parsedStoreId.equals(conn.getStoreId())) {
                    throw new RuntimeException("listingId storeId 与连接不匹配: listingId=" + listingId + ", connStoreId=" + conn.getStoreId());
                }
            });

            return conn;
        }

        List<PriceLabsConnection> candidates = connectionRepository.findByRoomTypeId(roomTypeId).stream()
                .filter(conn -> !Boolean.FALSE.equals(conn.getIsEnabled()))
                .filter(conn -> parsedStoreIdOpt.map(id -> id.equals(conn.getStoreId())).orElse(true))
                .collect(Collectors.toList());

        if (candidates.isEmpty()) {
            throw new RuntimeException("未找到对应的连接配置: listingId=" + listingId + ", ratePlanId 缺失");
        }

        if (candidates.size() == 1) {
            return candidates.get(0);
        }

        Long defaultPlanId = roomTypePricePlanRepository.findByRoomTypeId(roomTypeId).stream()
                .filter(rtp -> rtp.getPricePlan() != null && rtp.getPricePlan().getId() != null)
                .min((a, b) -> {
                    if (a.getId() == null && b.getId() == null) return 0;
                    if (a.getId() == null) return 1;
                    if (b.getId() == null) return -1;
                    return a.getId().compareTo(b.getId());
                })
                .map(rtp -> rtp.getPricePlan().getId())
                .orElse(null);

        if (defaultPlanId != null) {
            for (PriceLabsConnection c : candidates) {
                if (c.getPricePlan() != null && defaultPlanId.equals(c.getPricePlan().getId())) {
                    logger.warn("[PriceLabsWebhook] ratePlanId missing; resolved by default price plan. listingId={}, roomTypeId={}, pricePlanId={}", listingId, roomTypeId, defaultPlanId);
                    return c;
                }
            }
        }

        PriceLabsConnection fallback = candidates.get(0);
        logger.warn("[PriceLabsWebhook] ratePlanId missing and multiple connections found; fallback to first. listingId={}, roomTypeId={}, candidates={}", listingId, roomTypeId, candidates.size());
        return fallback;
    }

    private static boolean isChannelAutoSyncEnabled(Channel channel) {
        if (channel == null) {
            return false;
        }
        // 兼容历史数据：enabled/auto_sync_price 可能为 NULL，默认按“启用”处理
        return !Boolean.FALSE.equals(channel.getEnabled()) && !Boolean.FALSE.equals(channel.getAutoSyncPrice());
    }

    private static boolean shouldSkipPriceOverrideByManual(RoomPrice roomPrice) {
        if (roomPrice == null || !Boolean.TRUE.equals(roomPrice.getManualOverride())) {
            return false;
        }
        LocalDate until = roomPrice.getManualOverrideUntil();
        if (until == null) {
            return true;
        }
        return !LocalDate.now().isAfter(until);
    }

    private static BigDecimal resolveEffectiveBasePrice(RoomPrice roomPrice, BigDecimal incomingBasePrice) {
        if (shouldSkipPriceOverrideByManual(roomPrice) && roomPrice != null && roomPrice.getPrice() != null) {
            return roomPrice.getPrice();
        }
        return incomingBasePrice;
    }

    private static Integer normalizeMinStay(Integer minStay) {
        if (minStay == null) {
            return null;
        }
        return minStay > 0 ? minStay : 1;
    }

    // ==================== 渠道价格调整管理 ====================

    /**
     * 获取所有渠道的价格调整设置
     */
    public List<ChannelPriceAdjustmentDTO> getChannelPriceAdjustments() {
        Long storeId = StoreContextHolder.getContext().getStoreId();

        return channelRepository.findByStoreId(storeId).stream()
                .filter(ch -> Boolean.TRUE.equals(ch.getEnabled()))
                .map(this::convertToAdjustmentDTO)
                .collect(Collectors.toList());
    }

    /**
     * 更新渠道的价格调整设置
     */
    @Transactional
    public ChannelPriceAdjustmentDTO updateChannelPriceAdjustment(Long channelId,
            PriceAdjustmentType adjustmentType, BigDecimal adjustmentValue, Boolean autoSyncPrice) {
        Long storeId = StoreContextHolder.getContext().getStoreId();

        Channel channel = channelRepository.findById(channelId)
                .filter(ch -> ch.getStoreId().equals(storeId))
                .orElseThrow(() -> new RuntimeException("渠道不存在"));

        channel.setPriceAdjustmentType(adjustmentType);
        channel.setPriceAdjustmentValue(adjustmentValue);
        if (autoSyncPrice != null) {
            channel.setAutoSyncPrice(autoSyncPrice);
        }

        Channel saved = channelRepository.save(channel);

        // 生产环境自动兜底：调整比例后，确保未来一段时间的渠道价存在且已按最新比例重算
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(DEFAULT_FALLBACK_DAYS - 1L);
        recalculateChannelPrices(saved.getId(), startDate, endDate);

        return convertToAdjustmentDTO(saved);
    }

    /**
     * 批量更新渠道价格调整
     */
    @Transactional
    public List<ChannelPriceAdjustmentDTO> batchUpdateChannelPriceAdjustments(
            List<ChannelPriceAdjustmentDTO> adjustments) {
        Long storeId = StoreContextHolder.getContext().getStoreId();
        List<ChannelPriceAdjustmentDTO> results = new ArrayList<>();
        List<Long> updatedChannelIds = new ArrayList<>();

        for (ChannelPriceAdjustmentDTO dto : adjustments) {
            channelRepository.findById(dto.getChannelId())
                    .filter(ch -> ch.getStoreId().equals(storeId))
                    .ifPresent(channel -> {
                        channel.setPriceAdjustmentType(dto.getAdjustmentType());
                        channel.setPriceAdjustmentValue(dto.getAdjustmentValue());
                        if (dto.getAutoSyncPrice() != null) {
                            channel.setAutoSyncPrice(dto.getAutoSyncPrice());
                        }
                        Channel saved = channelRepository.save(channel);
                        results.add(convertToAdjustmentDTO(saved));
                        updatedChannelIds.add(saved.getId());
                    });
        }

        if (!updatedChannelIds.isEmpty()) {
            LocalDate startDate = LocalDate.now();
            LocalDate endDate = startDate.plusDays(DEFAULT_FALLBACK_DAYS - 1L);
            for (Long id : updatedChannelIds) {
                recalculateChannelPrices(id, startDate, endDate);
            }
        }

        return results;
    }

    /**
     * 重新计算所有渠道价格（当调整设置改变后）
     */
    @Transactional
    public int recalculateChannelPrices(Long channelId, LocalDate startDate, LocalDate endDate) {
        Long storeId = StoreContextHolder.getContext().getStoreId();

        Channel channel = channelRepository.findById(channelId)
                .filter(ch -> ch.getStoreId().equals(storeId))
                .orElseThrow(() -> new RuntimeException("渠道不存在"));

        int days = clampDays(startDate, endDate);
        channelPriceFallbackService.generate(storeId, startDate, days, null, List.of(channelId));

        List<ChannelPrice> prices = channelPriceRepository
                .findByStoreIdAndChannelIdAndPriceDateBetween(storeId, channelId, startDate, endDate);

        int count = 0;
        for (ChannelPrice cp : prices) {
            if (cp.getBasePrice() != null) {
                BigDecimal newChannelPrice = channel.calculateChannelPrice(cp.getBasePrice());
                cp.setChannelPrice(newChannelPrice);
                cp.setIsSyncedToOta(false); // 标记需要重新同步
                channelPriceRepository.save(cp);
                count++;
            }
        }

        return count;
    }

    // ==================== 渠道价格查询 ====================

    /**
     * 获取指定日期范围的渠道价格
     */
    public List<ChannelPriceDTO> getChannelPrices(Long roomTypeId, Long channelId,
            LocalDate startDate, LocalDate endDate) {
        Long storeId = StoreContextHolder.getContext().getStoreId();

        // 方案A（最小改动）：查询接口尽量只读。
        // 仅当该区间内完全没有任何 channel_prices 时才触发兜底生成，避免与 PriceLabs 大批量回推并发写入导致死锁。
        boolean hasAnyChannelPricesInRange = false;
        if (startDate != null && endDate != null) {
            hasAnyChannelPricesInRange = channelPriceRepository.existsByStoreIdAndPriceDateBetween(storeId, startDate, endDate);
        }
        if (!hasAnyChannelPricesInRange) {
            try {
                int days = clampDays(startDate, endDate);
                channelPriceFallbackService.generate(
                        storeId,
                        startDate,
                        days,
                        null,
                        channelId != null ? List.of(channelId) : null
                );
            } catch (Exception ex) {
                if (isDeadlockException(ex)) {
                    logger.warn("[ChannelPriceQuery] fallback generate skipped due to deadlock. storeId={}, startDate={}, endDate={}, message={}",
                            storeId, startDate, endDate, ex.getMessage());
                } else {
                    throw ex;
                }
            }
        }

        List<ChannelPrice> prices;
        if (roomTypeId != null && channelId != null) {
            prices = channelPriceRepository.findByStoreAndRoomTypeAndChannelAndDateRange(
                    storeId, roomTypeId, channelId, startDate, endDate);
        } else if (roomTypeId != null) {
            prices = channelPriceRepository.findByStoreIdAndRoomTypeIdAndPriceDateBetween(
                    storeId, roomTypeId, startDate, endDate);
        } else if (channelId != null) {
            prices = channelPriceRepository.findByStoreIdAndChannelIdAndPriceDateBetween(
                    storeId, channelId, startDate, endDate);
        } else {
            prices = channelPriceRepository.findByStoreIdAndDateRange(storeId, startDate, endDate);
        }

        return prices.stream()
                .map(this::convertToChannelPriceDTO)
                .collect(Collectors.toList());
    }

    private static boolean isDeadlockException(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof SQLException sqlException) {
                if (sqlException.getErrorCode() == 1213) { // MySQL ER_LOCK_DEADLOCK
                    return true;
                }
                String sqlState = sqlException.getSQLState();
                if (sqlState != null && sqlState.equalsIgnoreCase("40001")) { // serialization failure / deadlock
                    return true;
                }
            }
            String message = current.getMessage();
            if (message != null && message.toLowerCase().contains("deadlock found")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private static int clampDays(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            return DEFAULT_FALLBACK_DAYS;
        }
        long diff = ChronoUnit.DAYS.between(startDate, endDate) + 1L;
        if (diff < 1) {
            return 1;
        }
        return (int) Math.min(diff, MAX_FALLBACK_DAYS);
    }

    /**
     * 获取未同步到 OTA 的价格
     */
    public List<ChannelPriceDTO> getUnsyncedPrices(Long channelId) {
        Long storeId = StoreContextHolder.getContext().getStoreId();

        List<ChannelPrice> prices;
        if (channelId != null) {
            prices = channelPriceRepository.findByStoreIdAndChannelIdAndIsSyncedToOtaFalse(storeId, channelId);
        } else {
            prices = channelPriceRepository.findByStoreIdAndIsSyncedToOtaFalse(storeId);
        }

        return prices.stream()
                .map(this::convertToChannelPriceDTO)
                .collect(Collectors.toList());
    }

    // ==================== 同步日志管理 ====================

    /**
     * 获取同步日志（分页）
     */
    public Page<PriceLabsSyncLogDTO> getSyncLogs(int page, int size) {
        Long storeId = StoreContextHolder.getContext().getStoreId();
        Pageable pageable = PageRequest.of(page, size);

        return syncLogRepository.findByStoreIdOrderByCreatedAtDesc(storeId, pageable)
                .map(this::convertToSyncLogDTO);
    }

    /**
     * 获取最近的同步日志
     */
    public List<PriceLabsSyncLogDTO> getRecentSyncLogs(int limit) {
        Long storeId = StoreContextHolder.getContext().getStoreId();
        Pageable pageable = PageRequest.of(0, limit);

        return syncLogRepository.findRecentLogs(storeId, pageable).stream()
                .map(this::convertToSyncLogDTO)
                .collect(Collectors.toList());
    }

    // ==================== DTO 转换方法 ====================

    private PriceLabsIntegrationDTO convertToIntegrationDTO(PriceLabsIntegration entity) {
        PriceLabsIntegrationDTO dto = new PriceLabsIntegrationDTO();
        dto.setId(entity.getId());
        dto.setStoreId(entity.getStoreId());
        dto.setIsEnabled(entity.getIsEnabled());
        dto.setPriceLabsEmail(entity.getPriceLabsEmail());
        dto.setSyncUrl(entity.getSyncUrl());
        dto.setCalendarTriggerUrl(entity.getCalendarTriggerUrl());
        dto.setHookUrl(entity.getHookUrl());
        dto.setLastListingSyncAt(entity.getLastListingSyncAt());
        dto.setLastPriceSyncAt(entity.getLastPriceSyncAt());
        dto.setLastReservationSyncAt(entity.getLastReservationSyncAt());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());

        // 附加统计信息
        dto.setConnectedRoomTypeCount(connectionRepository.countConnectedRoomTypes(entity.getStoreId()));
        dto.setTotalSyncCount(syncLogRepository.countByStoreId(entity.getStoreId()));
        dto.setSuccessSyncCount(syncLogRepository.countByStoreIdAndStatus(entity.getStoreId(), SyncStatus.SUCCESS));

        return dto;
    }

    private PriceLabsIntegrationDTO createDefaultIntegrationDTO(Long storeId) {
        PriceLabsIntegrationDTO dto = new PriceLabsIntegrationDTO();
        dto.setStoreId(storeId);
        dto.setIsEnabled(false);
        dto.setConnectedRoomTypeCount(0L);
        dto.setTotalSyncCount(0L);
        dto.setSuccessSyncCount(0L);
        return dto;
    }

    private PriceLabsConnectionDTO convertToConnectionDTO(PriceLabsConnection entity) {
        PriceLabsConnectionDTO dto = new PriceLabsConnectionDTO();
        dto.setId(entity.getId());
        dto.setStoreId(entity.getStoreId());
        dto.setRoomTypeId(entity.getRoomType().getId());
        dto.setRoomTypeName(entity.getRoomType().getName());
        dto.setPricePlanId(entity.getPricePlan().getId());
        dto.setPricePlanName(entity.getPricePlan().getName());
        dto.setPriceLabsListingId(entity.getPriceLabsListingId());
        dto.setIsEnabled(entity.getIsEnabled());
        dto.setLastSyncAt(entity.getLastSyncAt());
        dto.setSyncStatus(entity.getSyncStatus());
        dto.setErrorMessage(entity.getErrorMessage());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }

    private ChannelPriceDTO convertToChannelPriceDTO(ChannelPrice entity) {
        ChannelPriceDTO dto = new ChannelPriceDTO();
        dto.setId(entity.getId());
        dto.setStoreId(entity.getStoreId());
        dto.setRoomTypeId(entity.getRoomType().getId());
        dto.setRoomTypeName(entity.getRoomType().getName());
        dto.setPricePlanId(entity.getPricePlan().getId());
        dto.setPricePlanName(entity.getPricePlan().getName());
        dto.setChannelId(entity.getChannel().getId());
        dto.setChannelName(entity.getChannel().getName());
        dto.setChannelCode(entity.getChannel().getCode());
        dto.setPriceDate(entity.getPriceDate());
        dto.setBasePrice(entity.getBasePrice());
        dto.setChannelPrice(entity.getChannelPrice());
        dto.setMinStay(entity.getMinStay());
        dto.setMaxStay(entity.getMaxStay());
        dto.setIsSyncedToOta(entity.getIsSyncedToOta());
        dto.setOtaSyncAt(entity.getOtaSyncAt());
        dto.setPriceLabsUpdatedAt(entity.getPriceLabsUpdatedAt());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }

    private PriceLabsSyncLogDTO convertToSyncLogDTO(PriceLabsSyncLog entity) {
        PriceLabsSyncLogDTO dto = new PriceLabsSyncLogDTO();
        dto.setId(entity.getId());
        dto.setStoreId(entity.getStoreId());
        dto.setSyncType(entity.getSyncType());
        dto.setDirection(entity.getDirection());
        dto.setStatus(entity.getStatus());
        dto.setAffectedCount(entity.getAffectedCount());
        dto.setErrorMessage(entity.getErrorMessage());
        dto.setRequestData(entity.getRequestData());
        dto.setResponseData(entity.getResponseData());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }

    private ChannelPriceAdjustmentDTO convertToAdjustmentDTO(Channel channel) {
        ChannelPriceAdjustmentDTO dto = new ChannelPriceAdjustmentDTO();
        dto.setChannelId(channel.getId());
        dto.setChannelName(channel.getName());
        dto.setChannelCode(channel.getCode());

        // 处理可能为 null 的字段，提供默认值
        PriceAdjustmentType adjustmentType = channel.getPriceAdjustmentType();
        if (adjustmentType == null) {
            // 如果有佣金率，默认使用佣金模式；否则使用百分比模式
            if (channel.getCommissionRate() != null && channel.getCommissionRate() > 0) {
                adjustmentType = PriceAdjustmentType.COMMISSION;
            } else {
                adjustmentType = PriceAdjustmentType.PERCENTAGE;
            }
        }
        dto.setAdjustmentType(adjustmentType);

        // 调整值：如果为 null，根据调整类型设置默认值
        BigDecimal adjustmentValue = channel.getPriceAdjustmentValue();
        if (adjustmentValue == null) {
            if (adjustmentType == PriceAdjustmentType.COMMISSION && channel.getCommissionRate() != null) {
                // 使用现有的佣金率
                adjustmentValue = BigDecimal.valueOf(channel.getCommissionRate());
            } else {
                adjustmentValue = BigDecimal.ZERO;
            }
        }
        dto.setAdjustmentValue(adjustmentValue);

        // 自动同步默认为 true
        Boolean autoSync = channel.getAutoSyncPrice();
        dto.setAutoSyncPrice(autoSync != null ? autoSync : true);

        // 设置示例价格计算
        BigDecimal exampleBase = new BigDecimal("1000");
        dto.setExampleBasePrice(exampleBase);
        dto.setExampleChannelPrice(channel.calculateChannelPrice(exampleBase));

        return dto;
    }
}
