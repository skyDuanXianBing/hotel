package server.demo.service;

import org.springframework.stereotype.Service;
import server.demo.entity.RoomType;
import server.demo.repository.RoomTypeRepository;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;

import server.demo.i18n.ApiMessages;
/**
 * Shared database lock for every reservation path that can consume room-type inventory.
 *
 * <p>Rows are locked one at a time in ascending ID order so callers that need more than
 * one room type use the same deterministic lock order.</p>
 */
@Service
public class RoomTypeInventoryLockService {

    private final RoomTypeRepository roomTypeRepository;

    public RoomTypeInventoryLockService(RoomTypeRepository roomTypeRepository) {
        this.roomTypeRepository = roomTypeRepository;
    }

    public Set<Long> lockRoomTypes(Long storeId, Collection<Long> roomTypeIds) {
        if (storeId == null) {
            throw new IllegalArgumentException(ApiMessages.get("api.t.2bfd332b0f72"));
        }
        TreeSet<Long> sortedIds = new TreeSet<>();
        if (roomTypeIds != null) {
            for (Long roomTypeId : roomTypeIds) {
                if (roomTypeId == null || roomTypeId <= 0) {
                    throw new IllegalArgumentException(ApiMessages.get("api.t.3f34209fa2c0"));
                }
                sortedIds.add(roomTypeId);
            }
        }

        Set<Long> lockedIds = new LinkedHashSet<>();
        for (Long roomTypeId : sortedIds) {
            RoomType locked = roomTypeRepository.findByStoreIdAndIdForUpdate(storeId, roomTypeId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            ApiMessages.get("api.t.386d40a76013") + roomTypeId
                    ));
            lockedIds.add(locked.getId());
        }
        return Set.copyOf(lockedIds);
    }
}
