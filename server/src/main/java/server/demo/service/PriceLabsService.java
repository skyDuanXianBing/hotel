package server.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * PriceLabs 集成服务
 * 处理与 PriceLabs 的所有集成相关业务逻辑
 */
@Service
public class PriceLabsService {

    private static final int DEFAULT_FALLBACK_DAYS = 365;
    private static final int MAX_FALLBACK_DAYS = 500;

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
    private PriceLabsApiClient priceLabsApiClient;

    @Autowired
    private server.demo.config.PriceLabsConfig priceLabsConfig;

    @Autowired
    private ChannelPriceFallbackService channelPriceFallbackService;

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

        // 检查是否已存在
        if (connectionRepository.findByRoomTypeIdAndPricePlanId(roomTypeId, pricePlanId).isPresent()) {
            throw new RuntimeException("该房型与价格计划的连接已存在");
        }

        RoomType roomType = roomTypeRepository.findById(roomTypeId)
                .orElseThrow(() -> new RuntimeException("房型不存在"));
        PricePlan pricePlan = pricePlanRepository.findById(pricePlanId)
                .orElseThrow(() -> new RuntimeException("价格计划不存在"));

        PriceLabsConnection connection = new PriceLabsConnection(roomType, pricePlan);
        connection.setStoreId(storeId);
        connection.generatePriceLabsListingId();

        PriceLabsConnection saved = connectionRepository.save(connection);
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

        // 遍历所有房源数据
        for (PriceLabsWebhookRequest.ListingData listingData : request.getData()) {
            String listingId = listingData.getListingId();
            String ratePlanId = listingData.getRatePlanId();

            Long roomTypeId = PriceLabsIdUtil.parseRoomTypeId(listingId)
                    .orElseThrow(() -> new RuntimeException("listingId 格式不正确: " + listingId));
            Long pricePlanId = PriceLabsIdUtil.parsePricePlanId(ratePlanId)
                    .orElseThrow(() -> new RuntimeException("ratePlanId 格式不正确: " + ratePlanId));

            // 查找对应的连接配置（listingId + ratePlanId）
            PriceLabsConnection connection = connectionRepository.findByRoomTypeIdAndPricePlanId(roomTypeId, pricePlanId)
                    .orElseThrow(() -> new RuntimeException("未找到对应的连接配置: listingId=" + listingId + ", ratePlanId=" + ratePlanId));

            PriceLabsIdUtil.parseStoreId(listingId).ifPresent(parsedStoreId -> {
                if (!parsedStoreId.equals(connection.getStoreId())) {
                    throw new RuntimeException("listingId storeId 与连接不匹配: listingId=" + listingId + ", connStoreId=" + connection.getStoreId());
                }
            });

            storeId = connection.getStoreId();
            RoomType roomType = connection.getRoomType();
            PricePlan pricePlan = connection.getPricePlan(); // 获取价格计划

            // 获取所有启用自动同步的渠道
            List<Channel> channels = channelRepository.findByStoreId(storeId).stream()
                    .filter(ch -> Boolean.TRUE.equals(ch.getEnabled()) && Boolean.TRUE.equals(ch.getAutoSyncPrice()))
                    .collect(Collectors.toList());

            // 处理日历数据
            if (listingData.getCalendar() != null) {
                for (PriceLabsWebhookRequest.CalendarData calendarData : listingData.getCalendar()) {
                    LocalDate priceDate = LocalDate.parse(calendarData.getDate(), formatter);
                    BigDecimal basePrice = calendarData.getPrice();

                    // 为每个渠道计算并保存价格
                    for (Channel channel : channels) {
                        BigDecimal channelPrice = channel.calculateChannelPrice(basePrice);

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
                        cp.setBasePrice(basePrice);
                        cp.setChannelPrice(channelPrice);

                        // 设置最小/最大入住天数
                        if (calendarData.getMinStay() != null) {
                            cp.setMinStay(calendarData.getMinStay());
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

        int days = clampDays(startDate, endDate);
        channelPriceFallbackService.generate(storeId, startDate, days, null, channelId != null ? List.of(channelId) : null);

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
        dto.setTotalSyncCount(syncLogRepository.count());
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
