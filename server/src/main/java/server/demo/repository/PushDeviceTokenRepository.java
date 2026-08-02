package server.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import server.demo.entity.PushDeviceToken;

import java.util.List;
import java.util.Optional;

@Repository
public interface PushDeviceTokenRepository extends JpaRepository<PushDeviceToken, Long> {

    Optional<PushDeviceToken> findByDeviceToken(String deviceToken);

    List<PushDeviceToken> findByUserIdInAndEnabledTrue(List<Long> userIds);

    void deleteByDeviceToken(String deviceToken);
}
