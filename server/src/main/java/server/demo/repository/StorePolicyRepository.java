package server.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import server.demo.entity.StorePolicy;

import java.util.Optional;

@Repository
public interface StorePolicyRepository extends JpaRepository<StorePolicy, Long> {
    Optional<StorePolicy> findByStoreId(Long storeId);
    void deleteByStoreId(Long storeId);
}
