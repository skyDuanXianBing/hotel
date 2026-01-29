package server.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import server.demo.entity.RegistrationAttachment;

import java.util.List;

@Repository
public interface RegistrationAttachmentRepository extends JpaRepository<RegistrationAttachment, Long> {
    List<RegistrationAttachment> findByFormId(Long formId);

    List<RegistrationAttachment> findByGuestId(Long guestId);

    boolean existsByGuestIdAndType(Long guestId, server.demo.enums.RegistrationAttachmentType type);
}
