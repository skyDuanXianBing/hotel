package server.demo.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.demo.entity.Channel;
import server.demo.enums.ChannelType;
import server.demo.enums.PriceAdjustmentType;
import server.demo.repository.ChannelRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 为指定门店补齐默认渠道（用于渠道配置、价格比例、OTA 同步等）。
 */
@Service
public class ChannelBootstrapService {

    private final ChannelRepository channelRepository;

    public ChannelBootstrapService(ChannelRepository channelRepository) {
        this.channelRepository = channelRepository;
    }

    @Transactional
    public int ensureDefaultChannelsForStore(Long storeId) {
        if (storeId == null) {
            return 0;
        }

        List<ChannelSeed> seeds = defaultSeeds();
        int created = 0;

        for (ChannelSeed seed : seeds) {
            boolean exists = channelRepository.existsByStoreIdAndCode(storeId, seed.code);
            if (exists) {
                continue;
            }

            Channel channel = new Channel();
            channel.setStoreId(storeId);
            channel.setName(seed.name);
            channel.setCode(seed.code);
            channel.setType(seed.type);
            channel.setIsActive(true);
            channel.setEnabled(true);
            channel.setColor(seed.color);
            channel.setDescription(seed.description);

            channel.setPriceAdjustmentType(PriceAdjustmentType.PERCENTAGE);
            channel.setPriceAdjustmentValue(BigDecimal.ZERO);
            channel.setAutoSyncPrice(true);

            channelRepository.save(channel);
            created++;
        }

        return created;
    }

    private static List<ChannelSeed> defaultSeeds() {
        List<ChannelSeed> seeds = new ArrayList<>();

        // 固定默认渠道：自来客、Airbnb、Booking.com
        seeds.add(new ChannelSeed("自来客", "DIRECT", ChannelType.DIRECT, "#409EFF", "直接预订客户"));
        seeds.add(new ChannelSeed("Airbnb", "AIRBNB", ChannelType.OTA, "#FF5A5F", "Airbnb"));
        seeds.add(new ChannelSeed("Booking.com", "BOOKING", ChannelType.OTA, "#003580", "Booking.com"));

        return seeds;
    }

    private record ChannelSeed(
            String name,
            String code,
            ChannelType type,
            String color,
            String description
    ) {}
}

