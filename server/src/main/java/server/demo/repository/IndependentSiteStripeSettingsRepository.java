package server.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import server.demo.entity.IndependentSiteStripeSettings;

import java.util.Optional;

@Repository
public interface IndependentSiteStripeSettingsRepository
        extends JpaRepository<IndependentSiteStripeSettings, Long> {

    Optional<IndependentSiteStripeSettings> findByStoreId(Long storeId);
}
