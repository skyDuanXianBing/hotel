package server.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import server.demo.entity.SuMessagingAiSetting;

import java.util.Optional;

public interface SuMessagingAiSettingRepository extends JpaRepository<SuMessagingAiSetting, Long> {
    Optional<SuMessagingAiSetting> findByStoreId(Long storeId);
}
