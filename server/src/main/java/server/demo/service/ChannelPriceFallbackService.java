package server.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.demo.constants.ChannelPriceOtaSyncState;
import server.demo.entity.Channel;
import server.demo.entity.ChannelPrice;
import server.demo.entity.PricePlan;
import server.demo.entity.RoomPrice;
import server.demo.entity.RoomType;
import server.demo.entity.RoomTypePricePlan;
import server.demo.repository.ChannelPriceRepository;
import server.demo.repository.ChannelRepository;
import server.demo.repository.RoomPriceRepository;
import server.demo.repository.RoomTypePricePlanRepository;
import server.demo.repository.RoomTypeRepository;
import server.demo.repository.StoreRepository;
import server.demo.util.LocalBasePriceResolver;
import server.demo.util.OtaChannelPricePolicy;
import server.demo.util.StoreTimeZoneUtil;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 生成/补齐 channel_prices 的本地兜底服务：
 * - 若存在 PriceLabs 数据（pricelabs_updated_at 不为空且 base_price 不为空），优先使用其 base_price
 * - 否则按 room_prices > room_type_price_plans > room_type 计算 base_price
 * - Airbnb / Booking 的 channel_price 使用基础价，倍率写入 Su mapping
 * - 其它渠道仍按渠道价格比例计算
 */
@Service
public class ChannelPriceFallbackService {

    private static final Logger logger = LoggerFactory.getLogger(ChannelPriceFallbackService.class);

    private static final int DEFAULT_DAYS = 365;
    private static final int MAX_DAYS = 500;
    private static final int BATCH_SIZE = 500;

    private final ChannelRepository channelRepository;
    private final ChannelPriceRepository channelPriceRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final RoomTypePricePlanRepository roomTypePricePlanRepository;
    private final RoomPriceRepository roomPriceRepository;
    private final StoreRepository storeRepository;
    private final Clock clock;

    public ChannelPriceFallbackService(
            ChannelRepository channelRepository,
            ChannelPriceRepository channelPriceRepository,
            RoomTypeRepository roomTypeRepository,
            RoomTypePricePlanRepository roomTypePricePlanRepository,
            RoomPriceRepository roomPriceRepository,
            StoreRepository storeRepository,
            Clock clock
    ) {
        this.channelRepository = channelRepository;
        this.channelPriceRepository = channelPriceRepository;
        this.roomTypeRepository = roomTypeRepository;
        this.roomTypePricePlanRepository = roomTypePricePlanRepository;
        this.roomPriceRepository = roomPriceRepository;
        this.storeRepository = storeRepository;
        this.clock = clock;
    }

    public record GenerateResult(
            Long storeId,
            LocalDate startDate,
            LocalDate endDate,
            int channels,
            int roomTypePricePlans,
            int created,
            int updated,
            int skippedNoBasePrice
    ) {}

    @Transactional
    public GenerateResult generate(Long storeId, LocalDate startDate, Integer days) {
        return generate(storeId, startDate, days, null, null);
    }

