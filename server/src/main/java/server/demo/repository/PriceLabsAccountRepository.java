package server.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import server.demo.entity.PriceLabsAccount;

import java.util.List;
import java.util.Optional;

@Repository
public interface PriceLabsAccountRepository extends JpaRepository<PriceLabsAccount, Long> {

    List<PriceLabsAccount> findByStoreIdOrderByCreatedAtAsc(Long storeId);

    List<PriceLabsAccount> findByStoreIdAndIsEnabledTrueOrderByCreatedAtAsc(Long storeId);

    Optional<PriceLabsAccount> findByStoreIdAndId(Long storeId, Long id);

    Optional<PriceLabsAccount> findByStoreIdAndPriceLabsEmail(Long storeId, String priceLabsEmail);

    boolean existsByStoreIdAndPriceLabsEmail(Long storeId, String priceLabsEmail);

    boolean existsByStoreIdAndAccountName(Long storeId, String accountName);
}
