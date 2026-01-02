package server.demo.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import server.demo.entity.Store;
import server.demo.repository.StoreRepository;
import server.demo.service.ChannelBootstrapService;

import java.util.List;

/**
 * 启动时为所有门店补齐默认渠道，避免“价格比例/渠道配置”空数据。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class ChannelBootstrapRunner implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(ChannelBootstrapRunner.class);

    private final StoreRepository storeRepository;
    private final ChannelBootstrapService channelBootstrapService;

    public ChannelBootstrapRunner(StoreRepository storeRepository, ChannelBootstrapService channelBootstrapService) {
        this.storeRepository = storeRepository;
        this.channelBootstrapService = channelBootstrapService;
    }

    @Override
    public void run(String... args) {
        List<Store> stores = storeRepository.findAll();
        int totalCreated = 0;
        for (Store store : stores) {
            try {
                int created = channelBootstrapService.ensureDefaultChannelsForStore(store.getId());
                totalCreated += created;
            } catch (Exception e) {
                logger.warn("[ChannelBootstrapRunner] ensure channels failed. storeId={}, err={}",
                        store.getId(), e.getMessage());
            }
        }
        if (totalCreated > 0) {
            logger.info("[ChannelBootstrapRunner] created default channels={}", totalCreated);
        }
    }
}

