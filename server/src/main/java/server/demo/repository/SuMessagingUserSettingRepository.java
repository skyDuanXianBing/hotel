package server.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import server.demo.entity.SuMessagingUserSetting;

import java.util.Optional;

public interface SuMessagingUserSettingRepository extends JpaRepository<SuMessagingUserSetting, Long> {
    Optional<SuMessagingUserSetting> findByUserId(Long userId);
}
