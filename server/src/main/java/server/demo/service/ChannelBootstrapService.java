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

        // DIRECT
        seeds.add(new ChannelSeed("自来客", "DIRECT", ChannelType.DIRECT, "#409EFF", "直接预订客户"));
        seeds.add(new ChannelSeed("手动录入", "MANUAL", ChannelType.DIRECT, "#409EFF", "手动录入订单"));
        seeds.add(new ChannelSeed("预订引擎", "BOOKING_ENGINE", ChannelType.DIRECT, "#409EFF", "在线预订引擎"));
        seeds.add(new ChannelSeed("中文网站", "CHINESE_WEB", ChannelType.DIRECT, "#67C23A", "中文官方网站"));

        // OTA
        seeds.add(new ChannelSeed("Airbnb", "AIRBNB", ChannelType.OTA, "#FF5A5F", "Airbnb"));
        seeds.add(new ChannelSeed("Booking.com", "BOOKING", ChannelType.OTA, "#003580", "Booking.com"));
        seeds.add(new ChannelSeed("Agoda", "AGODA", ChannelType.OTA, "#E6A23C", "Agoda"));
        seeds.add(new ChannelSeed("Expedia", "EXPEDIA", ChannelType.OTA, "#303133", "Expedia"));
        seeds.add(new ChannelSeed("Traveloka", "TRAVELOKA", ChannelType.OTA, "#409EFF", "Traveloka"));
        seeds.add(new ChannelSeed("Trip.com", "TRIP", ChannelType.OTA, "#409EFF", "Trip.com"));
        seeds.add(new ChannelSeed("Tiket.com", "TIKET", ChannelType.OTA, "#409EFF", "Tiket.com"));
        seeds.add(new ChannelSeed("青年旅舍世界", "HOSTELWORLD", ChannelType.OTA, "#2B3E50", "HostelWorld"));
        seeds.add(new ChannelSeed("途家", "TUJIA", ChannelType.OTA, "#FF6B35", "途家民宿"));
        seeds.add(new ChannelSeed("尼潘", "NEPPAN", ChannelType.OTA, "#FF6B35", "Neppan 渠道管理平台"));
        seeds.add(new ChannelSeed("红注", "REDNOT", ChannelType.OTA, "#F56C6C", "红注"));

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

