package server.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import server.demo.entity.RegistrationForm;
import server.demo.enums.RegistrationFormStatus;

import java.util.List;
import java.util.Optional;

@Repository
public interface RegistrationFormRepository extends JpaRepository<RegistrationForm, Long> {
    Optional<RegistrationForm> findByStoreIdAndOrderNumber(Long storeId, String orderNumber);

    Optional<RegistrationForm> findByStoreIdAndReservation_Id(Long storeId, Long reservationId);

    List<RegistrationForm> findByStoreIdAndStatusOrderByUpdatedAtDesc(Long storeId, RegistrationFormStatus status);

    List<RegistrationForm> findByStoreIdOrderByUpdatedAtDesc(Long storeId);
}
