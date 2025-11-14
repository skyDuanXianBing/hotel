package server.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import server.demo.entity.RoomGroupMember;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomGroupMemberRepository extends JpaRepository<RoomGroupMember, Long> {

    List<RoomGroupMember> findByStoreIdAndGroupId(Long storeId, Long groupId);

    Optional<RoomGroupMember> findByStoreIdAndRoomId(Long storeId, Long roomId);

    void deleteByStoreIdAndGroupId(Long storeId, Long groupId);

    void deleteByStoreIdAndGroupIdAndRoomId(Long storeId, Long groupId, Long roomId);

    boolean existsByStoreIdAndRoomId(Long storeId, Long roomId);

    boolean existsByStoreIdAndGroupIdAndRoomId(Long storeId, Long groupId, Long roomId);

    // 兼容旧逻辑接口
    @Deprecated
    List<RoomGroupMember> findByGroupId(Long groupId);

    @Deprecated
    Optional<RoomGroupMember> findByRoomId(Long roomId);

    @Deprecated
    void deleteByGroupId(Long groupId);

    @Deprecated
    void deleteByGroupIdAndRoomId(Long groupId, Long roomId);

    @Deprecated
    boolean existsByRoomId(Long roomId);

    @Deprecated
    boolean existsByGroupIdAndRoomId(Long groupId, Long roomId);
}
