package server.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import server.demo.entity.ChannelMappingPriceSetting;
import server.demo.enums.ChannelMappingPriceSyncStatus;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChannelMappingPriceSettingRepository extends JpaRepository<ChannelMappingPriceSetting, Long> {

    List<ChannelMappingPriceSetting> findByStoreIdAndChannelIdAndSuPropertyId(
            Long storeId,
            Long channelId,
            String suPropertyId
    );

    List<ChannelMappingPriceSetting> findByStoreIdAndSuPropertyIdAndSuChannelId(
            Long storeId,
            String suPropertyId,
            String suChannelId
    );

    Optional<ChannelMappingPriceSetting> findByStoreIdAndChannelIdAndMappingKey(
            Long storeId,
            Long channelId,
            String mappingKey
    );

    Optional<ChannelMappingPriceSetting> findByStoreIdAndChannelIdAndRowKey(
            Long storeId,
            Long channelId,
            String rowKey
    );

    List<ChannelMappingPriceSetting> findByStoreIdAndChannelIdAndRowKeyIn(
            Long storeId,
            Long channelId,
            Collection<String> rowKeys
    );

    List<ChannelMappingPriceSetting> findByStoreIdAndChannelIdAndSyncStatusIn(
            Long storeId,
            Long channelId,
            Collection<ChannelMappingPriceSyncStatus> statuses
    );
}
