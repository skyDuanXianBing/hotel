package server.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import server.demo.entity.RoomGroupMember;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomGroupMemberRepository extends JpaRepository<RoomGroupMember, Long> {

    /**
     * 根据分组ID查找所有成员
     */
    List<RoomGroupMember> findByGroupId(Long groupId);

    /**
     * 根据房间ID查找成员
     */
    Optional<RoomGroupMember> findByRoomId(Long roomId);

    /**
     * 根据分组ID删除所有成员
     */
    void deleteByGroupId(Long groupId);

    /**
     * 根据分组ID和房间ID删除成员
     */
    void deleteByGroupIdAndRoomId(Long groupId, Long roomId);

    /**
     * 检查房间是否已在某个分组中
     */
    boolean existsByRoomId(Long roomId);

    /**
     * 检查房间是否在指定分组中
     */
    boolean existsByGroupIdAndRoomId(Long groupId, Long roomId);
}
