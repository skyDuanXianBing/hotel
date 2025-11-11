package server.demo.service;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import server.demo.entity.RoomGroup;
import server.demo.entity.RoomGroupMember;
import server.demo.repository.RoomGroupMemberRepository;
import server.demo.repository.RoomGroupRepository;

import java.util.List;

@Service
public class RoomGroupService {

    @Autowired
    private RoomGroupRepository roomGroupRepository;

    @Autowired
    private RoomGroupMemberRepository roomGroupMemberRepository;

    /**
     * 根据用户ID获取所有分组
     */
    public List<RoomGroup> getAllByUserId(Long userId) {
        return roomGroupRepository.findByUserId(userId);
    }

    /**
     * 根据ID获取分组
     */
    public RoomGroup getById(Long id) {
        return roomGroupRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("分组不存在"));
    }

    /**
     * 创建分组
     */
    public RoomGroup create(RoomGroup roomGroup) {
        return roomGroupRepository.save(roomGroup);
    }

    /**
     * 更新分组
     */
    public RoomGroup update(Long id, RoomGroup updates) {
        RoomGroup existing = getById(id);

        if (updates.getName() != null) {
            existing.setName(updates.getName());
        }
        if (updates.getDescription() != null) {
            existing.setDescription(updates.getDescription());
        }

        return roomGroupRepository.save(existing);
    }

    /**
     * 删除分组(同时删除所有成员关系)
     */
    @Transactional
    public void delete(Long id) {
        // 先删除所有成员关系
        roomGroupMemberRepository.deleteByGroupId(id);
        // 再删除分组本身
        roomGroupRepository.deleteById(id);
    }

    /**
     * 获取分组的所有房间
     */
    public List<RoomGroupMember> getGroupMembers(Long groupId) {
        return roomGroupMemberRepository.findByGroupId(groupId);
    }

    /**
     * 添加房间到分组
     */
    public RoomGroupMember addRoomToGroup(Long groupId, Long roomId) {
        // 检查房间是否已在其他分组中
        if (roomGroupMemberRepository.existsByRoomId(roomId)) {
            throw new RuntimeException("房间已在其他分组中");
        }

        // 检查分组是否存在
        if (!roomGroupRepository.existsById(groupId)) {
            throw new RuntimeException("分组不存在");
        }

        RoomGroupMember member = new RoomGroupMember(groupId, roomId);
        return roomGroupMemberRepository.save(member);
    }

    /**
     * 批量添加房间到分组
     */
    @Transactional
    public void addRoomsToGroup(Long groupId, List<Long> roomIds) {
        // 检查分组是否存在
        if (!roomGroupRepository.existsById(groupId)) {
            throw new RuntimeException("分组不存在");
        }

        for (Long roomId : roomIds) {
            // 跳过已在其他分组中的房间
            if (!roomGroupMemberRepository.existsByRoomId(roomId)) {
                RoomGroupMember member = new RoomGroupMember(groupId, roomId);
                roomGroupMemberRepository.save(member);
            }
        }
    }

    /**
     * 从分组中移除房间
     */
    @Transactional
    public void removeRoomFromGroup(Long groupId, Long roomId) {
        roomGroupMemberRepository.deleteByGroupIdAndRoomId(groupId, roomId);
    }

    /**
     * 批量从分组中移除房间
     */
    @Transactional
    public void removeRoomsFromGroup(Long groupId, List<Long> roomIds) {
        for (Long roomId : roomIds) {
            roomGroupMemberRepository.deleteByGroupIdAndRoomId(groupId, roomId);
        }
    }
}
