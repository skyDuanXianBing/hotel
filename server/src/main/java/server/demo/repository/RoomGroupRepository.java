package server.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import server.demo.entity.RoomGroup;

import java.util.List;

@Repository
public interface RoomGroupRepository extends JpaRepository<RoomGroup, Long> {

    /**
     * 根据用户ID查找所有房间分组
     */
    List<RoomGroup> findByUserId(Long userId);
}
