package server.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import server.demo.entity.RegistrationLinkInboxItem;

import java.util.List;
import java.util.Optional;

@Repository
public interface RegistrationLinkInboxRepository extends JpaRepository<RegistrationLinkInboxItem, Long> {
    Optional<RegistrationLinkInboxItem> findByStoreIdAndBookingKey(Long storeId, String bookingKey);

    List<RegistrationLinkInboxItem> findTop200ByStoreIdOrderByCreatedAtDesc(Long storeId);
}