    @Transactional
    public GenerateResult generate(Long storeId, LocalDate startDate, Integer days, List<String> channelCodes, List<Long> channelIds) {
        if (storeId == null) {
            throw new IllegalArgumentException("storeId is required");
        }

        LocalDate effectiveStartDate = startDate != null ? startDate : currentStoreDate(storeId);
        int effectiveDays = clampDays(days != null ? days : DEFAULT_DAYS);
        LocalDate effectiveEndDate = effectiveStartDate.plusDays(effectiveDays - 1L);

        List<Channel> channels = channelRepository.findByStoreId(storeId).stream()
                .filter(ch -> Boolean.TRUE.equals(ch.getEnabled()) && Boolean.TRUE.equals(ch.getAutoSyncPrice()))
                .filter(ch -> {
                    if (channelCodes == null || channelCodes.isEmpty()) {
                        return true;
                    }
                    return channelCodes.contains(ch.getCode());
                })
                .filter(ch -> {
                    if (channelIds == null || channelIds.isEmpty()) {
                        return true;
                    }
                    return channelIds.contains(ch.getId());
                })
                .toList();

        if (channels.isEmpty()) {
            logger.warn("[ChannelPriceFallback] no enabled+autoSyncPrice channels. storeId={}", storeId);
            return new GenerateResult(storeId, effectiveStartDate, effectiveEndDate, 0, 0, 0, 0, 0);
        }

        List<RoomType> roomTypes = roomTypeRepository.findByStoreIdOrderByName(storeId);
        Map<Long, RoomType> roomTypeById = new HashMap<>();
        for (RoomType rt : roomTypes) {
            roomTypeById.put(rt.getId(), rt);
        }

        List<RoomTypePricePlan> rtpps = roomTypePricePlanRepository.findByStoreIdWithRoomTypeAndPricePlan(storeId);
        Map<String, RoomTypePricePlan> rtppByRoomTypeAndPlan = new HashMap<>();
        Map<Long, PricePlan> pricePlanById = new HashMap<>();
        for (RoomTypePricePlan rtpp : rtpps) {
            if (rtpp.getRoomType() == null || rtpp.getPricePlan() == null) {
                continue;
            }
            rtppByRoomTypeAndPlan.put(roomPlanKey(rtpp.getRoomType().getId(), rtpp.getPricePlan().getId()), rtpp);
            pricePlanById.put(rtpp.getPricePlan().getId(), rtpp.getPricePlan());
        }

        List<RoomPrice> roomPrices = roomPriceRepository.findByStoreIdAndPriceDateBetweenWithRoomTypeAndPricePlan(
                storeId, effectiveStartDate, effectiveEndDate
        );
        Map<String, RoomPrice> roomPriceByKey = new HashMap<>();
        for (RoomPrice rp : roomPrices) {
            if (rp.getRoomType() == null || rp.getPricePlan() == null || rp.getPriceDate() == null) {
                continue;
            }
            roomPriceByKey.put(roomPlanDateKey(rp.getRoomType().getId(), rp.getPricePlan().getId(), rp.getPriceDate()), rp);
            pricePlanById.put(rp.getPricePlan().getId(), rp.getPricePlan());
        }

        List<ChannelPrice> existing = channelPriceRepository.findByStoreIdAndDateRangeWithRelations(
                storeId, effectiveStartDate, effectiveEndDate
        );
        Map<String, ChannelPrice> channelPriceByKey = new HashMap<>();
        for (ChannelPrice cp : existing) {
            if (cp.getRoomType() == null || cp.getPricePlan() == null || cp.getChannel() == null || cp.getPriceDate() == null) {
                continue;
            }
            channelPriceByKey.put(
                    channelPriceKey(cp.getRoomType().getId(), cp.getPricePlan().getId(), cp.getChannel().getId(), cp.getPriceDate()),
                    cp
            );
        }

        int created = 0;
        int updated = 0;
        int skippedNoBasePrice = 0;

        List<ChannelPrice> batch = new ArrayList<>(BATCH_SIZE);

        // 以 room_type_price_plans 为基础生成组合；room_prices 存在但未建关联时，也允许补齐（按 room_prices 的 pricePlan）。
        Map<String, Boolean> roomPlanPairs = new HashMap<>();
        for (RoomTypePricePlan rtpp : rtpps) {
            if (rtpp.getRoomType() == null || rtpp.getPricePlan() == null) {
                continue;
            }
            roomPlanPairs.put(roomPlanKey(rtpp.getRoomType().getId(), rtpp.getPricePlan().getId()), true);
        }
        for (RoomPrice rp : roomPrices) {
            if (rp.getRoomType() == null || rp.getPricePlan() == null) {
                continue;
            }
            roomPlanPairs.put(roomPlanKey(rp.getRoomType().getId(), rp.getPricePlan().getId()), true);
        }

        for (String roomPlanKey : roomPlanPairs.keySet()) {
            long roomTypeId = parseFirstId(roomPlanKey);
            long pricePlanId = parseSecondId(roomPlanKey);

            RoomType roomType = roomTypeById.get(roomTypeId);
            if (roomType == null) {
                continue;
            }
            RoomTypePricePlan rtpp = rtppByRoomTypeAndPlan.get(roomPlanKey(roomTypeId, pricePlanId));

            LocalDate current = effectiveStartDate;
            while (!current.isAfter(effectiveEndDate)) {
                RoomPrice roomPrice = roomPriceByKey.get(roomPlanDateKey(roomTypeId, pricePlanId, current));
                LocalBasePriceResolver.Result local = LocalBasePriceResolver.resolve(roomPrice, rtpp, roomType, current);

                for (Channel channel : channels) {
                    String cpKey = channelPriceKey(roomTypeId, pricePlanId, channel.getId(), current);
                    ChannelPrice cp = channelPriceByKey.get(cpKey);
                    boolean isNew = false;
                    if (cp == null) {
                        cp = new ChannelPrice();
                        isNew = true;
                        cp.setStoreId(storeId);
                        cp.setRoomType(roomType);
                        cp.setPricePlan(pricePlanById.get(pricePlanId));
                        cp.setChannel(channel);
                        cp.setPriceDate(current);
                    }

                    if (cp.getPricePlan() == null) {
                        continue;
                    }

                    BigDecimal basePrice = selectBasePrice(cp, local.basePrice());
                    if (basePrice == null) {
                        skippedNoBasePrice++;
                        continue;
                    }

                    BigDecimal newChannelPrice = OtaChannelPricePolicy.resolveLocalFixedPrice(channel, basePrice);
                    if (newChannelPrice == null) {
                        skippedNoBasePrice++;
                        continue;
                    }

                    boolean changed = false;

                    if (cp.getPriceLabsUpdatedAt() == null || cp.getBasePrice() == null) {
                        if (cp.getBasePrice() == null || cp.getBasePrice().compareTo(basePrice) != 0) {
                            cp.setBasePrice(basePrice);
                            changed = true;
                        }
                    }

                    if (cp.getChannelPrice() == null || cp.getChannelPrice().compareTo(newChannelPrice) != 0) {
                        cp.setChannelPrice(newChannelPrice);
                        changed = true;
                    }

                    if (cp.getMinStay() == null && local.minStay() != null) {
                        cp.setMinStay(local.minStay());
                        changed = true;
                    }
                    if (cp.getMaxStay() == null && local.maxStay() != null) {
                        cp.setMaxStay(local.maxStay());
                        changed = true;
                    }

                    if (changed) {
                        cp.setIsSyncedToOta(false);
                        cp.setOtaSyncState(ChannelPriceOtaSyncState.PENDING);
                    }

                    if (isNew) {
                        created++;
                        channelPriceByKey.put(cpKey, cp);
                    } else if (changed) {
                        updated++;
                    }

                    if (isNew || changed) {
                        batch.add(cp);
                        if (batch.size() >= BATCH_SIZE) {
                            channelPriceRepository.saveAll(batch);
                            batch.clear();
                        }
                    }
                }

                current = current.plusDays(1);
            }
        }

        if (!batch.isEmpty()) {
            channelPriceRepository.saveAll(batch);
        }

        return new GenerateResult(
                storeId,
                effectiveStartDate,
                effectiveEndDate,
                channels.size(),
                rtpps.size(),
                created,
                updated,
                skippedNoBasePrice
        );
    }

