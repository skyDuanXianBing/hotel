package server.demo.service.saas;

import org.springframework.stereotype.Component;
import server.demo.constants.SaasFeatureCodes;
import server.demo.repository.RoomRepository;

/**
 * room_count（可存在房间数量）的实时计数器：与 RoomTypeService 容量校验使用同一口径
 * （{@link RoomRepository#countByStoreId}，门店全部房间总数）。
 */
@Component
public class RoomCountCapacityCounter implements CapacityCounter {

    private final RoomRepository roomRepository;

    public RoomCountCapacityCounter(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    @Override
    public String featureCode() {
        return SaasFeatureCodes.ROOM_COUNT;
    }

    @Override
    public long count(Long storeId) {
        return roomRepository.countByStoreId(storeId);
    }
}
