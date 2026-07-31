package server.demo.repository.saas;

import org.springframework.data.jpa.repository.JpaRepository;
import server.demo.entity.saas.SaasFeature;

import java.util.Optional;

public interface SaasFeatureRepository extends JpaRepository<SaasFeature, Long> {

    Optional<SaasFeature> findByFeatureCode(String featureCode);
}