    private static BigDecimal selectBasePrice(ChannelPrice existing, BigDecimal localBasePrice) {
        if (existing != null && existing.getPriceLabsUpdatedAt() != null && existing.getBasePrice() != null) {
            return existing.getBasePrice();
        }
        return localBasePrice;
    }

    private static int clampDays(int days) {
        if (days < 1) {
            return 1;
        }
        return Math.min(days, MAX_DAYS);
    }

    private LocalDate currentStoreDate(Long storeId) {
        ZoneId zoneId = StoreTimeZoneUtil.resolveZoneId(storeRepository.findById(storeId).orElse(null));
        return LocalDate.now(clock.withZone(zoneId));
    }

    private static String roomPlanKey(Long roomTypeId, Long pricePlanId) {
        return roomTypeId + "_" + pricePlanId;
    }

    private static String roomPlanDateKey(Long roomTypeId, Long pricePlanId, LocalDate date) {
        return roomTypeId + "_" + pricePlanId + "_" + date;
    }

    private static String channelPriceKey(Long roomTypeId, Long pricePlanId, Long channelId, LocalDate date) {
        return roomTypeId + "_" + pricePlanId + "_" + channelId + "_" + date;
    }

    private static long parseFirstId(String key) {
        int idx = key.indexOf('_');
        return Long.parseLong(key.substring(0, idx));
    }

    private static long parseSecondId(String key) {
        int idx = key.indexOf('_');
        return Long.parseLong(key.substring(idx + 1));
    }
}
