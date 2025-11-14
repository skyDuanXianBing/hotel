package server.demo.service;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import server.demo.entity.Room;
import server.demo.entity.RoomGroup;
import server.demo.entity.RoomGroupMember;
import server.demo.repository.RoomGroupMemberRepository;
import server.demo.repository.RoomGroupRepository;
import server.demo.repository.RoomRepository;
import server.demo.util.StoreContextUtils;

import java.util.List;

@Service
public class RoomGroupService {

    @Autowired
    private RoomGroupRepository roomGroupRepository;

    @Autowired
    private RoomGroupMemberRepository roomGroupMemberRepository;

    @Autowired
    private RoomRepository roomRepository;

    private Long currentStoreId() {
        return StoreContextUtils.requireStoreId();
    }

    private Long currentUserId() {
        return StoreContextUtils.requireUserId();
    }

    public List<RoomGroup> getAllForCurrentStore() {
        return roomGroupRepository.findByStoreId(currentStoreId());
    }

    public RoomGroup getById(Long id) {
        return roomGroupRepository.findByStoreIdAndId(currentStoreId(), id)
                .orElseThrow(() -> new RuntimeException("分组不存在或无访问权限"));
    }

    public RoomGroup create(RoomGroup roomGroup) {
        Long storeId = currentStoreId();
        if (roomGroupRepository.existsByStoreIdAndName(storeId, roomGroup.getName())) {
            throw new RuntimeException("分组名称已存在");
        }
        roomGroup.setStoreId(storeId);
        roomGroup.setUserId(currentUserId());
        return roomGroupRepository.save(roomGroup);
    }

    public RoomGroup update(Long id, RoomGroup updates) {
        RoomGroup existing = getById(id);
        if (updates.getName() != null && !updates.getName().equals(existing.getName())) {
            if (roomGroupRepository.existsByStoreIdAndName(currentStoreId(), updates.getName())) {
                throw new RuntimeException("分组名称已存在");
            }
            existing.setName(updates.getName());
        }
        if (updates.getDescription() != null) {
            existing.setDescription(updates.getDescription());
        }
        return roomGroupRepository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        RoomGroup group = getById(id);
        Long storeId = currentStoreId();
        roomGroupMemberRepository.deleteByStoreIdAndGroupId(storeId, group.getId());
        roomGroupRepository.delete(group);
    }

    public List<RoomGroupMember> getGroupMembers(Long groupId) {
        RoomGroup group = getById(groupId);
        return roomGroupMemberRepository.findByStoreIdAndGroupId(currentStoreId(), group.getId());
    }

    public RoomGroupMember addRoomToGroup(Long groupId, Long roomId) {
        RoomGroup group = getById(groupId);
        Room room = roomRepository.findByStoreIdAndId(currentStoreId(), roomId)
                .orElseThrow(() -> new RuntimeException("房间不存在或无访问权限"));

        if (roomGroupMemberRepository.existsByStoreIdAndRoomId(currentStoreId(), room.getId())) {
            throw new RuntimeException("房间已在其他分组中");
        }

        RoomGroupMember member = new RoomGroupMember(group.getId(), room.getId(), currentStoreId());
        return roomGroupMemberRepository.save(member);
    }

    @Transactional
    public void addRoomsToGroup(Long groupId, List<Long> roomIds) {
        RoomGroup group = getById(groupId);
        Long storeId = currentStoreId();

        for (Long roomId : roomIds) {
            boolean exists = roomGroupMemberRepository.existsByStoreIdAndRoomId(storeId, roomId);
            if (!exists) {
                Room room = roomRepository.findByStoreIdAndId(storeId, roomId)
                        .orElseThrow(() -> new RuntimeException("房间不存在或无访问权限"));
                RoomGroupMember member = new RoomGroupMember(group.getId(), room.getId(), storeId);
                roomGroupMemberRepository.save(member);
            }
        }
    }

    @Transactional
    public void removeRoomFromGroup(Long groupId, Long roomId) {
        RoomGroup group = getById(groupId);
        roomGroupMemberRepository.deleteByStoreIdAndGroupIdAndRoomId(currentStoreId(), group.getId(), roomId);
    }

    @Transactional
    public void removeRoomsFromGroup(Long groupId, List<Long> roomIds) {
        RoomGroup group = getById(groupId);
        Long storeId = currentStoreId();
        for (Long roomId : roomIds) {
            roomGroupMemberRepository.deleteByStoreIdAndGroupIdAndRoomId(storeId, group.getId(), roomId);
        }
    }
}
