package server.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import server.demo.entity.SuMessageThread;

import java.util.List;
import java.util.Optional;

public interface SuMessageThreadRepository extends JpaRepository<SuMessageThread, Long> {
    Optional<SuMessageThread> findByStoreIdAndId(Long storeId, Long id);

    Optional<SuMessageThread> findByStoreIdAndChannelIdAndThreadKey(Long storeId, Integer channelId, String threadKey);

    List<SuMessageThread> findByStoreIdOrderByLastActivityDesc(Long storeId);
}

