package server.demo.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import server.demo.entity.Channel;
import server.demo.entity.Store;
import server.demo.repository.ChannelRepository;
import server.demo.repository.StoreRepository;
import server.demo.service.ChannelBootstrapService;

import java.util.List;

/**
 * 三渠道（Expedia/Trip.com/Agoda）存量数据迁移（幂等，随每次启动执行）。
 * <p>
 * spring.flyway.enabled=false，迁移不走 Flyway，故采用 CommandLineRunner：
 * 1) 为每个存量门店补齐缺失的 EXPEDIA/TRIP/AGODA channels 行（store_id+code 唯一约束兜底，已存在则跳过；
 *    与 ChannelBootstrapRunner 互为兜底）；
 * 2) 修复历史脏数据：name='阿凡达' 的 AGODA 渠道更名为 'Agoda'。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class OtaThreeChannelDataMigration implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(OtaThreeChannelDataMigration.class);

    /** 历史脏数据：AGODA 渠道曾被误命名为"阿凡达"（DataInitializer 旧种子） */
    private static final String LEGACY_AGODA_CHANNEL_NAME = "阿凡达";
    private static final String AGODA_CHANNEL_CODE = "AGODA";
    private static final String AGODA_CHANNEL_DISPLAY_NAME = "Agoda";

    private final StoreRepository storeRepository;
    private final ChannelRepository channelRepository;
    private final ChannelBootstrapService channelBootstrapService;

    public OtaThreeChannelDataMigration(StoreRepository storeRepository,
                                        ChannelRepository channelRepository,
                                        ChannelBootstrapService channelBootstrapService) {
        this.storeRepository = storeRepository;
        this.channelRepository = channelRepository;
        this.channelBootstrapService = channelBootstrapService;
    }

    @Override
    public void run(String... args) {
        backfillThreeOtaChannels();
        fixLegacyAgodaChannelName();
    }

    /**
     * 为每个存量门店幂等补齐三渠道（复用默认渠道种子逻辑，已存在则跳过；单店失败不影响其他门店）。
     */
    private void backfillThreeOtaChannels() {
        List<Store> stores = storeRepository.findAll();
        for (Store store : stores) {
            if (store == null || store.getId() == null) {
                continue;
            }
            try {
                channelBootstrapService.ensureDefaultChannelsForStore(store.getId());
            } catch (Exception e) {
                logger.warn("[OtaThreeChannelMigration] 补齐三渠道失败，跳过该门店. storeId={}, err={}",
                        store.getId(), e.getMessage());
            }
        }
    }

    /**
     * 修复历史脏数据：name='阿凡达' 的 AGODA 渠道更名为 'Agoda'（幂等，已修复则不再命中）。
     */
    private void fixLegacyAgodaChannelName() {
        List<Channel> dirty = channelRepository.findAll().stream()
                .filter(channel -> AGODA_CHANNEL_CODE.equalsIgnoreCase(trim(channel.getCode())))
                .filter(channel -> LEGACY_AGODA_CHANNEL_NAME.equals(trim(channel.getName())))
                .toList();
        if (dirty.isEmpty()) {
            return;
        }
        for (Channel channel : dirty) {
            channel.setName(AGODA_CHANNEL_DISPLAY_NAME);
        }
        channelRepository.saveAll(dirty);
        logger.info("[OtaThreeChannelMigration] 已修复 AGODA 渠道脏名称 '{}' -> '{}'. count={}",
                LEGACY_AGODA_CHANNEL_NAME, AGODA_CHANNEL_DISPLAY_NAME, dirty.size());
    }

    private static String trim(String value) {
        return value != null ? value.trim() : null;
    }
}
