package server.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import server.demo.entity.RegistrationGuest;

import java.util.List;

@Repository
public interface RegistrationGuestRepository extends JpaRepository<RegistrationGuest, Long> {
    List<RegistrationGuest> findByFormIdOrderBySortOrderAsc(Long formId);

    void deleteByFormId(Long formId);
}
