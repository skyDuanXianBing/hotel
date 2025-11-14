package server.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import server.demo.entity.RoomGroup;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomGroupRepository extends JpaRepository<RoomGroup, Long> {

    List<RoomGroup> findByStoreId(Long storeId);

    Optional<RoomGroup> findByStoreIdAndId(Long storeId, Long id);

    boolean existsByStoreIdAndName(Long storeId, String name);

    @Deprecated
    List<RoomGroup> findByUserId(Long userId);
}
