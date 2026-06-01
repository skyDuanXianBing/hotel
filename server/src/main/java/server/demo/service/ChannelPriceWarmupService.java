package server.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import server.demo.repository.ChannelPriceRepository;
import server.demo.repository.RoomTypePricePlanRepository;
import server.demo.repository.StoreRepository;
import server.demo.util.StoreTimeZoneUtil;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 渠道价格预热（方案A）：
 * - 第一次收到带 X-Store-Id 的 StoreScoped 请求时，如果未来区间内还没有 channel_prices，则同步兜底生成并落库
 * - 仅生成 enabled && autoSyncPrice=true 的渠道（由 ChannelPriceFallbackService 负责过滤）
 * - 当 PriceLabs 后续回推 base_price 时，仍以 PriceLabs 数据为准（兜底不覆盖已回推数据）
 */
@Service
public class ChannelPriceWarmupService {

    private static final Logger logger = LoggerFactory.getLogger(ChannelPriceWarmupService.class);

    private static final int DEFAULT_DAYS = 365;

    private final ChannelPriceRepository channelPriceRepository;
    private final RoomTypePricePlanRepository roomTypePricePlanRepository;
    private final ChannelPriceFallbackService channelPriceFallbackService;
    private final StoreRepository storeRepository;
    private final Clock clock;

    private final Map<Long, LocalDate> warmedAtByStoreId = new ConcurrentHashMap<>();

    public ChannelPriceWarmupService(
            ChannelPriceRepository channelPriceRepository,
            RoomTypePricePlanRepository roomTypePricePlanRepository,
            ChannelPriceFallbackService channelPriceFallbackService,
            StoreRepository storeRepository,
            Clock clock
    ) {
        this.channelPriceRepository = channelPriceRepository;
        this.roomTypePricePlanRepository = roomTypePricePlanRepository;
        this.channelPriceFallbackService = channelPriceFallbackService;
        this.storeRepository = storeRepository;
        this.clock = clock;
    }

    public void warmupIfNeeded(Long storeId) {
        if (storeId == null) {
            return;
        }

        LocalDate today = currentStoreDate(storeId);
        LocalDate warmedAt = warmedAtByStoreId.get(storeId);
        if (today.equals(warmedAt)) {
            return;
        }

        LocalDate endDate = today.plusDays(DEFAULT_DAYS - 1L);

        if (channelPriceRepository.existsByStoreIdAndPriceDateBetween(storeId, today, endDate)) {
            warmedAtByStoreId.put(storeId, today);
            return;
        }

        if (!roomTypePricePlanRepository.existsByStoreId(storeId)) {
            // 门店还没建立“房型-价格计划”组合，预热会生成 0 条；不要缓存 warmedAt，等配置好后再触发
            logger.info("[ChannelPriceWarmup] skip warmup because no room_type_price_plans. storeId={}", storeId);
            return;
        }

        try {
            ChannelPriceFallbackService.GenerateResult result = channelPriceFallbackService.generate(storeId, today, DEFAULT_DAYS);
            logger.info("[ChannelPriceWarmup] warmup finished. storeId={}, created={}, updated={}, skippedNoBasePrice={}",
                    storeId, result.created(), result.updated(), result.skippedNoBasePrice());
            if (result.created() > 0 || result.updated() > 0) {
                warmedAtByStoreId.put(storeId, today);
            }
        } catch (Exception ex) {
            logger.warn("[ChannelPriceWarmup] warmup failed. storeId={}, message={}", storeId, ex.getMessage(), ex);
        }
    }

    private LocalDate currentStoreDate(Long storeId) {
        ZoneId zoneId = StoreTimeZoneUtil.resolveZoneId(storeRepository.findById(storeId).orElse(null));
        return LocalDate.now(clock.withZone(zoneId));
    }
}
