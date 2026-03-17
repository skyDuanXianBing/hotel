package server.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import server.demo.entity.PaymentMethod;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Long> {

    List<PaymentMethod> findByStoreIdOrderByDisplayOrderAscIdAsc(Long storeId);

    List<PaymentMethod> findByStoreIdAndEnabledTrueOrderByDisplayOrderAscIdAsc(Long storeId);

    Optional<PaymentMethod> findByStoreIdAndId(Long storeId, Long id);

    Optional<PaymentMethod> findByStoreIdAndNameIgnoreCase(Long storeId, String name);

    boolean existsByStoreIdAndNameIgnoreCase(Long storeId, String name);

    long countByStoreId(Long storeId);
}
